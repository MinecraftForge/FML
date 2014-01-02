This is Forge Mod Loader, or FML for short, by cpw.

More information can be found at https://github.com/MinecraftForge/FML/wiki

It is a clean reimplementation of a mod loading system for client and sercer.

It can be installed on its own, or as part of Minecraft Forge.

----------------------
About Forge Mod Loader
----------------------

Environments and compatibility
==============================
FML covers the two main environments: client and server. All
environments share the main mod loading code base, with additional varying hooks
based on the specific environment.

Minecraft Forge in all cases bundles FML as it's modloading technology of choice
because FML is open source, freely distributable, and can be easily updated by
contributors through github.

Notable integrations
====================
Optifine has FML compatibility. It varies from Optifine release to release, however
in general it will work well alongside an FML or 
Minecraft Forge installation. FML will detect and ensure the good operation of
Optifine (you can see it in your client as an additional data line on the 
bottom left).

Mod information
===============
FML exposes the mod information through a mod list visible on the main screen as
well as some small branding enhancements. For full data mods need to provide an
information file. This file is a standard format so hopefully tools providing
launch capabilities can also leverage this content.

-------------------------------
Binary installation information
-------------------------------
If you have downloaded a binary jar file you can install it as follows (client
or server):

Installation
============
Installation is handled by the FML installer, which can be downloaded at http://files.minecraftforge.net/fml/

Forge Installation
==================
This code also ships as a part of Minecraft Forge. You do not need to install it
separately from your Minecraft Forge installation. Minecraft Forge contains the
exact same code as this. Generally, you should not install FML if you are also
installing Minecraft Forge.

-------------------------------------------
Source installation information for modders
-------------------------------------------

Open a command prompt, navigate to the folder you extracted the FML source distribution to, and run "gradlew setupDevWorkspace" (without the quotes)


Forge source installation
=========================
MinecraftForge ships with this code and installs it as part of the forge
installation process, no further action is required on your part.

For reference this is version @MAJOR@.@MINOR@.@REV@.@BUILD@ of FML
for Minecraft version @MCVERSION@.
