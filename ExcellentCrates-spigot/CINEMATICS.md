# Cinematic Crate Openings

A cinematic teleports the player to a dedicated stage and lets an **existing opening animation** —
`simple_roll`, `csgo`, or any other id under `openings/` — actually open the crate there. It renders
nothing itself and rolls no reward itself; it is a thin shell around whichever opening type you
already have.

Cinematics are **opening providers**, exactly like Inventory, Simple Roll and Selectable. One scene
is one file under `openings/cinematic/`, and a crate selects it by id in the crate's *Opening
Animation* picker — nothing special is needed to attach one.

---

## The flow

```
Player clicks the crate block                  ("the base crate")
  → ExcellentCrates checks permissions, cost, cooldown and limits
  → the cinematic starts; the crate does NOT pay out yet
  → the player is teleported to the scene's stage    ("the actual crate")
  → the configured opening animation runs there, exactly as it would
    for a crate configured with that id directly
  → that opening rolls and grants the reward itself
  → the player is teleported back to where they were, with the new items
```

The click never opens the crate directly and never renders anything itself. It starts the cinematic,
and the cinematic's whole job is: teleport out, let a real opening type run, teleport back.

### There is no double reward, and no teleport-back without a real finish

Cost is taken once, up front, exactly as with any other crate. The delegated opening rolls and grants
the reward through its own normal lifecycle — the same one it would use if a crate were configured
with that id directly — so nothing about reward-granting, stats, cooldowns or milestones is
duplicated or reimplemented.

The player is teleported back only once that delegated opening has genuinely finished: completed
normally, skipped by mass-opening, or cut short by a disconnect. There is no fixed wait — the
cinematic watches for the delegated opening to end and reacts, however long that takes.

### Scenes with no stage or no opening id

A scene needs both a stage location and an opening id to be playable. Without either, the reward is
rolled and granted directly — matching how a misconfigured crate never costs a player their key — and
the console names exactly what is missing.

### Delegating to another cinematic scene

Not allowed. A cinematic scene cannot appear in its own — or any other scene's — opening picker,
since delegating to another cinematic would either loop back on itself or chain teleports
indefinitely. The picker only lists genuine opening types.

### The camera is locked while the delegate runs

The player is put in spectator mode and bound to a stationary point above the stage for the entire
time the delegate opening is running — no movement, no free look. This isn't just presentation: a
normal, physically-colliding player standing next to a world-rendered display (as Simple Roll spawns)
gets shoved around by ordinary entity collision, which looks like the display flying away. Spectators
don't collide with anything, so locking the camera is what stops that.

The lock releases automatically the instant the delegate finishes — completed normally, skipped by
mass-opening, or cut short by a disconnect — at the same moment the player is teleported back.

---

## Configuration

A scene file looks like this:

```yaml
Name: Legendary Crate

Stage: 128.5,65.0,-45.5,0.0,0.0,world   # Where the player is teleported. Set with the capture tool.

Opening: simple_roll   # Which existing opening animation actually runs at the stage.

Camera_Height: 1.7   # How far above the stage the locked camera sits, in blocks. Default 1.7.

Crate_Block: 128,64,-46,world   # Optional. The block a block-anchored delegate renders on top of.

Model: crate_lvl1   # Optional. A ModelEngine blueprint id, spawned on Crate_Block at hand-off.

Model_Animation: open   # Which animation plays the instant the model spawns. Default "open".

Model_Yaw: 0.0   # Which way the model prop faces, in degrees. Default 0.

Start_Delay: 0   # Ticks after arrival before the model prop spawns and animates. Default 0.

Opening_Delay: 0   # Ticks after Start_Delay before the delegate opening starts. Default 0.

End_Delay: 0   # Ticks after the delegate finishes before the player is teleported back. Default 0.
```

`Stage` is captured in-game — walk to wherever you have built "the actual crate" (decorate it however
you like: a real chest, a resource-pack model, anything at all), look the direction you want the
player facing, and right-click with the capture tool. `Opening` is any id from `openings/inventory/`,
`openings/simple_roll/`, `openings/selectable/`, or any other opening type — the exact same ids the
crate's own *Opening Animation* picker already lists. `Camera_Height` only moves where the locked
camera sits above the stage; a negative value puts it below the stage instead of above.

`Crate_Block` is what fixes Simple Roll's reward display — and anything else that renders itself on
top of a block — actually appearing on top of your crate rather than floating in front of wherever
the locked camera happens to face. **It is set by the same capture tool as `Stage`**: right-click the
crate's actual block (a chest, whatever you've built) instead of clicking air, and both are captured
in the one click. Clicking air afterwards to fine-tune your stance never clears an already-captured
crate block. Delegates that don't care about a block — GUI-based ones like `csgo` — simply ignore it.

### The model prop

