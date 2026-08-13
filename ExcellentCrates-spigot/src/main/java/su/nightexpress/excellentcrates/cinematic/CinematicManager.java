package su.nightexpress.excellentcrates.cinematic;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.Placeholders;
import su.nightexpress.excellentcrates.api.opening.OpeningProvider;
import su.nightexpress.excellentcrates.config.Config;
import su.nightexpress.excellentcrates.config.Keys;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.excellentcrates.dialog.DialogRegistry;
import su.nightexpress.excellentcrates.dialog.cinematic.CinematicDialogs;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneCameraHeightDialog;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneCreationDialog;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneEndDelayDialog;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneNameDialog;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneOpeningDelayDialog;
import su.nightexpress.excellentcrates.dialog.cinematic.SceneOpeningDialog;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;
import su.nightexpress.excellentcrates.util.pos.WorldPos;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Owns everything about cinematic scenes that is not the hand-off itself: creating and deleting
 * scene files, handing out and consuming the capture tool that sets the stage location, and
 * finishing the round trip once a delegated opening completes.
 *
 * <p>Scenes are opening providers, so the plugin-wide registry in {@code OpeningManager} remains the
 * single source of truth — this manager only reaches into it rather than keeping a parallel list.
 */
public class CinematicManager extends AbstractManager<CratesPlugin> {

    private final DialogRegistry             dialogs;
    private final Map<UUID, PendingReturn>   pendingReturns;

    public CinematicManager(@NotNull CratesPlugin plugin, @NotNull DialogRegistry dialogs) {
        super(plugin);
        this.dialogs = dialogs;
        this.pendingReturns = new HashMap<>();
    }

    @Override
    protected void onLoad() {
        this.loadDialogs();

        this.addListener(new CinematicListener(this.plugin, this));

        // Hand-offs are not driven by the delegate's own tick - see CinematicOpening's class comment
        // - so this manager needs its own per-tick pump to notice when a delegate finishes and bring
        // the player back.
        this.addTask(this::tickReturns, 1L);
    }

