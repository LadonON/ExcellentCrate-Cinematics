package su.nightexpress.excellentcrates.hooks.impl;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The only class in the plugin that touches Nexo types.
 *
 * <p>Package-private and reached exclusively through {@link NexoHook}, which guards every entry
 * point with an installation check. Keeping the Nexo imports isolated here means the JVM never has
 * to resolve them on a server where Nexo is absent.
 */
final class NexoBridge {

    private NexoBridge() {}

    /**
     * @return all custom block ids followed by all furniture ids, sorted for a stable editor listing.
     */
    @NotNull
    static List<String> modelIds() {
        List<String> ids = new ArrayList<>();
        ids.addAll(Arrays.asList(NexoBlocks.blockIDs()));
        ids.addAll(Arrays.asList(NexoFurniture.furnitureIDs()));
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    static boolean isBlock(@NotNull String modelId) {
        return NexoBlocks.isCustomBlock(modelId);
    }

    static boolean isFurniture(@NotNull String modelId) {
        return NexoFurniture.isFurniture(modelId);
    }

    @Nullable
    static String idFromItem(@NotNull ItemStack itemStack) {
        return NexoItems.idFromItem(itemStack);
    }

    /**
     * Places furniture facing the direction encoded in the location's yaw.
     *
     * @return the backing display entity, or {@code null} if Nexo refused the placement.
     */
    @Nullable
    static Display placeFurniture(@NotNull String modelId, @NotNull Location location) {
        ItemDisplay display = NexoFurniture.place(modelId, location, location.getYaw(), BlockFace.UP);
        return display;
    }

    static boolean placeBlock(@NotNull String modelId, @NotNull Location location) {
        NexoBlocks.place(modelId, location);
        return NexoBlocks.isCustomBlock(location.getBlock());
    }

    /**
     * Removes furniture first, then a custom block, since a location can only hold one of the two.
     */
    static void remove(@NotNull Location location) {
        if (NexoFurniture.isFurniture(location)) {
            NexoFurniture.remove(location);
            return;
        }
        if (NexoBlocks.isCustomBlock(location.getBlock())) {
            NexoBlocks.remove(location);
        }
    }
}
