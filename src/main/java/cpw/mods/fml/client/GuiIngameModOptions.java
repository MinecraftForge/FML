package cpw.mods.fml.client;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.Level;

public class GuiIngameModOptions extends GuiScreen
{
    private final GuiScreen parentScreen;
    protected String title = "Mod Options";
    private GuiModOptionList optionList;
    protected final List<ModContainer> modList;
    protected ModContainer selectedMod = null;
    protected int selected = -1;
    private GuiButton btnConfigure;

    public GuiIngameModOptions(GuiScreen parentScreen)
    {
        this.parentScreen = parentScreen;
        this.modList = new ArrayList<ModContainer>();
        for (ModContainer mod : Loader.instance().getModList())
        {
            IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(mod);
            if (guiFactory != null && guiFactory.mainConfigGuiClass() != null)
            {
                this.modList.add(mod);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui()
    {
        this.optionList=new GuiModOptionList(this);
        this.optionList.registerScrollButtons(this.buttonList, 7, 8);
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 35, I18n.format("gui.done")));
        this.btnConfigure = new GuiButton(201, this.optionList.left + this.optionList.listWidth + 5, this.optionList.top + 12, 100, 20, "Configure");
        this.buttonList.add(this.btnConfigure);
    }

    @Override
    protected void actionPerformed(GuiButton p_146284_1_)
    {
        if (p_146284_1_.enabled)
        {
            if (p_146284_1_.id == 200)
            {
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(this.parentScreen);
            }
            else if (p_146284_1_.id == 201)
            {
                try
                {
                    IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(this.selectedMod);
                    GuiScreen newScreen = guiFactory.mainConfigGuiClass().getConstructor(GuiScreen.class).newInstance(this);
                    this.mc.displayGuiScreen(newScreen);
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.ERROR, e, "There was a critical issue trying to build the config GUI for %s", this.selectedMod.getModId());
                }
            }
        }
    }

    @Override
    public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_)
    {
        // force a non-transparent background
        this.drawDefaultBackground();
        this.optionList.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
        this.drawCenteredString(this.fontRendererObj, this.title, this.width / 2, 15, 0xFFFFFF);
        if (this.selectedMod != null) {
            this.drawString(this.fontRendererObj, this.selectedMod.getName(), this.optionList.left + this.optionList.listWidth + 5, this.optionList.top, 0xFFFFFF);
            // Only needed if the initial filtering is not done
            // IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(this.selectedMod);
            // this.btnConfigure.enabled = guiFactory != null && guiFactory.mainConfigGuiClass() != null;
        }
        this.btnConfigure.visible = this.selectedMod != null;
        super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
    }

    FontRenderer getFontRenderer() {
        return fontRendererObj;
    }

    public void selectModIndex(int index)
    {
        this.selected = index;
        if (this.selected >= 0 && this.selected <= this.modList.size())
        {
            this.selectedMod = this.modList.get(this.selected);
        }
        else
        {
            this.selectedMod = null;
        }
    }
}
