BLOCK PATCHER
=============
Makes it safer to remove mods.

**WARNING: Doesn't work.**

Usage
-----
0. Have mods you want to remove.
1. Add this mod to your instance & launch it.
2. Remove the offending mods.
3. Edit blockpatcher.cfg: copy the relevent block names from the 'suggested' section to the 'blockPatches' section.
4. (Do you need to keep this mod around after loading the world? Not sure!)


Todo
----
* Metadata remapping
* Recover items from inventories
* Resurrection (It should be possible to update to the next version of Minecraft before the mod is ready, and then install the mod after it's ported, and everything remains.)
* Preserve items
* NBT transforming (Scripting? For when serialization formats change.)
* Preserve TileEntities
* Mocking (Removed blocks still retain some visual appearence, including (nearly) full usability for total-vanilla blocks.)
* GUIs
* Decide if it should automatically apply suggestions for removed mods


API
---
Implement the method "Block blockPatcher$getReplacement()" on your Block to describe a replacement.


License
-------
MIT.

