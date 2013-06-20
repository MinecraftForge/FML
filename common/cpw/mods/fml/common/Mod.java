/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.common;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

import net.minecraft.item.ItemBlock;

import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;


/**
 * The new mod style in FML 1.3
 *
 * @author cpw
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Mod
{
    /**
     * The unique mod identifier for this mod
     */
    String modid();
    /**
     * A user friendly name for the mod
     */
    String name() default "";
    /**
     * A version string for this mod
     */
    String version() default "";
    /**
     * A simple dependency string for this mod (see modloader's "priorities" string specification)
     */
    String dependencies() default "";
    /**
     * Whether to use the mcmod.info metadata by default for this mod.
     * If true, settings in the mcmod.info file will override settings in these annotations.
     */
    boolean useMetadata() default false;

    /**
     * The acceptable range of minecraft versions that this mod will load and run in
     * The default ("empty string") indicates that only the current minecraft version is acceptable.
     * FML will refuse to run with an error if the minecraft version is not in this range across all mods.
     * @return A version range as specified by the maven version range specification or the empty string
     */
    String acceptedMinecraftVersions() default "";
    /**
     * An optional bukkit plugin that will be injected into the bukkit plugin framework if
     * this mod is loaded into the FML framework and the bukkit coremod is present.
     * Instances of the bukkit plugin can be obtained via the {@link BukkitPluginRef} annotation on fields.
     * @return The name of the plugin to load for this mod
     */
    String bukkitPlugin() default "";
    /**
     * Mods that this mod will <strong>not</strong> load with.
     * An optional comma separated string of (+|-)(*|modid[@value]) which specify mods that
     * this mod will refuse to load with, resulting in the game failing to start.
     * Entries can be prefixed with a + for a positive exclusion assertion, or - for a negative exclusion
     * assertion. Asterisk is the wildcard and represents <strong>all</strong> mods.
     *
     * The <strong>only</strong> mods that cannot be excluded are FML and MCP, trivially.
     * Other special values:
     * <ul>
     * <li>+f indicates that the mod will accept a minecraft forge environment.</li>
     * <li>-* indicates that the mod will not accept any other mods.</li>
     * </ul>
     *
     * Some examples:
     * <ul>
     * <li><em>-*,+f,+IronChest</em>: Will run only in a minecraft forge environment with the mod IronChests.
     * The -* forces all mods to be excluded, then the +f and +IronChest add into the "allowed list".</li>
     * <li><em>+f,-IC2</em>: Will run in a minecraft forge environment but will <strong>not</strong> run if
     * IndustrialCraft 2 (IC2) is loaded alongside.</li>
     * <li><em>-*</em>: Will not run if <strong>any</strong> othe mod is loaded except MCP/FML itself.</li>
     * </ul>
     *
     * If a mod is present on the excluded list, the game will stop and show an error screen. If the
     * class containing the {@link Mod} annotation has a "getCustomErrorException" method, it will be
     * called to retrieve a custom error message for display in this case. If two mods have a declared
     * exclusion which is matched, the screen that is shown is indeterminate.
     *
     * @return A string listing modids to exclude from loading with this mod.
     */
    String modExclusionList() default "";
    /**
     * Specifying this field allows for a mod to expect a signed jar with a fingerprint matching this value.
     * The fingerprint should be SHA-1 encoded, lowercase with ':' removed. An empty value indicates that
     * the mod is not expecting to be signed.
     *
     * Any incorrectness of the fingerprint, be it missing or wrong, will result in the {@link FingerprintWarning}
     * method firing <i>prior to any other event on the mod</i>.
     *
     * @return A certificate fingerprint that is expected for this mod.
     */
    String certificateFingerprint() default "";

    /**
     * The language the mod is authored in. This will be used to control certain libraries being downloaded.
     * Valid values are currently "java", "scala"
     *
     * @return The language the mod is authored in
     */
    String modLanguage() default "java";
    /**
     * An optional ASM hook class, that can be used to apply ASM to classes loaded from this mod. It is also given
     * the ASM tree of the class declaring {@link Mod} to with what it will.
     *
     * @return The name of a class to be loaded and executed. Must implement {@link IASMHook}.
     */
    String asmHookClass() default "";

    /**
     * Populate the annotated field with the mod instance.
     * @author cpw
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface Instance {
        /**
         * The mod object to inject into this field
         */
        String value() default "";
    }
    /**
     * Populate the annotated field with the mod's metadata.
     * @author cpw
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface Metadata {
        /**
         * The mod id specifying the metadata to load here
         */
        String value() default "";
    }
    
    /*******************************************************************************
     * The below per-event annotations have been deprecated and will be removed 
     * in 1.6. To gain access to the events use the standard {@link com.google.common.eventbus.Subscribe}
     * 
     * Your main mod class will automatically be registered to your internal EventBus.
     * See available events in {@link cpw.mods.fml.common.event}
     *******************************************************************************/
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface FingerprintWarning {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface PreInit {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface Init {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface PostInit {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface ServerAboutToStart {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface ServerStarting {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface ServerStarted {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface ServerStopping {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface ServerStopped {}
    @Deprecated
    @Retention(RUNTIME)
    @Target(METHOD)
    public @interface IMCCallback {}

    /*******************************************************************************
     * The below is deprecated on the bases that it was a idea that never got fully
     * implemented. It may return at any time but until it is fully implemented 
     * modders should not rely on it.
     *******************************************************************************/
    /**
     * Populate the annotated field with an instance of the Block as specified
     * @author cpw
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface Block {
        /**
         * The block's name
         */
        String name();
        /**
         * The associated ItemBlock subtype for the item (can be null for an ItemBlock)
         */
        Class<?> itemTypeClass() default ItemBlock.class;
    }
    /**
     * Populate the annotated field with an Item
     * @author cpw
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface Item {
        /**
         * The name of the item
         */
        String name();
        /**
         * The type of the item
         */
        String typeClass();
    }
}
