package su.nightexpress.excellentcrates.editor.cinematic;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.opening.cinematic.scene.FrameType;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import static su.nightexpress.excellentcrates.Placeholders.GENERIC_TYPE;

/**
 * Icon-grid picker for the frame kind to append — the same shape as the crate's opening-type
 * picker, one icon per {@link FrameType}.
 */
public class CinematicFrameTypeMenu extends LinkedMenu<CratesPlugin, CinematicProvider> implements LangContainer {

    /** Centre row of a 9x3 chest, which comfortably holds the eight frame kinds. */
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19};

    private static final IconLocale LOCALE_TYPE = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Type")
        .rawName(GENERIC_TYPE)
        .appendInfo("Adds a frame of this kind to", "the end of the timeline.").br()
        .appendClick("Click to add")
        .build();

    public CinematicFrameTypeMenu(@NotNull CratesPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X3, Lang.EDITOR_TITLE_CINEMATIC_ADD.text());
        this.plugin.injectLang(this);

        this.addItem(MenuItem.buildReturn(this, 22, (viewer, event) -> {
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrames(viewer.getPlayer(), this.getLink(viewer)));
        }));

        this.addItem(MenuItem.background(Material.BLACK_STAINED_GLASS_PANE, 0, 1, 7, 8, 9, 17, 18, 25, 26));
        this.addItem(MenuItem.background(Material.GRAY_STAINED_GLASS_PANE, 2, 3, 4, 5, 6, 20, 21, 23, 24));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        CinematicProvider provider = this.getLink(player);
        CinematicScene scene = provider.getScene();

        FrameType[] types = FrameType.values();
        for (int index = 0; index < types.length && index < SLOTS.length; index++) {
            FrameType type = types[index];

            viewer.addItem(NightItem.fromType(type.getIcon())
                .localized(LOCALE_TYPE)
                .replacement(replacer -> replacer.replace(GENERIC_TYPE, type::getName))
                .toMenuItem().setSlots(SLOTS[index]).setHandler((viewer1, event) -> {
                    scene.addFrame(type.create());
                    provider.save();

                    // Drop the admin straight into the new frame's settings - it is almost always
                    // the next thing they want, and a fresh frame has placeholder values.
                    int newIndex = scene.countFrames() - 1;
                    this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrame(player, provider, newIndex));
                }).build()
            );
        }
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }
}
