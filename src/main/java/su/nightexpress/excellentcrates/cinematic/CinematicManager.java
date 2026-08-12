package su.nightexpress.excellentcrates.cinematic;

import org.bukkit.Location;
import org.bukkit.Material;
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
import su.nightexpress.excellentcrates.dialog.DialogRegistry;
import su.nightexpress.excellentcrates.dialog.cinematic.*;
import su.nightexpress.excellentcrates.hooks.impl.NexoHook;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.ScenePlayback;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.MoveCameraFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.TeleportCameraFrame;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;
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
 * Owns everything about cinematic scenes that is not the playback itself: creating and deleting
 * scene files, handing out and consuming the capture tool, and running editor previews.
 *
 * <p>Scenes are opening providers, so the plugin-wide registry in {@code OpeningManager} remains the
 * single source of truth — this manager only reaches into it rather than keeping a parallel list.
 */
public class CinematicManager extends AbstractManager<CratesPlugin> {

    /** Item shown by the prop during an editor preview, where there is no crate to borrow from. */
    private static final Material PREVIEW_PROP_MATERIAL = Material.CHEST;

    /** Sentinel meaning "this capture tool is not aimed at a particular frame". */
    private static final int NO_FRAME = -1;

    private final DialogRegistry           dialogs;
    private final Map<UUID, ScenePlayback> previews;

    public CinematicManager(@NotNull CratesPlugin plugin, @NotNull DialogRegistry dialogs) {
        super(plugin);
        this.dialogs = dialogs;
        this.previews = new HashMap<>();
    }

    @Override
    protected void onLoad() {
        this.loadDialogs();

        this.addListener(new CinematicListener(this.plugin, this));

        // Previews run outside the opening system, so they need their own per-tick pump.
        this.addTask(this::tickPreviews, 1L);
    }

    @Override
    protected void onShutdown() {
        this.previews.values().forEach(ScenePlayback::stop);
        this.previews.clear();

        NexoHook.clear();
    }

