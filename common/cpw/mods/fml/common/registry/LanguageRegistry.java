package cpw.mods.fml.common.registry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.StringTranslate;

public class LanguageRegistry
{
    private static final LanguageRegistry INSTANCE = new LanguageRegistry();

    private Map<String,Properties> modLanguageData=new HashMap<String,Properties>();

    public static LanguageRegistry instance()
    {
        return INSTANCE;
    }

    public String getStringLocalization(String key)
    {
        return getStringLocalization(key, StringTranslate.getInstance().getCurrentLanguage());
    }

    public String getStringLocalization(String key, String lang)
    {
        String localizedString = "";
        Properties langPack = modLanguageData.get(lang);

        if (langPack != null) {
            if (langPack.getProperty(key) != null) {
                localizedString = langPack.getProperty(key);
            }
        }

        return localizedString;
    }

    public void addStringLocalization(String key, String value)
    {
        addStringLocalization(key, "en_US", value);
    }
    public void addStringLocalization(String key, String lang, String value)
    {
        Properties langPack=modLanguageData.get(lang);
        if (langPack==null) {
            langPack=new Properties();
            modLanguageData.put(lang, langPack);
        }
        langPack.put(key,value);
    }

    public void addStringLocalization(Properties langPackAdditions) {
        addStringLocalization(langPackAdditions, "en_US");
    }

    public void addStringLocalization(Properties langPackAdditions, String lang) {
        Properties langPack = modLanguageData.get(lang);
        if (langPack == null) {
            langPack = new Properties();
            modLanguageData.put(lang, langPack);
        }
        if (langPackAdditions != null) {
            langPack.putAll(langPackAdditions);
        }
    }

    public static void reloadLanguageTable()
    {
        // reload language table by forcing lang to null and reloading the properties file
        String lang = StringTranslate.getInstance().getCurrentLanguage();
        StringTranslate.getInstance().currentLanguage = null;
        StringTranslate.getInstance().setLanguage(lang);
    }


    public void addNameForObject(Object objectToName, String lang, String name)
    {
        String objectName;
        if (objectToName instanceof Item) {
            objectName=((Item)objectToName).getItemName();
        } else if (objectToName instanceof Block) {
            objectName=((Block)objectToName).getBlockName();
        } else if (objectToName instanceof ItemStack) {
            objectName=((ItemStack)objectToName).getItem().getItemNameIS((ItemStack)objectToName);
        //briman0094: added CreativeTabs to nameable objects
        } else {
            throw new IllegalArgumentException(String.format("Illegal object for naming %s",objectToName));
        }
        objectName+=".name";
        addStringLocalization(objectName, lang, name);
    }
    
    public void addNameForCreativeTab(CreativeTabs tab, String lang, String name)
    {
    	 String tName = "itemGroup." + tab.getTabLabel();
    	 addStringLocalization(tName, lang, name);
    }
    
    public void addNameForCreativeTab(CreativeTabs tab, String name)
    {
    	addNameForCreativeTab(tab, "en_US", name);
    }
    
    public static void addName(CreativeTabs tab, String name)
    {
    	instance().addNameForCreativeTab(tab, name);
    }

    public static void addName(Object objectToName, String name)
    {
        instance().addNameForObject(objectToName, "en_US", name);
    }

    public void loadLanguageTable(Properties languagePack, String lang)
    {
        Properties usPack=modLanguageData.get("en_US");
        if (usPack!=null) {
            languagePack.putAll(usPack);
        }
        Properties langPack=modLanguageData.get(lang);
        if (langPack==null) {
            return;
        }
        languagePack.putAll(langPack);
    }

    public void loadLocalization(String localizationFile, String lang, boolean isXML)
    {
        loadLocalization(this.getClass().getResource(localizationFile), lang, isXML);
    }

    public void loadLocalization(URL localizationFile, String lang, boolean isXML)
    {
        InputStream langStream = null;
        Properties langPack = new Properties();

        try    {
            langStream = localizationFile.openStream();

            if (isXML) {
                langPack.loadFromXML(langStream);
            }
            else {
                langPack.load(langStream);
            }

            addStringLocalization(langPack, lang);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally    {
            try    {
                if (langStream != null)    {
                    langStream.close();
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
