package cpw.mods.fml.common.asm.transformers;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModAPIManager;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

import net.minecraft.launchwrapper.IClassTransformer;

public class ModAPITransformer implements IClassTransformer {

    private static final boolean logDebugInfo = Boolean.valueOf(System.getProperty("fml.debugAPITransformer", "true"));
    private ListMultimap<String, ASMData> optionals;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (optionals == null || !optionals.containsKey(name))
        {
            return basicClass;
        }
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - found optionals for class %s - processing", name);
        for (ASMData optional : optionals.get(name))
        {
            String modId = (String) optional.getAnnotationInfo().get("modid");

            if (Loader.isModLoaded(modId) || ModAPIManager.INSTANCE.hasAPI(modId))
            {
                if (logDebugInfo) FMLRelaunchLog.finest("Optional removal skipped - mod present %s", modId);
                continue;
            }
            if (logDebugInfo) FMLRelaunchLog.finest("Optional on %s triggered - mod missing %s", name, modId);

            if (optional.getAnnotationInfo().containsKey("iface"))
            {
                // Defaulted annotation entries seemingly aren't collected, so this can be null.
                Object stripMethods = optional.getAnnotationInfo().get("methods");
                stripInterface(classNode,(String)optional.getAnnotationInfo().get("iface"), stripMethods == null ? false : (Boolean)stripMethods);
            }
            else
            {
                stripMethod(classNode, (String)optional.getObjectName());
            }

        }
        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - class %s processed", name);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private void stripMethod(ClassNode classNode, String methodDescriptor)
    {
        for (ListIterator<MethodNode> iterator = classNode.methods.listIterator(); iterator.hasNext();)
        {
            MethodNode method = iterator.next();
            if (methodDescriptor.equals(method.name+method.desc))
            {
                iterator.remove();
                if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - method %s removed", methodDescriptor);
                return;
            }
        }
        if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - method %s NOT removed - not found", methodDescriptor);
    }

    private void stripInterface(ClassNode classNode, String interfaceName, boolean stripMethods)
    {
        String ifaceName = interfaceName.replace('.', '/');
        boolean found = classNode.interfaces.remove(ifaceName);
        if (found && logDebugInfo) FMLRelaunchLog.finest("Optional removal - interface %s removed", interfaceName);
        if (stripMethods) {
            for (ListIterator<MethodNode> iterator = classNode.methods.listIterator(); iterator.hasNext();)
            {
                MethodNode method = iterator.next();
                if (method.desc.contains("L" + ifaceName + ";"))
                {
                    iterator.remove();
                    found = true;
                    if (logDebugInfo) FMLRelaunchLog.finest("Optional removal - method %s removed because its signature contained %s", method.name+method.desc, interfaceName);
                }
            }
        }
        if (!found && logDebugInfo) FMLRelaunchLog.finest("Optional removal - interface %s NOT removed - not found", interfaceName);
    }

    public void initTable(ASMDataTable dataTable)
    {
        optionals = ArrayListMultimap.create();
        Set<ASMData> interfaceLists = dataTable.getAll("cpw.mods.fml.common.Optional$InterfaceList");
        addData(unpackInterfaces(interfaceLists));
        Set<ASMData> interfaces = dataTable.getAll("cpw.mods.fml.common.Optional$Interface");
        addData(interfaces);
        Set<ASMData> methods = dataTable.getAll("cpw.mods.fml.common.Optional$Method");
        addData(methods);
    }

    private Set<ASMData> unpackInterfaces(Set<ASMData> packedInterfaces)
    {
        Set<ASMData> result = Sets.newHashSet();
        for (ASMData data : packedInterfaces)
        {
            List<Map<String,Object>> packedList = (List<Map<String,Object>>) data.getAnnotationInfo().get("value");
            for (Map<String,Object> packed : packedList)
            {
                ASMData newData = data.copy(packed);
                result.add(newData);
            }
        }

        return result;
    }
    private void addData(Set<ASMData> interfaces)
    {
        for (ASMData data : interfaces)
        {
            optionals.put(data.getClassName(),data);
        }
    }

}
