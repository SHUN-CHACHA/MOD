# HeadFirework — User Guide

**HeadFirework** is a Fabric mod for Minecraft that adds a special firework rocket. When it explodes, the face of the player whose head was used to craft it appears at the explosion point, scaled to match the firework's shape, growing in from small before it fades out.

- Works the same way in **singleplayer** and on **multiplayer servers** — only the server (or the singleplayer instance) needs the mod installed. Players connecting to a server do **not** need to install anything themselves.
- Compatible with both **Creative** and **Survival** mode.

## Requirements

- Minecraft 26.2
- Fabric Loader
- Fabric API

## How to Craft

### 1. Player Head Firework Star

Combine the following in a crafting table (shapeless — any arrangement works):

| Ingredient | Required? | Effect |
|---|---|---|
| Gunpowder | Required | Base ingredient |
| Dye (one or more) | Required | Sets the firework's color(s). Using multiple dyes creates a multi-color explosion |
| Player Head (with owner data) | Required | The face that will appear in the explosion |
| Fire Charge **or** Feather **or** Gold Nugget | Optional — pick **one** | Fire Charge → Large Ball shape · Feather → Star shape · Gold Nugget → Burst shape |
| Diamond | Optional | Adds a twinkle (sparkle) effect |
| Glowstone Dust | Optional | Adds a trail effect |

If none of Fire Charge / Feather / Gold Nugget is used, the star defaults to a **Small Ball** shape.

The player head must already have owner (skin) data attached — for example, one obtained with `/give <player> minecraft:player_head[minecraft:profile="<name>"]` or picked up from a player-placed head.

### 2. Rocket

Combine in a crafting table (shapeless):

- The Player Head Firework Star (from step 1)
- Paper
- Gunpowder (1–3)

The number of gunpowder used controls flight duration, same as a vanilla firework rocket (1 = short flight, 3 = long flight).

## What Happens When It Explodes

1. The rocket flies up like a normal firework.
2. At the moment of explosion, the head of the player used in the star's recipe appears at the explosion point.
3. The face starts small and **grows** to its full size over a short animation.
4. It holds at full size for the configured display duration.
5. Near the end of the display duration, it **shrinks back down to nothing** (a fade-out effect achieved via scale, since item displays don't support transparency).

The display size depends on the firework's shape (Small Ball, Large Ball, Star, Creeper, Burst) and can be tuned — see below.

The firework star and rocket are automatically named after the player whose head was used (e.g. "Steve's Firework"). If the head has no owner name, it's labeled as an unnamed player's firework.

## Configuring the Mod

### In-game GUI

Press **Ctrl+J** to open the HeadFirework settings screen. From here you can adjust, with a slider for each:

- Small Ball size
- Large Ball size
- Star / Burst size
- Display duration (in ticks)
- Fade-out duration (in ticks)

Each slider has its own **R** (reset) button next to it, which resets only that value to its default — other values you've changed stay as they are. Click **Apply** to send the changes to the server (works on multiplayer too, since it sends the same commands the server already understands — no client-side mod needed on the server's other players).

### Commands (require operator/gamemaster permission)

```
/headfirework config scale <small_ball|large_ball|star|creeper|burst> <value>
/headfirework config display_duration <ticks>
/headfirework config animation_duration <ticks>
/headfirework config fade_duration <ticks>
/headfirework config show
```

- `scale` — sets the display size for a given explosion shape
- `display_duration` — total time (in ticks, 20 ticks = 1 second) the face stays visible, including grow-in and fade-out
- `animation_duration` — how long the grow-in animation takes
- `fade_duration` — how long the shrink/fade-out takes, counted from the end of `display_duration`
- `show` — prints the current settings

Settings are saved to `config/headfirework.json` and persist across restarts.

## Language

The mod automatically follows Minecraft's own language setting (Options → Language). Switching between **English (US)** and **日本語 (Japanese)** in-game updates the mod's menu, keybind name, and command messages to match — no separate toggle needed inside the mod.

## Notes / Known Limitations

- True transparency fading isn't supported (Minecraft's item display entities don't have an alpha/opacity option), so the fade-out is achieved by shrinking the face to nothing instead.
- Multiplayer dedicated server testing is still limited — singleplayer/integrated server behavior has been verified thoroughly.

## Version

This guide corresponds to **HeadFirework v1.1.0**.
