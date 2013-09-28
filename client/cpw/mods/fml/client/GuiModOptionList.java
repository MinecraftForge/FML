package cpw.mods.fml.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSmallButton;
import net.minecraft.client.resources.I18n;

/**
 * GUI for listing mod options
 * @author Syllabus
 */
public class GuiModOptionList extends GuiScreen {

    public GuiOptions returnScreen;
    public GuiSlotModOptions modList;
    public GuiModOption clickedOption;

    public GuiModOptionList(GuiOptions options)
    {
        returnScreen = options;
    }

    public void func_73866_w_()
    {
        field_73887_h.add(new GuiSmallButton(6, field_73880_f / 2 - 75, field_73881_g - 38, I18n.func_135053_a("gui.done")));
        modList = new GuiSlotModOptions(this);
        modList.registerScrollButtons(this.field_73887_h, 7, 8);
    }
    
    protected void func_73875_a(GuiButton par1GuiButton)
    {
        if (par1GuiButton.field_73742_g)
        {
            switch (par1GuiButton.field_73741_f)
            {
                case 5:
                    break;
                case 6:
                    field_73882_e.func_71373_a(returnScreen);
                    break;
                default:
                    modList.actionPerformed(par1GuiButton);
            }
        }
    }
    
    public void updateScreen()
    {
        super.updateScreen();
        if(clickedOption != null)
        {
            mc.displayGuiScreen(clickedOption);
        }
    }
    
    public void func_73863_a(int par1, int par2, float par3)
    {
        modList.drawScreen(par1, par2, par3);
        func_73732_a(field_73886_k, "Mod Options", field_73880_f / 2, 16, 16777215);
        super.func_73863_a(par1, par2, par3);
    }
    
    public FontRenderer getRenderer()
    {
        return field_73886_k;
    }
    
    public void setClickedOption(GuiModOption gui)
    {
        clickedOption = gui;
    }
    
    public Minecraft getMc()
    {
        return field_73882_e;
    }

}