    @Override
    protected void onShutdown() {
        // Best-effort: a reload or disable must not strand anyone in spectator mode staring at a
        // marker that is about to vanish out from under them.
        for (Map.Entry<UUID, PendingReturn> entry : this.pendingReturns.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                this.finishReturn(player, entry.getValue());
            }
            else if (entry.getValue().cameraMarker.isValid()) {
                entry.getValue().cameraMarker.remove();
            }
        }
        this.pendingReturns.clear();
    }

    private void loadDialogs() {
        this.dialogs.register(CinematicDialogs.SCENE_CREATION, SceneCreationDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_NAME, SceneNameDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_OPENING, () -> new SceneOpeningDialog(this.plugin));
        this.dialogs.register(CinematicDialogs.SCENE_CAMERA_HEIGHT, SceneCameraHeightDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_OPENING_DELAY, SceneOpeningDelayDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_END_DELAY, SceneEndDelayDialog::new);
    }

    // ------------------------------------------------------------------
    // Scene registry
    // ------------------------------------------------------------------

    /**
     * @return every loaded cinematic scene, sorted by id so the editor list is stable between opens.
     */
    @NotNull
    public List<CinematicProvider> getScenes() {
        return this.plugin.getOpeningManager().getProviders().stream()
            .filter(CinematicProvider.class::isInstance)
            .map(CinematicProvider.class::cast)
            .sorted(Comparator.comparing(CinematicProvider::getId))
            .toList();
    }

    @Nullable
    public CinematicProvider getScene(@NotNull String id) {
        OpeningProvider provider = this.plugin.getOpeningManager().getProviderById(id);
        return provider instanceof CinematicProvider cinematic ? cinematic : null;
    }

    public int countScenes() {
        return this.getScenes().size();
    }

    /**
     * @return every opening id a scene may delegate to: every loaded provider except cinematic scenes
     * themselves, which would recurse.
     */
    @NotNull
    public List<String> getDelegatableOpeningIds() {
        return this.plugin.getOpeningManager().getProviderByIdMap().values().stream()
            .filter(provider -> !(provider instanceof CinematicProvider))
            .map(OpeningProvider::getId)
            .sorted(String::compareTo)
            .toList();
    }

    /**
     * An id may only be used once across <i>all</i> opening types, since crates reference openings
     * by a single flat id.
     */
    public boolean canCreateScene(@NotNull String id) {
        return this.plugin.getOpeningManager().getProviderById(id) == null;
    }

    /**
     * Creates an empty scene file under {@code openings/cinematic/} and registers it immediately, so
     * it shows up in both the scene list and the crate's opening picker without a reload.
     */
    public void createScene(@NotNull String id) {
        Path path = Path.of(this.plugin.getDataFolder() + Config.DIR_OPENINGS_CINEMATIC, FileConfig.withExtension(id));
        FileUtil.createFileIfNotExists(path);

        this.plugin.getOpeningManager().loadProvider(path.toFile(), CinematicProvider::new);

        CinematicProvider provider = this.getScene(id);
        if (provider == null) return;

        provider.getScene().setName(StringUtil.capitalizeUnderscored(id));
        provider.save();
    }

    /**
     * Deletes a scene's file and unregisters it. Crates still pointing at the id fall back to no
     * animation rather than erroring.
     *
     * @return {@code true} if the file was removed.
     */
    public boolean deleteScene(@NotNull CinematicProvider provider) {
        File file = provider.getFile();

        try {
            if (file != null && !Files.deleteIfExists(file.toPath())) return false;
        }
        catch (IOException exception) {
            this.plugin.error("Could not delete cinematic scene '" + provider.getId() + "'.");
            exception.printStackTrace();
            return false;
        }

        this.plugin.getOpeningManager().removeProvider(provider.getId());
        return true;
    }

    // ------------------------------------------------------------------
    // Capture tool
    // ------------------------------------------------------------------

    /**
     * Gives the admin a capture tool that writes the current position and facing into the scene's
     * stage location on right-click, and — if the right-click lands on a block rather than air —
     * also marks that block as the scene's crate block, so delegates that render on top of a block
     * (Simple Roll, most notably) have one to render on top of.
     */
    public void giveCaptureTool(@NotNull Player player, @NotNull CinematicProvider provider) {
        ItemStack itemStack = Config.CINEMATIC_CAPTURE_TOOL.get().getItemStack();

        PDCUtil.set(itemStack, Keys.captureToolSceneId, provider.getId());

        Players.addItem(player, itemStack);

        Lang.CINEMATIC_CAPTURE_TOOL_GIVEN.message().send(player, replacer -> replacer
            .replace(Placeholders.GENERIC_NAME, provider.getScene().getName())
        );
    }

    /**
     * Consumes a capture tool right-click, writing the admin's current position and facing into the
     * scene's stage — and, if they right-clicked an actual block rather than air, that block into the
     * scene's crate block too.
     *
     * <p>An air click never clears a crate block set by an earlier capture: adjusting exactly where
     * you stand afterwards shouldn't undo which block the reveal is anchored to.
     *
     * @return {@code true} if the item was a capture tool and the interaction was handled.
     */
    public boolean handleCaptureInteraction(@NotNull Player player, @NotNull ItemStack itemStack, @NotNull PlayerInteractEvent event) {
        String sceneId = PDCUtil.getString(itemStack, Keys.captureToolSceneId).orElse(null);
        if (sceneId == null) return false;

        // Claim the interaction even if the scene has since been deleted, otherwise the tool would
        // fall through to normal block placement.
        itemStack.setAmount(0);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        CinematicProvider provider = this.getScene(sceneId);
        if (provider == null) return true;

        CinematicScene scene = provider.getScene();

        // The admin's own location already carries the yaw and pitch the stage teleport should use.
        scene.setStage(WorldPoint.from(player.getLocation()));

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            scene.setCrateBlock(WorldPos.from(clickedBlock));
        }

        provider.save();

        Lang.CINEMATIC_CAPTURE_TOOL_DONE.message().send(player, replacer -> replacer
            .replace(Placeholders.GENERIC_NAME, provider.getScene().getName())
        );

        this.plugin.runTask(task -> this.plugin.getEditorManager().openCinematicOptions(player, provider));
        return true;
    }

    // ------------------------------------------------------------------
    // Hand-off round trip
    // ------------------------------------------------------------------

    /**
     * Registers a player as waiting for their delegated opening to finish, so they can be teleported
     * back once it does.
     *
     * <p>Called by {@link su.nightexpress.excellentcrates.opening.cinematic.CinematicOpening} right
     * after it swaps the delegate into place. From this point that opening is dangling — it is no
     * longer registered anywhere and will never tick again — so this manager, not that object, is
     * what finishes the round trip.
     *
     * @param blockPos     the base crate's block, if the player clicked one, so its hologram can be
     *                     restored; {@code null} if there was nothing to hide in the first place.
     * @param cameraMarker the stationary marker locking the player's camera, so it can be removed and
     *                     the player detached from it once the delegate finishes.
     * @param endDelayTicks how many ticks to hold the camera lock in place after the delegate opening
     *                      finishes, before actually teleporting the player back.
     */
    public void awaitReturn(@NotNull Player player, @NotNull Location returnLocation, @NotNull GameMode returnGameMode,
                            @NotNull Crate crate, @Nullable WorldPos blockPos, @NotNull ArmorStand cameraMarker,
                            int endDelayTicks) {
        this.pendingReturns.put(player.getUniqueId(), new PendingReturn(returnLocation.clone(), returnGameMode, crate, blockPos, cameraMarker, endDelayTicks));
    }

    /**
     * Restores a player who disconnects while their delegated opening is still running.
     *
     * <p>Neither the gamemode restore nor the marker removal can wait for the next tick's
     * {@link #tickReturns()} pass: once a player is offline, {@code setGameMode} can no longer reach
     * them, and a spectator-mode gamemode left stuck in their saved data would greet them with a
     * frozen camera on their very next login. This runs during the quit event itself, while they are
     * still briefly reachable.
     */
    public void forceReturn(@NotNull Player player) {
        PendingReturn pending = this.pendingReturns.remove(player.getUniqueId());
        if (pending == null) return;

        this.finishReturn(player, pending);
    }

    private void tickReturns() {
        if (this.pendingReturns.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingReturn>> iterator = this.pendingReturns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingReturn> entry = iterator.next();
            PendingReturn pending = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null) {
                // Normally forceReturn already handled this on quit and the entry would be gone
                // entirely; this is a defensive fallback so a marker can never linger regardless.
                if (pending.cameraMarker.isValid()) {
                    pending.cameraMarker.remove();
                }
                iterator.remove();
                continue;
            }

            if (pending.returnCountdown < 0) {
                // Still running - the delegate hasn't finished (or been cancelled) yet.
                if (this.plugin.getOpeningManager().isOpening(player)) continue;

                // The delegate just finished. Start (or skip, if unconfigured) the scene's end delay
                // before actually stepping the player back out.
                pending.returnCountdown = pending.endDelayTicks;
            }

            if (pending.returnCountdown > 0) {
                pending.returnCountdown--;
                continue;
            }

            this.finishReturn(player, pending);
            iterator.remove();
        }
    }

    /**
     * Detaches and removes the camera marker, restores the player's gamemode and position, and
     * un-hides the base crate's hologram if one was hidden.
     *
     * <p>The spectator target is cleared before the marker is removed, not after — detaching from a
     * marker that already stopped existing can leave the client visually stuck on the dead entity.
     */
    private void finishReturn(@NotNull Player player, @NotNull PendingReturn pending) {
        if (player.getSpectatorTarget() == pending.cameraMarker) {
            player.setSpectatorTarget(null);
        }
        if (pending.cameraMarker.isValid()) {
            pending.cameraMarker.remove();
        }

        player.setGameMode(pending.returnGameMode);
        player.teleport(pending.returnLocation);

        if (pending.blockPos != null) {
            this.plugin.getHologramManager().ifPresent(hologramManager -> hologramManager.enableBlockHologram(pending.crate, pending.blockPos));
        }
    }

    /**
     * What a hand-off needs remembered until the delegate finishes: where and in what gamemode to put
     * the player back, which hologram (if any) to restore, and which marker is locking their camera.
     *
     * <p>{@code returnCountdown} starts at {@code -1}, meaning the delegate is still running. Once
     * {@link #tickReturns()} first notices the delegate has finished, it is set to the scene's
     * {@code endDelayTicks} and counted down to zero before the return actually happens - so a scene
     * with no end delay configured returns the same tick the delegate finishes, exactly as before end
     * delays existed.
     */
    private static final class PendingReturn {
        private final Location   returnLocation;
        private final GameMode   returnGameMode;
        private final Crate      crate;
        private final WorldPos   blockPos;
        private final ArmorStand cameraMarker;
        private final int        endDelayTicks;

        private int returnCountdown = -1;

        private PendingReturn(@NotNull Location returnLocation, @NotNull GameMode returnGameMode,
                              @NotNull Crate crate, @Nullable WorldPos blockPos, @NotNull ArmorStand cameraMarker,
                              int endDelayTicks) {
            this.returnLocation = returnLocation;
            this.returnGameMode = returnGameMode;
            this.crate = crate;
            this.blockPos = blockPos;
            this.cameraMarker = cameraMarker;
            this.endDelayTicks = endDelayTicks;
        }
    }
}
