package su.nightexpress.excellentcrates.opening.cinematic;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.api.crate.Reward;
import su.nightexpress.excellentcrates.crate.cost.Cost;
import su.nightexpress.excellentcrates.crate.impl.CrateSource;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.opening.world.WorldOpening;
import su.nightexpress.excellentcrates.util.pos.WorldPos;

/**
 * The fourth opening type: presents the reward through a scripted camera scene.
 *
 * <p>Like the other three, this class only changes <i>how the reveal is presented</i>. Cost,
 * permissions, cooldowns, limits, milestones and reward rolling all stay in
 * {@link su.nightexpress.excellentcrates.opening.AbstractOpening} and the crate itself.
 *
 * <p>Playback starts on the first tick rather than in {@link #onStart()} — the same lazy pattern
 * {@code SimpleRollOpening} uses — because mass opening calls {@link #instaRoll()} immediately
 * after {@code start()}. Starting the scene eagerly would teleport the player into spectator mode
 * and back out again for every crate in a 30-crate mass open.
 */
public class CinematicOpening extends WorldOpening {

    private final CinematicScene scene;
    private final Reward         reward;
    private final ScenePlayback  playback;

    private boolean started;
    private boolean instaRolled;
    /** Set when the scene could not be played, e.g. its world is unloaded. */
    private boolean broken;

    private WorldPos blockPos;

    public CinematicOpening(@NotNull CratesPlugin plugin,
                            @NotNull Player player,
                            @NotNull CrateSource source,
                            @Nullable Cost cost,
                            @NotNull CinematicScene scene) {
        super(plugin, player, source, cost);
        this.scene = scene;
        this.reward = source.getCrate().rollReward(player);
        this.playback = new ScenePlayback(plugin, player, scene, this.crate.getItemStack(), this.reward);
    }

    @Override
    public long getInterval() {
        return 1L;
    }

    @Override
    protected void onStart() {
        // Intentionally empty - see the class comment on lazy start.
    }

    @Override
    protected void onTick() {
        if (this.broken) return;

        if (!this.started) {
            this.started = true;
            this.beginPlayback();
            return;
        }

        this.playback.tick();
    }

    /**
     * Hides the crate's hologram, commits the player to the opening, and rolls the camera.
     *
     * <p>If the scene cannot run the opening is marked broken and completes normally, so the player
     * still receives the reward they paid for instead of being charged for nothing.
     */
    private void beginPlayback() {
        Block block = this.source.getBlock();
        if (block != null) {
            this.blockPos = WorldPos.from(block);
            this.hideHologram(this.blockPos);
        }

        // Past this point the crate has visibly been opened; refunding would duplicate the reward.
        this.setRefundable(false);

        if (!this.playback.start()) {
            this.broken = true;
            this.plugin.warn("Cinematic scene '" + this.scene.getId() + "' could not be played"
                + " (no frames, or its world is not loaded). Granting the reward without animation.");
        }
    }

    @Override
    public void instaRoll() {
        this.setRefundable(false);
        this.instaRolled = true;
        this.playback.stop(); // No-op when playback never started, e.g. during mass opening.
        this.stop();
    }

    @Override
    public boolean isCompleted() {
        return this.instaRolled || this.broken || this.playback.isFinished();
    }

    @Override
    protected void onComplete() {
        // Reward delivery, stats and post-open commands are handled by AbstractOpening#onStop.
    }

    @Override
    protected void onStop() {
        this.addReward(this.reward);
        this.playback.stop();

        if (this.blockPos != null) {
            this.showHologram(this.blockPos);
        }

        super.onStop();
    }

    @NotNull
    public CinematicScene getScene() {
        return this.scene;
    }
}