    private void loadDialogs() {
        this.dialogs.register(CinematicDialogs.SCENE_CREATION, SceneCreationDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_NAME, SceneNameDialog::new);
        this.dialogs.register(CinematicDialogs.SCENE_REWARD_OFFSET, SceneRewardOffsetDialog::new);
        this.dialogs.register(CinematicDialogs.FRAME_DURATION, FrameDurationDialog::new);
        this.dialogs.register(CinematicDialogs.FRAME_SOUND, FrameSoundDialog::new);
        this.dialogs.register(CinematicDialogs.FRAME_TITLE, FrameTitleDialog::new);
        this.dialogs.register(CinematicDialogs.FRAME_PARTICLE, FrameParticleDialog::new);
        this.dialogs.register(CinematicDialogs.FRAME_TRANSFORM, FrameTransformDialog::new);
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
     * Gives the admin a capture tool aimed at a scene-level position.
     */
    public void giveCaptureTool(@NotNull Player player, @NotNull CinematicProvider provider, @NotNull CaptureTarget target) {
        this.giveCaptureTool(player, provider, target, NO_FRAME);
    }

    /**
     * Gives the admin a capture tool. The scene, target kind and frame index are stamped onto the
     * item, so a single tool implementation serves the viewer position, the prop position and every
     * camera keyframe — one implementation reused three ways, as the design brief asks.
     */
    public void giveCaptureTool(@NotNull Player player, @NotNull CinematicProvider provider, @NotNull CaptureTarget target, int frameIndex) {
        ItemStack itemStack = Config.CINEMATIC_CAPTURE_TOOL.get().getItemStack();

        PDCUtil.set(itemStack, Keys.captureToolSceneId, provider.getId());
        PDCUtil.set(itemStack, Keys.captureToolTarget, target.name());
        PDCUtil.set(itemStack, Keys.captureToolFrameIndex, frameIndex);

        Players.addItem(player, itemStack);

        Lang.CINEMATIC_CAPTURE_TOOL_GIVEN.message().send(player, replacer -> replacer
            .replace(Placeholders.GENERIC_TYPE, target.getName())
            .replace(Placeholders.GENERIC_NAME, provider.getScene().getName())
        );
    }

    /**
     * Consumes a capture tool right-click, writing the admin's current position and facing into the
     * scene.
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

        CaptureTarget target = CaptureTarget.byName(PDCUtil.getString(itemStack, Keys.captureToolTarget).orElse(""));
        int frameIndex = PDCUtil.getInt(itemStack, Keys.captureToolFrameIndex).orElse(NO_FRAME);

        // The admin's own location already carries the yaw and pitch a camera needs.
        Location location = player.getLocation();
        this.applyCapture(provider.getScene(), target, frameIndex, WorldPoint.from(location));
        provider.save();

        Lang.CINEMATIC_CAPTURE_TOOL_DONE.message().send(player, replacer -> replacer
            .replace(Placeholders.GENERIC_TYPE, target.getName())
            .replace(Placeholders.GENERIC_NAME, provider.getScene().getName())
        );

        this.reopenAfterCapture(player, provider, target, frameIndex);
        return true;
    }

    private void applyCapture(@NotNull CinematicScene scene, @NotNull CaptureTarget target, int frameIndex, @NotNull WorldPoint point) {
        switch (target) {
            case SCENE_PLAYER -> scene.setPlayerPoint(point);
            case SCENE_PROP -> scene.setPropPoint(point);
            case FRAME_CAMERA -> {
                CinematicFrame frame = scene.getFrame(frameIndex);
                if (frame instanceof MoveCameraFrame move) {
                    move.setTarget(point);
                }
                else if (frame instanceof TeleportCameraFrame teleport) {
                    teleport.setTarget(point);
                }
            }
        }
    }

    /**
     * Returns the admin to the screen they left, so capturing a position does not cost them their
     * place in the editor.
     */
    private void reopenAfterCapture(@NotNull Player player, @NotNull CinematicProvider provider, @NotNull CaptureTarget target, int frameIndex) {
        this.plugin.runTask(task -> {
            if (target == CaptureTarget.FRAME_CAMERA) {
                this.plugin.getEditorManager().openCinematicFrame(player, provider, frameIndex);
            }
            else {
                this.plugin.getEditorManager().openCinematicOptions(player, provider);
            }
        });
    }

    // ------------------------------------------------------------------
    // Editor preview
    // ------------------------------------------------------------------

    /**
     * Plays a scene for an admin without consuming a key or rolling a reward.
     *
     * <p>Uses the same {@link ScenePlayback} the real opening does, so what a builder sees while
     * iterating is exactly what a player will see.
     *
     * @return {@code false} if the scene is not playable yet.
     */
    public boolean startPreview(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();
        if (!scene.isPlayable()) return false;

        this.stopPreview(player);

        ScenePlayback playback = new ScenePlayback(this.plugin, player, scene, new ItemStack(PREVIEW_PROP_MATERIAL), null);
        if (!playback.start()) return false;

        this.previews.put(player.getUniqueId(), playback);
        return true;
    }

    public void stopPreview(@NotNull Player player) {
        ScenePlayback playback = this.previews.remove(player.getUniqueId());
        if (playback != null) playback.stop();
    }

    public boolean isPreviewing(@NotNull Player player) {
        return this.previews.containsKey(player.getUniqueId());
    }

    private void tickPreviews() {
        if (this.previews.isEmpty()) return;

        Iterator<Map.Entry<UUID, ScenePlayback>> iterator = this.previews.entrySet().iterator();
        while (iterator.hasNext()) {
            ScenePlayback playback = iterator.next().getValue();

            playback.tick();

            if (playback.isFinished()) {
                playback.stop();
                iterator.remove();
            }
        }
    }
}