`Model` is entirely optional and requires the [ModelEngine](https://modelengine.info) plugin. When
set, the hand-off spawns that blueprint on `Crate_Block` (or, if no crate block was captured, at the
stage location itself) once `Start_Delay` has elapsed after the player arrives, and plays
`Model_Animation` on it once. `Model_Yaw` sets which way it faces, independent of whichever direction
the admin happened to be looking when they captured the stage — 0 = south, 90 = west, 180 = north,
270 = east, the same convention as a player's own facing.

Whether the model holds on its last frame afterwards, loops, or does anything else is entirely up to
how that animation's loop mode is authored in Blockbench — this plugin only starts the animation, it
never drives it frame by frame. A blueprint built with its reveal animation's loop mode set to `Hold`
therefore stays visually "open" for exactly as long as the delegate opening keeps running; the model is
removed the same instant the player is teleported back, whether the delegate finished normally, was
skipped by mass-opening, or was cut short by a disconnect.

A scene works exactly as before without a `Model` set, or on a server without ModelEngine installed —
this only replaces whatever built-in reveal the delegate opening renders with a custom model.

### Timing the reveal

Three delays, all in ticks (20 ticks = 1 second), stage the cinematic's beginning and end. All default
to `0`, which reproduces the original behavior exactly: the model appears, and the delegate opening
starts, the instant the player arrives, and the player is teleported back the instant the delegate
finishes.

- **`Start_Delay`** — ticks after arrival before the model prop spawns and plays its animation. This
  is the cinematic's "beginning" delay.
- **`Opening_Delay`** — ticks after the model's animation triggers before the delegate opening
  actually starts. Added on top of `Start_Delay`, not counted from arrival.
- **`End_Delay`** — ticks after the delegate opening finishes before the player is teleported back and
  the model prop removed. Lets the delegate's final frame, or the model's held pose, linger a moment
  before the camera lock releases.

For example, `Start_Delay: 40` (2 seconds) followed by `Opening_Delay: 20` (1 second) holds on an
empty stage for 2 seconds, then plays the model's opening animation, then starts the delegate opening
1 second after that — 3 seconds after the player arrived in total.

---

## Building one in game

1. **Editor → Cinematics → New Scene.** Give it an id.
2. **Opening Animation.** Pick which existing opening type actually runs — `simple_roll`, `csgo`,
   whatever you already have configured.
3. **Stage Location.** Click to get the capture tool. Stand where you want the player, look the
   direction they should face, and right-click — the crate's own block if you want Simple Roll's
   reward display anchored to it, or air if you don't need that.
4. **Camera Height.** Optional — defaults to 1.7 blocks above the stage. Change it if that vantage
   doesn't suit the space you built.
5. **Model.** Optional, and only shown if ModelEngine is installed. Pick a blueprint to spawn on the
   crate block at hand-off, which animation plays the instant it appears, and which way it faces.
6. **Start Delay / Opening Delay / End Delay.** Optional — all default to `0`. Stagger when the model
   appears, when the delegate opening actually starts, and how long the final scene lingers before the
   player is teleported back.
7. **Point a crate at the scene.** In that crate's editor, *Opening Animation* → the cinematic scene's
   id, same as picking any other opening type.
8. **Link the crate to a block**, same as any physical crate.

There is no frame timeline and no camera keyframes — the visuals are whichever opening type you
delegated to, unchanged from how they already work, plus whatever model prop you optionally configured.

---

## What happens when something is missing

Nothing here duplicates a reward or strands a player.

| Situation                                   | Result                                                        |
|----------------------------------------------|----------------------------------------------------------------|
| No stage location set                        | Reward is rolled and granted directly; console names the scene. |
| No opening id set                             | Same — direct grant, console names the scene.                  |
| The opening id does not exist                 | Same — direct grant, console names the missing id.             |
| The opening id is itself a cinematic scene    | Refused at that point too — direct grant, console explains why. |
| Player disconnects mid-delegate               | The delegated opening settles exactly as it would for a normal crate open (refund or grant, matching its own rules); nothing is left teleported or stuck. |
| Player dies mid-delegate                      | Same settlement, then the player is returned to their pre-click location once back online for it. |

---

## Requirements

None beyond the ExcellentCrates opening types you already have — this system orchestrates providers
that already exist, and works fully without any third-party dependency of its own.

The model prop is the one exception, and it's optional: it requires the
[ModelEngine](https://modelengine.info) plugin, plus a resource pack that carries the model's textures
out to players' clients. ModelEngine generates that pack itself (`plugins/ModelEngine/resource
pack.zip`, regenerated whenever its blueprints change) — you still need to host it somewhere players'
clients can download it from and point `server.properties`' `resource-pack` / `resource-pack-sha1` at
that URL and hash. A scene with no `Model` set needs none of this.
