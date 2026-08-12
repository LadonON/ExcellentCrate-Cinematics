package su.nightexpress.excellentcrates.hooks.impl;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.hooks.HookId;
import su.nightexpress.nightcore.util.Plugins;

import java.util.List;

/**
 * Soft-dependency entry point for <a href="https://nexomc.com">Nexo</a> custom blocks and furniture.
 *
 * <p>This class deliberately contains <b>no</b> references to Nexo types. Every call is forwarded to
 * {@link NexoBridge}, which is only ever class-loaded once {@link #isInstalled()} has returned
 * {@code true}. Servers without Nexo therefore never attempt to resolve Nexo classes, mirroring how
 * the hologram handlers stay optional with respect to ProtocolLib / PacketEvents.
 *
 * <p>A "model id" here means either a Nexo custom block id or a Nexo furniture id — the cinematic
 * prop and the crate's linked world block both accept either kind.
 */
public class NexoHook {

    /**
     * Cached because {@link Plugins#isInstalled(String)} is queried on hot paths (menu rendering,
     * per-tick prop updates) and plugins cannot be installed while the server is running.
     */
    private static Boolean installed;

    /**
     * @return {@code true} if the Nexo plugin is present on this server.
     */
    public static boolean isInstalled() {
        if (installed == null) {
            installed = Plugins.isInstalled(HookId.NEXO);
        }
        return installed;
    }

    /**
     * Clears the cached detection result. Called on plugin shutdown so a reload re-detects Nexo.
     */
    public static void clear() {
        installed = null;
    }

    /**
     * @return every Nexo custom block and furniture id known to the server, or an empty list if
     * Nexo is not installed.
     */
    @NotNull
    public static List<String> getModelIds() {
        return isInstalled() ? NexoBridge.modelIds() : List.of();
    }

    /**
     * @return {@code true} if {@code modelId} resolves to a Nexo custom block or furniture.
     */
    public static boolean isModel(@Nullable String modelId) {
        if (modelId == null || modelId.isBlank() || !isInstalled()) return false;

        return NexoBridge.isBlock(modelId) || NexoBridge.isFurniture(modelId);
    }

    /**
     * @return {@code true} if {@code modelId} is furniture (entity-backed) rather than a placed block.
     */
    public static boolean isFurniture(@Nullable String modelId) {
        if (modelId == null || modelId.isBlank() || !isInstalled()) return false;

        return NexoBridge.isFurniture(modelId);
    }

    /**
     * Reads the Nexo id off an item the admin is holding. This is how the editor resolves a model
     * without ever asking anyone to type an id by hand.
     *
     * @return the Nexo id of the item, or {@code null} if it is not a Nexo item.
     */
    @Nullable
    public static String getIdFromItem(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !isInstalled()) return null;

        return NexoBridge.idFromItem(itemStack);
    }

    /**
     * Spawns {@code modelId} as furniture at {@code location}.
     *
     * <p>Nexo furniture is backed by an {@link org.bukkit.entity.ItemDisplay}, which is the same
     * entity type the cinematic vanilla fallback uses. Returning the {@link Display} lets the frame
     * executor drive position/rotation/scale identically in both cases.
     *
     * @return the spawned display entity, or {@code null} if the id is not furniture or placement failed.
     */
    @Nullable
    public static Display spawnFurniture(@NotNull String modelId, @NotNull Location location) {
        if (!isInstalled() || !NexoBridge.isFurniture(modelId)) return null;

        return NexoBridge.placeFurniture(modelId, location);
    }

    /**
     * Places {@code modelId} as a custom block at {@code location}, replacing whatever is there.
     * Used by the "Place Nexo Block" action so a crate's linked block can be a Nexo block.
     *
     * @return {@code true} if a Nexo custom block now occupies the location.
     */
    public static boolean placeBlock(@NotNull String modelId, @NotNull Location location) {
        if (!isInstalled() || !NexoBridge.isBlock(modelId)) return false;

        return NexoBridge.placeBlock(modelId, location);
    }

    /**
     * Removes a Nexo block or furniture at {@code location}, without dropping loot.
     */
    public static void remove(@NotNull Location location) {
        if (!isInstalled()) return;

        NexoBridge.remove(location);
    }
}
