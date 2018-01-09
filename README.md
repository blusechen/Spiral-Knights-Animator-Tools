# About the project
This is a revamp of the old, crappy tool dubbed [Spiral Knights Model Converter](https://github.com/XanTheDragon/Spiral-Knights-Model-Converter).

The code runs as a precompiled JAR file.

# What can it do?

This program is designed using ThreeRings's base libraries: Clyde, Narya, Nenya, and Vilya. Clyde is the primary unit designed for handling their model format.

This tool can convert any model in Spiral Knights to a usable format. Please note that at the moment this *only* converts geometry. Any effects cannot be converted at this time.

**Please note:** Knight models (from `rsrc/character/pc/model.dat`) can **not** be converted. This is because they are a localized model type to Spiral Knights known internally as `ProjectXModelConfig`. This implementation was specifically designed for player usage, meaning it has an entirely custom implementation designed for things like the legs being independent from the upper body, animations for swinging swords that only affect needed parts of the model, etc. **You need to convert `rsrc/character/npc/crew/model.dat` rather than this.**
