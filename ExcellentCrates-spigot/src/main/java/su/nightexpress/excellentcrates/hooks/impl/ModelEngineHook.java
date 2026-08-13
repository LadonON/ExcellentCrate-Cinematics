package su.nightexpress.excellentcrates.hooks.impl;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.hooks.HookId;
import su.nightexpress.nightcore.util.Plugins;

import java.util.List;

/**
 * Soft-dependency entry point for <a href="https://modelengine.info">ModelEngine</a> custom models.
 *
 * <p>This class deliberately contains <b>no</b> references to ModelEngine types. Every call is
 * forwarded to {@link ModelEngineBridge}, which is only ever class-loaded once {@link #isInstalled()}
 * has returned {@code true}, mirroring {@link NexoHook}.
 *
 * <p>Used by the cinematic hand-off to spawn the crate's model at the stage and play its opening
 * animation - see {@code CinematicOpening}.
 */
public class ModelEngineHook {

    /**
     * Cached because {@link Plugins#isInstalled(String)} is queried on a hot path (every cinematic
     * hand-off) and plugins cannot be installed while the server is running.
     */
    private static Boolean installed;

    /**
     * @return {@code true} if the ModelEngine plugin is present on this server.
     */
    public static boolean isInstalled() {
        if (installed == null) {
            installed = Plugins.isInstalled(HookId.MODEL_ENGINE);
        }
        return installed;
    }

    /**
     * Clears the cached detection result. Called on plugin shutdown so a reload re-detects ModelEngine.
     */
    public static void clear() {
        installed = null;
    }

    /**
     * @return every registered ModelEngine blueprint id, or an empty list if ModelEngine is not
     * installed. This is what the editor's model picker lists.
     */
    @NotNull
    public static List<String> getModelIds() {
        return isInstalled() ? ModelEngineBridge.modelIds() : List.of();
    }

    /**
     * @return {@code true} if {@code modelId} resolves to a registered ModelEngine blueprint.
     */
    public static boolean isModel(@Nullable String modelId) {
        if (modelId == null || modelId.isBlank() || !isInstalled()) return false;

        return ModelEngineBridge.hasBlueprint(modelId);
    }

    /**
     * Spawns {@code modelId} at {@code location}, facing {@code yaw} degrees, and plays
     * {@code animationId} on it.
     *
     * <p>Whether the model holds on the animation's last frame - or loops, or does anything else
     * once it finishes - is entirely up to that animation's own loop mode as authored in Blockbench;
     * this only starts it once, the same instant the model appears.
     *
     * @return the spawned prop, so the caller can {@link ModelEngineProp#remove()} it later, or
     * {@code null} if ModelEngine is not installed or the id does not resolve to a model.
     */
    @Nullable
    public static ModelEngineProp spawnProp(@NotNull String modelId, @NotNull String animationId, @NotNull Location location, double yaw) {
        if (!isInstalled()) return null;

        return ModelEngineBridge.spawn(modelId, animationId, location, yaw);
    }
}
