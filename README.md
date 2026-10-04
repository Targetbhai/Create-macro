# Create Macro (Fabric, Minecraft 1.21.11, Loader 0.19.5)

Client-side mod. Press **M** in-game to open the macro GUI (rebindable in Controls).

## Using it
- **+ New macro** -> opens the editor. Each macro row in the list has an **ON/OFF** toggle and a delete (X) button.
- **Trigger key**: click it, then press any key or mouse button.
- **Holding**: click to cycle (Any item, Sword, Ender pearl, End crystal, Obsidian, Respawn anchor, Glowstone, Mace, Elytra, Spear, Axe, Totem, Golden apple, Shield, Wind charge, Firework, Cobweb, Water bucket).
  The *Custom items* box overrides it: comma-separated ids or fragments, e.g. `totem, trident, minecraft:bow`.
- **Steps**: click the key button, press a key / mouse button. Click it again to add more keys to the same step. **Clear** resets the step.
  The slider sets the delay (0-100 ms) that step's keys are held before the next step runs.
- **+ Add more** adds another step. Scroll with the mouse wheel when there are many.
- Macros are saved to `config/createmacro.json`.

## Build
Needs JDK 21 and Gradle 8.14+ (or open in IntelliJ).

    gradle wrapper --gradle-version 8.14.3   # one time
    ./gradlew build

The jar is in `build/libs/`. Drop it in `mods/` together with Fabric API.

If Gradle can't resolve `fabric_version` or `loom_version`, set them in `gradle.properties` to the
current ones for 1.21.11 from https://fabricmc.net/develop/.

## Notes
- Steps work by pressing the game's own key bindings, so a key only does something if it is bound in
  Options > Controls (hotbar 1-9, use item, attack, swap hands, drop, ...).
- Minecraft processes input once per tick (50 ms), so delays below that are effectively rounded to ticks.
- Many multiplayer servers ban macro mods. Check the server's rules before using it there.
