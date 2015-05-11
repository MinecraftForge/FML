-------------------------------------------
Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "unrenamed" MCP source code (aka
srgnames) - this means that you will not be able to read them directly against
normal code.

Source pack installation information:

Standalone source installation
==============================

To install this source code for development purposes, extract this zip file.
It ships with a demonstration mod. Run 'gradlew setupDevWorkspace' to create
a gradle environment primed with FML. Run 'gradlew eclipse' or 'gradlew idea' to
create an IDE workspace of your choice.
Refer to ForgeGradle for more information about the gradle environment
Note: On macs or linux you run the './gradlew.sh' instead of 'gradlew'

Forge source installation
=========================
MinecraftForge ships with this code and installs it as part of the forge
installation process, no further action is required on your part.

For reference this is version @MAJOR@.@MINOR@.@REV@.@BUILD@ of FML
for Minecraft version @MCVERSION@.

LexManos' Install Video
=======================
https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be

For more details update more often refer to the Forge Forums:
http://www.minecraftforge.net/forum/index.php/topic,14048.0.html



-------------------------------------------
            Building a new mod
-------------------------------------------
To build a mod, place your source files in src/main/java. Any related resources such as
textures or mcmod.info files should be placed in src/main/resources. Run 'gradlew build'
to create a new jar. 

An example mod has been provided. You can find any built jars in build/libs. Examine your 
build.gradle to change mod name, mod ID, etc.

Additional information can be found at https://forgegradle.readthedocs.org/en/latest/
