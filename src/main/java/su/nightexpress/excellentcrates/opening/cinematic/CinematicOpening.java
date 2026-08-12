package su.nightexpress.excellentcrates.opening.cinematic;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.opening.Opening;
import su.nightexpress.excellentcrates.api.opening.OpeningProvider;
import su.nightexpress.excellentcrates.crate.cost.Cost;
import su.nightexpress.excellentcrates.crate.impl.CrateSource;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.opening.world.WorldOpening;
import su.nightexpress.excellentcrates.util.pos.WorldPos;

/**
 * The fourth opening type: teleports the player to a dedicated stage and hands the actual opening
 * off to whichever existing opening animation the scene names — {@code simple_roll}, {@code csgo},
 * or any other id under {@code openings/}.
 *
 * <p>This is deliberately a thin shell. It does not roll a reward, does not render anything, and
 * does not decide when the crate opens — the delegate opening does all of that itself, exactly as it
 * would for a crate configured with that id directly. This class's only job is: teleport out, start
 * the delegate, and once the delegate finishes, teleport back. See {@link CinematicManager} for the
 * "once the delegate finishes" half of that, which happens after this object is no longer the
 * player's registered opening and therefore cannot tick itself back to life.
 *
 * <p>Two outcomes therefore exist:
 * <ul>
 *   <li><b>Hand-off succeeds.</b> This opening's own lifecycle ends right there — it is swapped out
 *       of {@code OpeningManager} for the delegate and never ticks again. All reward-granting,
 *       stats, cooldown and milestones happen on the <i>delegate's</i> lifecycle, not this one.</li>
 *   <li><b>The scene is broken</b> (no stage, no opening id, an unknown id, or an id that would
 *       recurse into another cinematic scene). No teleport happens; the reward is rolled and granted
 *       directly by this opening instead, exactly like {@code DummyOpening}, so a misconfigured scene
 *       never costs a player their key.</li>
 * </ul>
 *
 * <p><b>Camera lock.</b> While the delegate runs, the player is put in spectator mode and bound to a
 * stationary marker entity hovering above the stage, which fully locks their camera — no movement, no
 * free look — until the delegate finishes. This is not just presentation: a normal, colliding player
 * standing right next to a world-rendered reward display (as Simple Roll spawns) gets shoved around by
 * ordinary entity-collision physics, which reads as the display "flying away". Spectators do not
 * collide with anything, so locking the camera this way is what stops that.
 */
public class CinematicOpening extends WorldOpening {

    private final CinematicScene scene;

    private boolean started;
    private boolean instaRolled;
    private boolean broken;

    public CinematicOpening(@NotNull CratesPlugin plugin,
                            @NotNull Player player,
                            @NotNull CrateSource source,
                            @Nullable Cost cost,
                            @NotNull CinematicScene scene) {
        super(plugin, player, source, cost);
        this.scene = scene;
    }

    @Override
    public long getInterval() {
        return 1L;
    }

    @Override
    protected void onStart() {
        // Intentionally empty - the hand-off happens on the first tick (see onTick), not here.
        // Mass opening calls instaRoll() immediately after start(); starting eagerly here would
        // teleport the player out and back for every crate in a 30-crate mass open.
    }

    @Override
    protected void onTick() {
        if (this.started) return; // Unreachable in practice - see the class comment - but cheap to guard.
        this.started = true;

        this.handOff();
    }

    /**
     * Validates the scene, then either teleports the player to the stage and starts the delegate
     * opening, or falls back to granting the reward directly.
     */
    private void handOff() {
        Location stageLocation = this.scene.getStage().toLocation();
        if (!this.scene.isPlayable() || stageLocation == null) {
            this.failBroken("its stage is not set, or the stage's world is not loaded");
            return;
        }

        String openingId = this.scene.getOpeningId();
        OpeningProvider provider = this.plugin.getOpeningManager().getProviderById(openingId);
        if (provider == null) {
            this.failBroken("its opening animation '" + openingId + "' does not exist");
            return;
        }
        if (provider instanceof CinematicProvider) {
            // A cinematic delegating to another cinematic either loops back on itself or chains
            // teleports indefinitely - neither is a meaningful thing to configure.
            this.failBroken("its opening animation '" + openingId + "' is itself a cinematic scene, which is not allowed");
            return;
        }

        Block block = this.source.getBlock();
        WorldPos blockPos = block == null ? null : WorldPos.from(block);
        if (blockPos != null) {
            this.hideHologram(blockPos);
        }

        Location returnLocation = this.player.getLocation().clone();
        GameMode returnGameMode = this.player.getGameMode();

        // Locking the camera above the stage rather than at it gives a slightly better vantage over
        // whatever the delegate renders there, and keeps the player's own hitbox - even though
        // spectators don't collide with anything - clear of ground-level scenery.
        Location cameraLocation = stageLocation.clone().add(0D, this.scene.getCameraHeight(), 0D);
        this.player.teleport(cameraLocation);

        ArmorStand cameraMarker = this.spawnCameraMarker(cameraLocation);
        this.player.setGameMode(GameMode.SPECTATOR);
        this.player.setSpectatorTarget(cameraMarker);

        // The delegate renders and rolls its own reward against the same crate, at the stage rather
        // than wherever the player originally clicked - so it gets a source built from the scene's
        // own crate block, if one was captured, rather than the block the player actually clicked.
        // Simple Roll (and anything else that renders on top of a block) needs this to sit on the
        // stage's crate rather than floating wherever the locked camera happens to face; without it,
        // it falls back to that floating position gracefully rather than erroring.
        WorldPos stageBlockPos = this.scene.hasCrateBlock() ? this.scene.getCrateBlock() : null;
        CrateSource stageSource = new CrateSource(this.crate, null, stageBlockPos);
        Opening delegate = provider.createOpening(this.player, stageSource, null);

        this.plugin.getOpeningManager().swapOpening(this.player, delegate);
        this.plugin.getCinematicManager().awaitReturn(this.player, returnLocation, returnGameMode, this.crate, blockPos, cameraMarker);

        // Past this point the crate has genuinely been opened by the delegate; refunding here would
        // be wrong since the delegate is what now owns (and will itself refund or grant) the cost.
        this.setRefundable(false);
    }

    /**
     * An invisible, collision-free marker the player's camera is bound to via
     * {@link Player#setSpectatorTarget}. It never moves once placed, which is what makes the camera
     * "completely locked" rather than merely parked.
     */
    @NotNull
    private ArmorStand spawnCameraMarker(@NotNull Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setMarker(true);       // No hitbox, so it cannot be interacted with or collided.
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);  // Never written to the region file.
            stand.setCollidable(false);
        });
    }

    private void failBroken(@NotNull String reason) {
        this.broken = true;
        this.setRefundable(false);
        this.addReward(this.crate.rollReward(this.player));

        this.plugin.warn("Cinematic scene '" + this.scene.getId() + "' could not run because " + reason
            + ". Granting the reward without a stage.");
    }

    @Override
    public void instaRoll() {
        this.setRefundable(false);
        this.instaRolled = true;
        this.addReward(this.crate.rollReward(this.player));
        this.stop();
    }

    @Override
    public boolean isCompleted() {
        return this.instaRolled || this.broken;
    }

    @Override
    protected void onComplete() {
        // Reward delivery, stats and post-open commands are handled by AbstractOpening#completeOpening,
        // reached through the normal stop() -> onStop() path for the insta-roll and broken-fallback
        // cases. The hand-off case never reaches here at all - see the class comment.
    }

    @NotNull
    public CinematicScene getScene() {
        return this.scene;
    }
}
