package cpw.mods.fml.common.discovery.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import net.minecraft.src.BaseMod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ModCandidate;

public class ASMModParser
{

    private Type asmType;
    private int classVersion;
    private Type asmSuperType;
    private LinkedList<ModAnnotation> annotations = Lists.newLinkedList();
    private String baseModProperties;

    static enum AnnotationType
    {
        CLASS, FIELD, METHOD, SUBTYPE;
    }

    public ASMModParser(InputStream stream) throws IOException
    {
        ClassReader reader = new ClassReader(stream);
        reader.accept(new ModClassVisitor(this), 0);
    }

    public void beginNewTypeName(String typeQName, int classVersion, String superClassQName)
    {
        this.asmType = Type.getObjectType(typeQName);
        this.classVersion = classVersion;
        this.asmSuperType = Type.getObjectType(superClassQName);
    }

    public void startClassAnnotation(String annotationName)
    {
        ModAnnotation ann = new ModAnnotation(AnnotationType.CLASS, Type.getType(annotationName), this.asmType.getClassName());
        annotations.addFirst(ann);
    }

    public void addAnnotationProperty(String key, Object value)
    {
        annotations.getFirst().addProperty(key, value);
    }

    public void startFieldAnnotation(String fieldName, String annotationName)
    {
        ModAnnotation ann = new ModAnnotation(AnnotationType.FIELD, Type.getType(annotationName), fieldName);
        annotations.addFirst(ann);
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper("ASMAnnotationDiscoverer")
               .add("className", asmType.getClassName())
               .add("classVersion", classVersion)
               .add("superName", asmSuperType.getClassName())
               .add("annotations", annotations)
               .add("isBaseMod", isBaseMod())
               .add("baseModProperties", baseModProperties)
               .toString();
    }

    public Type getASMType()
    {
        return asmType;
    }

    public int getClassVersion()
    {
        return classVersion;
    }

    public Type getASMSuperType()
    {
        return asmSuperType;
    }

    public LinkedList<ModAnnotation> getAnnotations()
    {
        return annotations;
    }

    public void validate()
    {
//        if (classVersion > 50.0)
//        {
//
//            throw new LoaderException(new RuntimeException("Mod compiled for Java 7 detected"));
//        }
    }

    public boolean isBaseMod()
    {
        return getASMSuperType().equals(Type.getType(BaseMod.class));
    }

    public void setBaseModProperties(String foundProperties)
    {
        this.baseModProperties = foundProperties;
    }

    public String getBaseModProperties()
    {
        return this.baseModProperties;
    }

    public void sendToTable(ASMDataTable table, ModCandidate candidate)
    {
        for (ModAnnotation ma : annotations)
        {
            table.addASMData(candidate, ma.asmType.getClassName(), this.asmType.getClassName(), ma.member, ma.values);
        }
    }

    public void addAnnotationArray(String name)
    {
        annotations.getFirst().addArray(name);
    }

    public void addAnnotationEnumProperty(String name, String desc, String value)
    {
        annotations.getFirst().addEnumProperty(name, desc, value);

    }

    public void endArray()
    {
        annotations.getFirst().endArray();

    }

    public void addSubAnnotation(String name, String desc)
    {
        ModAnnotation ma = annotations.getFirst();
        annotations.addFirst(ma.addChildAnnotation(name, desc));
    }

    public void endSubAnnotation()
    {
        // take the child and stick it at the end
        ModAnnotation child = annotations.removeFirst();
        annotations.addLast(child);
    }
}
