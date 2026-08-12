package su.nightexpress.excellentcrates.cinematic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.nightcore.manager.AbstractListener;

/**
 * Wires the capture tool and editor previews into player events.
 */
public class CinematicListener extends AbstractListener<CratesPlugin> {

    private final CinematicManager manager;

    public CinematicListener(@NotNull CratesPlugin plugin, @NotNull CinematicManager manager) {
        super(plugin);
        this.manager = manager;
    }

    /**
     * Runs before {@code CrateListener} so a capture tool right-clicked onto a crate block captures
     * a position instead of opening the crate.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCaptureToolUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Right-click only, and air counts: a camera position is usually mid-air, nowhere near a
        // block. Ignoring left-clicks also stops the tool being consumed by an accidental swing.
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        this.manager.handleCaptureInteraction(event.getPlayer(), item, event);
    }

    /**
     * A preview leaves the player in spectator mode at the scene, so it has to be torn down if they
     * disconnect mid-playback.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.manager.isPreviewing(player)) {
            this.manager.stopPreview(player);
        }
    }
}
