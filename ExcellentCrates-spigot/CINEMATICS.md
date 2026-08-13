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

Opening_Delay: 0   # Ticks after arrival before the delegate opening starts. Default 0.

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

### There is no crate model of its own

A cinematic renders nothing itself. Whatever the player sees at the stage is (a) the scenery you
built there by hand and (b) whichever delegate opening you pointed the scene at. If you want a custom
crate model standing on the stage, build it into the world as scenery — the cinematic will happily
teleport the player in front of it.

Cinematics could once spawn a [ModelEngine](https://modelengine.info) prop of their own, configured
via `Model`, `Model_Animation`, `Model_Yaw` and `Start_Delay`. That is gone: the plugin no longer
depends on ModelEngine in any form. Those four keys are simply ignored if an old scene file still
carries them, and are stripped from the file the next time the scene is saved.

### Timing the reveal

Two delays, both in ticks (20 ticks = 1 second), stage the cinematic's beginning and end. Both default
to `0`, which means the delegate opening starts the instant the player arrives, and the player is
teleported back the instant the delegate finishes.

- **`Opening_Delay`** — ticks after arrival before the delegate opening starts. Holds the player on
  the stage for a beat before anything happens, letting them take in the scenery you built.
- **`End_Delay`** — ticks after the delegate opening finishes before the player is teleported back.
  Lets the delegate's final frame linger a moment before the camera lock releases.

For example, `Opening_Delay: 40` (2 seconds) holds on the stage for 2 seconds, then starts the
delegate opening.

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
5. **Opening Delay / End Delay.** Optional — both default to `0`. Set how long the player waits on the
   stage before the delegate opening starts, and how long the final scene lingers before they are
   teleported back.
6. **Point a crate at the scene.** In that crate's editor, *Opening Animation* → the cinematic scene's
   id, same as picking any other opening type.
7. **Link the crate to a block**, same as any physical crate.

There is no frame timeline and no camera keyframes — the visuals are whichever opening type you
delegated to, unchanged from how they already work, seen from the stage you built.

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

None. This system orchestrates opening providers that already exist, and has no third-party dependency
of its own — no ModelEngine, and no resource pack needed for the cinematic itself.

If the scenery you build on the stage happens to use custom models, that is between you, whatever
plugin provides them, and your resource pack. The cinematic neither knows nor cares.
