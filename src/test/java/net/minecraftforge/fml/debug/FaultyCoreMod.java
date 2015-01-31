package net.minecraftforge.fml.debug;

import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

public class FaultyCoreMod implements IFMLLoadingPlugin {
    public static boolean enabled = false;

    public String[] getASMTransformerClass()
    {
        if(enabled) return new String[] { FaultyTransformer.class.getName() };
        return null;
    }

    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {}
    public String getAccessTransformerClass() { return null; }

    public static class FaultyTransformer implements IClassTransformer {

        public byte[] transform(String name, String transformedName, byte[] basicClass)
        {
            if(name.equals("net.minecraft.client.gui.GuiMainMenu")) throw new RuntimeException("Faulty transformer test exception");
            return basicClass;
        }
    }
}
