package cpw.mods.fml.common.registry;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;

import com.google.common.collect.ImmutableMap;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoadererState;
import cpw.mods.fml.common.ModContainer;

public class FMLControlledNamespacedRegistry<I> extends RegistryNamespaced {
    private final Class<I> superType;
    private String optionalDefaultName;
    private I optionalDefaultObject;
    private int maxId;
    private int minId;
    private char discriminator;
    // aliases redirecting legacy names to the actual name, may need recursive application to find the final name.
    // these need to be registry specific, it's possible to only have a loosely linked item for a block which may get renamed by itself.
    private final Map<String, String> aliases = new HashMap<String, String>();

    FMLControlledNamespacedRegistry(String optionalDefault, int maxIdValue, int minIdValue, Class<I> type, char discriminator)
    {
        this.superType = type;
        this.discriminator = discriminator;
        this.optionalDefaultName = optionalDefault;
        this.maxId = maxIdValue;
        this.minId = minIdValue;
    }

    @SuppressWarnings("unchecked")
    void set(FMLControlledNamespacedRegistry<I> registry)
    {
        if (this.superType != registry.superType) throw new IllegalArgumentException("incompatible registry");

        this.discriminator = registry.discriminator;
        this.optionalDefaultName = registry.optionalDefaultName;
        this.maxId = registry.maxId;
        this.minId = registry.minId;
        this.aliases.clear();
        this.aliases.putAll(registry.aliases);
        underlyingIntegerMap = new ObjectIntIdentityMap();
        registryObjects.clear();

        for (I thing : (Iterable<I>) registry)
        {
            addObjectRaw(registry.getId(thing), registry.getNameForObject(thing), thing);
        }
    }

    // public api

    /**
     * Add an object to the registry, trying to use the specified id.
     *
     * @deprecated register through {@link GameRegistry} instead.
     */
    @Override
    @Deprecated
    public void addObject(int id, String name, Object thing)
    {
        GameData.getMain().register(thing, name, id);
    }

    /**
     * DANGEROUS! EVIL! DO NOT USE!
     *
     * @deprecated register through {@link GameRegistry} instead.
     */
    @Override
    @Deprecated
    public void putObject(Object objName, Object obj)
    {
        String name = (String) objName;
        I thing = (I) obj;

        if (name == null) throw new NullPointerException("Can't use a null-name for the registry.");
        if (name.isEmpty()) throw new IllegalArgumentException("Can't use an empty name for the registry.");
        if (thing == null) throw new NullPointerException("Can't add null-object to the registry.");

        String existingName = getNameForObject(thing);

        if (existingName == null)
        {
            FMLLog.bigWarning("Ignoring putObject(%s, %s), not resolvable", name, thing);
        }
        else if (existingName.equals(name))
        {
            FMLLog.bigWarning("Ignoring putObject(%s, %s), already added", name, thing);
        }
        else
        {
            FMLLog.bigWarning("Ignoring putObject(%s, %s), adding alias to %s instead", name, thing, existingName);
            addAlias(name, existingName);
        }
    }

    /**
     * Fetch the object identified by the specified name or the default object.
     *
     * For blocks the default object is the air block, for items it's null.
     *
     * @param name Unique name identifying the object.
     * @return Registered object of the default object if it wasn't found-
     */
    @Override
    public I getObject(String name)
    {
        I object = getRaw(name);
        return object == null ? this.optionalDefaultObject : object;
    }

    /**
     * Fetch the object identified by the specified id or the default object.
     *
     * For blocks the default object is the air block, for items it's null.
     *
     * @param id ID identifying the object.
     * @return Registered object of the default object if it wasn't found-
     */
    @Override
    public I getObjectById(int id)
    {
        I object = getRaw(id);
        return object == null ? this.optionalDefaultObject : object;
    }

    /**
     * @deprecated use getObjectById instead
     */
    @Deprecated
    public I get(int id)
    {
        return getObjectById(id);
    }

    /**
     * @deprecated use getObject instead
     */
    @Deprecated
    public I get(String name)
    {
        return getObject(name);
    }

    /**
     * Get the id for the specified object.
     *
     * Don't hold onto the id across the world, it's being dynamically re-mapped as needed.
     *
     * Usually the name should be used instead of the id, if using the Block/Item object itself is
     * not suitable for the task.
     *
     * @param thing Block/Item object.
     * @return Block/Item id or -1 if it wasn't found.
     */
    public int getId(I thing)
    {
        return getIDForObject(thing);
    }

    /**
     * Get the object identified by the specified id.
     *
     * @param id Block/Item id.
     * @return Block/Item object or null if it wasn't found.
     */
    public I getRaw(int id)
    {
        return superType.cast(super.getObjectById(id));
    }

    /**
     * Get the object identified by the specified name.
     *
     * @param name Block/Item name.
     * @return Block/Item object or null if it wasn't found.
     */
    public I getRaw(String name)
    {
        I ret = superType.cast(super.getObject(name));

        if (ret == null) // no match, try aliases recursively
        {
            name = aliases.get(name);

            if (name != null) return getRaw(name);
        }

        return ret;
    }

