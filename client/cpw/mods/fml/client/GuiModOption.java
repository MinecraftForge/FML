package cpw.mods.fml.client;

import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;

/**
 * Extend this when adding a custom mod options tab.
 * All options should be client only(just as a reminder),
 * though it is possible to have common based configs.
 * @author Syllabus
 */
public abstract class GuiModOption extends GuiScreen {

    /**
     * the GuiOptions return GUI
     */
    public GuiOptions parent;

    /**
     * No need to call this ever.
     */
    public void setOptions(GuiOptions gui)
    {
        parent = gui;
    }

    /**
     * Used for when the mod is listed.
     * @return
     */
    public abstract String getModName();

    /*
     * If need be I'll add more stuffs to this file if asked for it.
     */
}
