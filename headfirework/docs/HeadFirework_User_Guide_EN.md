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
| Player Head (with owner data) | Required | The face that will appear in the explosion. Multiple heads from the same player can be added at once (they aren't consumed, and no matter how many you add, you always get exactly one star) |
| Fire Charge **or** Feather **or** Gold Nugget | Optional — pick **one** | Fire Charge → Large Ball shape · Feather → Star shape · Gold Nugget → Burst shape |
| Diamond | Optional | Adds a twinkle (sparkle) effect |
| Glowstone Dust | Optional | Adds a trail effect |

If none of Fire Charge / Feather / Gold Nugget is used, the star defaults to a **Small Ball** shape.

The player head must already have owner (skin) data attached — for example, one obtained with `/give <player> minecraft:player_head[minecraft:profile="<name>"]` or picked up from a player-placed head.

### 2. Rocket

Combine in a crafting table (shapeless):

- Stars from step 1 — **stars from different players can be mixed together**, letting one rocket show multiple people's faces at once. You can also use several stars from the same player.
- Paper (same count as the stars used)
- Gunpowder (1–3)

The number of gunpowder used controls flight duration, same as a vanilla firework rocket (1 = short flight, 3 = long flight). See the [Crafting Recipes guide](./HeadFirework_Crafting_Recipes_EN.md) for worked examples (how many rockets you get, etc.).

## What Happens When It Explodes

1. The rocket flies up like a normal firework.
2. At the moment of explosion, the head(s) of the player(s) used in the star's recipe appear at the explosion point. If there are multiple faces, they're shown side by side at the same time.
3. Each face starts small and **grows** to its full size over a short animation.
4. It holds at full size for the configured display duration.
5. Near the end of the display duration, it **shrinks back down to nothing** (a fade-out effect achieved via scale, since item displays don't support transparency).

The display size depends on the firework's shape (Small Ball, Large Ball, Star, Creeper, Burst) and can be tuned — see below.

The firework star and rocket are automatically named after the player whose head was used (e.g. "Steve's Firework"). If the head has no owner name, it's labeled as an unnamed player's firework.

## Facing Direction

The direction a face looks (North / South / East / West) is decided in this priority order: **Forced mode (admin)** > **Per-player setting** > **Server default (admin)**.

### Setting your own facing direction (any player, commands only)

```
/headfirework myface <north|south|east|west>  … set the direction your own firework faces
/headfirework myface show                     … check your current setting
/headfirework myface reset                    … clear your setting and fall back to the server default (or forced mode)
```

- **Anyone can use this — no operator permission required.**
- What applies is **your setting at the moment the firework explodes**, not when it was crafted — even a firework you crafted earlier and stockpiled will use whatever setting you have when it goes off.
- If you haven't set a personal facing, the server default is used.
- While an admin has "forced mode" active, it overrides your personal setting. Your personal setting isn't deleted, so turning forced mode off restores it (or the default, if you never set one).
- When several players' stars are combined into one rocket, each face is shown facing according to that player's own setting.

### In-game GUI (server admins)

Press **Ctrl+J** to open HeadFirework's admin settings screen. **As of v1.1.4, operator (gamemaster) permission is required.** A player without that permission who presses Ctrl+J will not see the screen open — instead they get the message "You need OP permission to open this settings screen."

From here you can adjust:

- Small Ball size
- Large Ball size
- Star / Burst size
- Display duration (in ticks)
- Fade-out duration (in ticks)
- The server's default facing direction
- Forced mode (on/off and direction)

Each slider has its own **R** (reset) button next to it, which resets only that value to its default — other values you've changed stay as they are.

Click **Apply** to send the changes to the server (works on multiplayer too, since it sends the same commands the server already understands — no client-side mod needed on the server's other players).

### Commands (require operator/gamemaster permission)

```
/headfirework config scale <small_ball|large_ball|star|creeper|burst> <value>
/headfirework config display_duration <ticks>
/headfirework config animation_duration <ticks>
/headfirework config fade_duration <ticks>
/headfirework config facing <north|south|east|west>
/headfirework config force_facing <north|south|east|west|off>
/headfirework config show
```

- `scale` — sets the display size for a given explosion shape
- `display_duration` — total time (in ticks, 20 ticks = 1 second) the face stays visible, including grow-in and fade-out
- `animation_duration` — how long the grow-in animation takes
- `fade_duration` — how long the shrink/fade-out takes, counted from the end of `display_duration`
- `facing` — sets the server's default facing direction, used for any player who hasn't set their own
- `force_facing` — temporarily forces **every player's** facing to the given direction. Per-player settings aren't deleted; `off` simply restores each player's own setting (or the default)
- `show` — prints the current settings

Settings are saved to `config/headfirework.json` and persist across restarts.

## Language

The mod automatically follows Minecraft's own language setting (Options → Language). Switching between **English (US)** and **日本語 (Japanese)** in-game updates the mod's menu, keybind name, and command messages to match — no separate toggle needed inside the mod.

## Notes / Known Limitations

- True transparency fading isn't supported (Minecraft's item display entities don't have an alpha/opacity option), so the fade-out is achieved by shrinking the face to nothing instead.
- Multiplayer dedicated server testing is still limited — singleplayer/integrated server behavior has been verified thoroughly.

## Version

This guide corresponds to **HeadFirework v1.1.4**.
