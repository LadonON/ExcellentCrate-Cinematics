package su.nightexpress.excellentcrates.editor.cinematic;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.Filled;
import su.nightexpress.nightcore.ui.menu.data.MenuFiller;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.excellentcrates.Placeholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * The scene timeline: an ordered list of frames.
 *
 * <p>Ordering is the one genuinely new interaction in the editor, since nothing else in the plugin
 * is order-sensitive. Vanilla inventories have no drag-and-drop, so reordering reuses click types
 * the menu code already understands rather than inventing a gesture: left-click edits, shift-click
 * moves a frame earlier, shift-right-click moves it later, and drop deletes — the same drop-key
 * convention used for crates, rewards and keys.
 */
public class CinematicFramesMenu extends LinkedMenu<CratesPlugin, CinematicProvider> implements Filled<FrameRef>, LangContainer {

    private static final IconLocale LOCALE_FRAME = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Entry")
        .rawName(GENERIC_NAME)
        .appendCurrent("Step", GENERIC_CURRENT)
        .appendCurrent("Type", GENERIC_TYPE)
        .appendCurrent("Duration", GENERIC_TIME).br()
        .appendClick("Click to edit")
        .rawLore(
            DARK_GRAY.wrap("Shift-click to move " + GOLD.wrap("up") + "."),
            DARK_GRAY.wrap("Shift-right-click to move " + GOLD.wrap("down") + "."),
            DARK_GRAY.wrap("Press " + GOLD.wrap("[" + TagWrappers.KEY.apply("key.drop") + "]") + " to delete.")
        )
        .build();

    private static final IconLocale LOCALE_ADD = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Add")
        .accentColor(GREEN)
        .name("Add Frame")
        .appendInfo("Appends a new frame to", "the end of the timeline.").br()
        .appendClick("Click to pick a type")
        .build();

    private static final IconLocale LOCALE_TIMELINE = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Info")
        .name("Timeline")
        .appendCurrent("Frames", GENERIC_AMOUNT)
        .appendCurrent("Total Duration", GENERIC_TIME).br()
        .appendInfo("Frames run top to bottom.").br()
        .appendInfo("Use " + SOFT_YELLOW.wrap("Wait") + " frames to leave", "gaps between the others.")
        .build();

    public CinematicFramesMenu(@NotNull CratesPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X6, Lang.EDITOR_TITLE_CINEMATIC_FRAMES.text());
        this.plugin.injectLang(this);

        this.addItem(MenuItem.buildReturn(this, 49, (viewer, event) -> {
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicOptions(viewer.getPlayer(), this.getLink(viewer)));
        }));
        this.addItem(MenuItem.buildNextPage(this, 53));
        this.addItem(MenuItem.buildPreviousPage(this, 45));
        this.addItem(MenuItem.background(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(45, 54).toArray()));

        this.addItem(Material.ANVIL, LOCALE_ADD, 47, (viewer, event, provider) -> {
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrameTypes(viewer.getPlayer(), provider));
        });
    }

    @Override
    @NotNull
    public MenuFiller<FrameRef> createFiller(@NotNull MenuViewer viewer) {
        var autoFill = MenuFiller.builder(this);
        CinematicProvider provider = this.getLink(viewer);
        CinematicScene scene = provider.getScene();

        autoFill.setSlots(IntStream.range(0, 45).toArray());
        autoFill.setItems(this.buildRefs(provider));
        autoFill.setItemCreator(ref -> {
            CinematicFrame frame = ref.frame();
            if (frame == null) return NightItem.fromType(Material.BARRIER);

            return NightItem.fromType(frame.getType().getIcon())
                .localized(LOCALE_FRAME)
                .replacement(replacer -> replacer
                    .replace(GENERIC_NAME, frame::getSummary)
                    .replace(GENERIC_CURRENT, () -> (ref.index() + 1) + " / " + scene.countFrames())
                    .replace(GENERIC_TYPE, () -> frame.getType().getName())
                    .replace(GENERIC_TIME, () -> CinematicFrame.formatTicks(frame.getDuration()))
                );
        });
        autoFill.setItemClick(ref -> (viewer1, event) -> {
            Player player = viewer1.getPlayer();
            ClickType click = event.getClick();

            if (click == ClickType.DROP) {
                scene.removeFrame(ref.index());
                provider.save();
                this.runNextTick(() -> this.flush(player));
                return;
            }

            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                // Shift-left moves the frame one step earlier, shift-right one step later.
                if (scene.moveFrame(ref.index(), click == ClickType.SHIFT_LEFT ? -1 : 1)) {
                    provider.save();
                }
                this.runNextTick(() -> this.flush(player));
                return;
            }

            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrame(player, provider, ref.index()));
        });

        return autoFill.build();
    }

    /**
     * @return one reference per frame, in timeline order.
     */
    @NotNull
    private List<FrameRef> buildRefs(@NotNull CinematicProvider provider) {
        int count = provider.getScene().countFrames();

        List<FrameRef> refs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            refs.add(new FrameRef(provider, index));
        }
        return refs;
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        CinematicScene scene = this.getLink(viewer).getScene();

        viewer.addItem(NightItem.fromType(Material.CLOCK)
            .localized(LOCALE_TIMELINE)
            .replacement(replacer -> replacer
                .replace(GENERIC_AMOUNT, () -> String.valueOf(scene.countFrames()))
                .replace(GENERIC_TIME, () -> CinematicFrame.formatTicks(scene.getTotalDuration()))
            )
            .toMenuItem().setSlots(51).build()
        );

        this.autoFill(viewer);
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }
}
