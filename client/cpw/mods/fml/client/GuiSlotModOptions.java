package cpw.mods.fml.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.client.renderer.Tessellator;

/**
 * Used for handling the list of mod options that were registered in ClientRegistry
 * @author
 */
public class GuiSlotModOptions extends GuiScrollingList{

	public GuiModOptionList parent;

	public List<String> optionsList;
	public Map<String, GuiModOption> optionsMap;

	public GuiSlotModOptions(GuiModOptionList modOptions)
	{
		super(modOptions.getMc(), modOptions.field_73880_f, modOptions.field_73881_g, 32, modOptions.field_73881_g - 61, 18, 1);
		parent = modOptions;
		optionsList = new ArrayList<String>();
		optionsMap = new HashMap<String, GuiModOption>();

		for(String key : ClientRegistry.modOptions.keySet())
		{
			optionsList.add(key);
			optionsMap.put(key, ClientRegistry.modOptions.get(key));
		}
	}

	@Override
	protected int getSize()
	{
		return optionsList.size();
	}

	@Override
	protected void elementClicked(int i, boolean flag)
	{
		GuiModOption gui = optionsMap.get(optionsList.get(i));
		gui.setOptions(parent.returnScreen);
		parent.setClickedOption(gui);
	}

	@Override
	protected boolean isSelected(int i)
	{
		return false;
	}

	protected int getContentHeight()
	{
		return getSize() * 20;
	}

	@Override
	protected void drawBackground() 
	{
		parent.func_73873_v_();
	}

	@Override
	protected void drawSlot(int i, int j, int k, int l, Tessellator tessellator) 
	{
		parent.getRenderer().func_78276_b((String)optionsList.get(i), parent.field_73880_f / 2, k + 1, 16777215);
	}

}
