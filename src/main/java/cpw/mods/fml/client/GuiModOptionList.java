package cpw.mods.fml.client;

import cpw.mods.fml.common.ModContainer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

public class GuiModOptionList extends GuiScrollingList {

    private GuiIngameModOptions parent;

    public GuiModOptionList(GuiIngameModOptions parent)
    {
        super(parent.mc, 150, parent.height, 32, parent.height - 65 + 4, 10, 35);
        this.parent = parent;
    }

    @Override
    protected int getSize()
    {
        return this.parent.modList.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick)
    {
        this.parent.selectModIndex(index);
    }

    @Override
    protected boolean isSelected(int index)
    {
        return this.parent.selected == index;
    }

    @Override
    protected void drawBackground()
    {
    }

    @Override
    protected void drawSlot(int var1, int var2, int var3, int var4, Tessellator var5)
    {
        ModContainer mod = this.parent.modList.get(var1);
        FontRenderer fontRenderer = this.parent.getFontRenderer();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(mod.getName(), this.listWidth - 10), this.left + 3, var3 + 2, 0xFFFFFF);
        fontRenderer.drawString(fontRenderer.trimStringToWidth("Mod ID: " + mod.getModId(), this.listWidth - 10), this.left + 3, var3 + 12, 0xCCCCCC);
        fontRenderer.drawString(fontRenderer.trimStringToWidth("Version: " + mod.getDisplayVersion(), this.listWidth - 10), this.left + 3, var3 + 22, 0xCCCCCC);
    }

}
