# HeadFirework (Paper Edition) User Guide

A Paper plugin that lets you craft a custom firework star from a player head, gunpowder, and dye. When the rocket explodes, that player's face appears at the blast site.

## Installation

- Drop `headfirework-paper-*.jar` into the server's `plugins` folder and restart.
- **No client-side installation is required** — players see the effect with a vanilla client.
- Supported version: Minecraft 26.2 / Paper

## Crafting

### 1. Craft a player-head firework star

Place the following in a crafting table.

| Ingredient | Amount | Role |
|---|---|---|
| Gunpowder | 1 | Required |
| Dye | 1 or more (multiple colors allowed) | Explosion color(s). Multiple colors produce a multi-color firework |
| Player head (with owner data) | 1 or more, same player only | Determines whose face appears. **Not consumed** — it stays in your inventory after crafting |

Add one of the following (optional) to change the explosion shape — omit for a small ball.

| Extra ingredient | Shape |
|---|---|
| (none) | Small ball |
| Fire charge | Large ball |
| Feather | Star |
| Gold nugget | Burst |

Optional effect ingredients (0-1 each):

- 1 Diamond → flicker effect
- 1 Glowstone dust → trail effect

The recipe fails if heads from different players are mixed in, or if any unsupported item is present.

### 2. Assemble a rocket

Place the following in a crafting table.

| Ingredient | Amount |
|---|---|
| Crafted star(s) | 1 or more (stars from different players can be combined) |
| Paper | same count as the stars |
| Gunpowder | 1-3 (affects flight duration) |

N stars produce **3×N rockets**. Combining stars from multiple players lets one launch show several people's faces at once.

## Explosion effect

When the rocket explodes, the face(s) of the player(s) behind the stars appear at the explosion point.

- The face grows from small to its target size, holds steady, then shrinks away while fading out — about 3 seconds by default (configurable)
- Multiple faces are arranged side by side
- The direction each face looks (north/east/south/west) can be configured

## Setting your own face direction (for regular players)

You don't need to be an operator to set which way your own face looks when it appears in a firework.

```
/headfirework myface <north|south|east|west>  … set the direction for your own face
/headfirework myface show                     … check your current setting
/headfirework myface reset                    … remove your personal setting (falls back to the server default)
/headfirework mygui                           … do the same thing through a chest GUI
```

- Anyone can use this — no admin permission required
- Once set, **whenever a firework crafted with your own head explodes**, your face appears facing the direction you chose — no matter who actually launches the rocket. Other players don't need to set anything themselves to see it correctly
- The direction used is **your setting at the moment the firework explodes**, not at the moment it was crafted. If you change your setting later, even fireworks you crafted earlier and stockpiled will use the new setting when launched
- When stars from multiple players are combined into one rocket, each face displays using its own owner's setting (the faces are still laid out in a row based on the server's default direction, so the layout itself stays consistent)
- If you haven't set a personal preference, the server-wide default (set by an admin, see below) is used

## Server-wide configuration (for admins)

Changing the server-wide default settings requires the `headfirework.admin` permission (operator by default).

### Via commands

```
/headfirework config scale <shape> <value>         … display size (shape: small_ball, large_ball, star, creeper, burst)
/headfirework config display_duration <ticks>       … total display duration
/headfirework config animation_duration <ticks>     … time spent growing in
/headfirework config fade_duration <ticks>          … time spent fading out
/headfirework config facing <north|south|east|west> … direction the face looks
/headfirework config show                           … list current values
```

Omit the value to reset that item to its default (e.g. `/headfirework config scale large_ball`). 20 ticks = 1 second.

### Via GUI

```
/headfirework gui
```

Opens a chest GUI where each value can be adjusted with −/+ buttons, with a per-item reset button. Click the compass item to cycle the facing direction. **The animation growth time (`animation_duration`) is not available in the GUI — use the command instead.**

### Debug command

```
/headfirework testhead <pitch> <roll> <yaw>
```

Spawns a single head at the given angles in front of you (disappears after 5 seconds). Not needed for normal play.

## Known limitations

- Since item displays have no built-in transparency, the fade-out is simulated by shrinking the model instead
- Multi-server behavior has not yet been verified