    /**
     * Determine if the registry has an entry for the specified name.
     *
     * Aliased names will be resolved as well.
     *
     * @param name Object name to check.
     * @return true if a matching entry was found.
     */
    @Override
    public boolean containsKey(String name)
    {
        boolean ret = super.containsKey(name);

        if (!ret) // no match, try aliases recursively
        {
            name = aliases.get(name);

            if (name != null) return containsKey(name);
        }

        return ret;
    }

    /**
     * Get the id for the specified object.
     *
     * Don't hold onto the id across the world, it's being dynamically re-mapped as needed.
     *
     * Usually the name should be used instead of the id, if using the Block/Item object itself is
     * not suitable for the task.
     *
     * @param itemName Block/Item registry name.
     * @return Block/Item id or -1 if it wasn't found.
     */
    public int getId(String itemName)
    {
        I obj = getRaw(itemName);
        if (obj == null) return -1;

        return getId(obj);
    }

    /**
     * @deprecated use containsKey instead
     */
    @Deprecated
    public boolean contains(String itemName)
    {
        return containsKey(itemName);
    }

    // internal

    @SuppressWarnings("unchecked")
    public void serializeInto(Map<String, Integer> idMapping) // for saving
    {
        for (I thing : (Iterable<I>) this)
        {
            idMapping.put(discriminator+getNameForObject(thing), getId(thing));
        }
    }

    public Map<String, String> getAliases() // for saving
    {
        return ImmutableMap.copyOf(aliases);
    }

    /**
     * Add the specified object to the registry.
     *
     * @param id ID to use if available, auto-assigned otherwise.
     * @param name Name to use, prefixed by the mod id.
     * @param thing Object to add.
     * @param availabilityMap Map marking available IDs for auto assignment.
     * @return ID eventually allocated.
     */
    int add(int id, String name, I thing, BitSet availabilityMap)
    {
        if (name == null) throw new NullPointerException("Can't use a null-name for the registry.");
        if (name.isEmpty()) throw new IllegalArgumentException("Can't use an empty name for the registry.");
        if (thing == null) throw new NullPointerException("Can't add null-object to the registry.");
        if (name.equals(optionalDefaultName))
        {
            this.optionalDefaultObject = thing;
        }

        int idToUse = id;
        if (id == 0 || availabilityMap.get(id))
        {
            idToUse = availabilityMap.nextClearBit(minId);
        }
        if (idToUse > maxId)
        {
            throw new RuntimeException(String.format("Invalid id %d - maximum id range exceeded.", id));
        }

        ModContainer mc = Loader.instance().activeModContainer();
        if (mc != null && Loader.instance().isInState(LoaderState.PREINITIALIZATION))
        {
            String prefix = mc.getModId();
            name = prefix + ":"+ name;
        }

        if (getRaw(name) == thing) // already registered, return prev registration's id
        {
            FMLLog.bigWarning("The object %s has been registered twice for the same name %s.", thing, name);
            return getId(thing);
        }
        if (getRaw(name) != null) // duplicate name, will crash later due to the BiMap
        {
            FMLLog.bigWarning("The name %s has been registered twice, for %s and %s.", name, getRaw(name), thing);
        }
        if (getId(thing) >= 0) // duplicate object, will crash later due to the BiMap
        {
            FMLLog.bigWarning("The object %s has been registered twice, using the names %s and %s.", thing, getNameForObject(thing), name);
        }
        if (GameData.isFrozen(this))
        {
            FMLLog.bigWarning("The object %s (name %s) is being added too late.", thing, name);
        }

        addObjectRaw(idToUse, name, thing);

        FMLLog.finer("Registry add: %s %d %s", name, idToUse, thing);
        return idToUse;
    }

    void addAlias(String from, String to)
    {
        aliases.put(from, to);
        FMLLog.finer("Registry alias: %s -> %s", from, to);
    }

    @SuppressWarnings("unchecked")
    Map<String,Integer> getEntriesNotIn(FMLControlledNamespacedRegistry<I> registry)
    {
        Map<String,Integer> ret = new HashMap<String, Integer>();

        for (I thing : (Iterable<I>) this)
        {
            if (!registry.field_148758_b.containsKey(thing)) ret.put(getNameForObject(thing), getId(thing));
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    void dump()
    {
        List<Integer> ids = new ArrayList<Integer>();

        for (I thing : (Iterable<I>) this)
        {
            ids.add(getId(thing));
        }

        // sort by id
        Collections.sort(ids);

        for (int id : ids)
        {
            I thing = getRaw(id);
            FMLLog.finer("Registry: %s %d %s", getNameForObject(thing), id, thing);
        }
    }

    /**
     * Version of addObject not using the API restricting overrides.
     */
    private void addObjectRaw(int id, String name, I thing)
    {
        if (name == null) throw new NullPointerException();
        if (thing == null) throw new NullPointerException();

        underlyingIntegerMap.func_148746_a(thing, id); // obj <-> id
        super.putObject(ensureNamespaced(name), thing); // name <-> obj
    }
}
