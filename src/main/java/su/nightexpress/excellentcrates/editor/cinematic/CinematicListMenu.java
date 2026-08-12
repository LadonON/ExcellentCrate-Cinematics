package su.nightexpress.excellentcrates.editor.cinematic;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.cinematic.CinematicManager;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.dialog.DialogRegistry;
import su.nightexpress.excellentcrates.dialog.cinematic.CinematicDialogs;
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

import java.util.stream.IntStream;

import static su.nightexpress.excellentcrates.Placeholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GREEN;

/**
 * Lists every cinematic scene, one icon each — the same shape as {@code CrateListMenu} and
 * {@code KeyListMenu}, including the shared border, the creation icon in the usual slot, and the
 * drop-key deletion convention.
 */
public class CinematicListMenu extends LinkedMenu<CratesPlugin, CinematicManager> implements Filled<CinematicProvider>, LangContainer {

    private static final IconLocale LOCALE_CREATION = LangEntry.iconBuilder("Editor.Button.Cinematic.Create")
        .accentColor(GREEN)
        .name("New Scene")
        .appendInfo("Use this button to create", "brand new cinematic scenes!").br()
        .appendClick("Click to create")
        .build();

    private static final IconLocale LOCALE_SCENE = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene")
        .rawName(GENERIC_NAME)
        .appendCurrent("Status", GENERIC_INSPECTION)
        .appendCurrent("ID", GENERIC_ID)
        .appendCurrent("Frames", GENERIC_AMOUNT)
        .appendCurrent("Duration", GENERIC_TIME).br()
        .rawLore(TagWrappers.DARK_GRAY.wrap("Press " + TagWrappers.GOLD.wrap("[" + TagWrappers.KEY.apply("key.drop") + "]") + " to delete.")).br()
        .appendClick("Click to edit")
        .build();

    private final DialogRegistry dialogs;

    public CinematicListMenu(@NotNull CratesPlugin plugin, @NotNull DialogRegistry dialogs) {
        super(plugin, MenuType.GENERIC_9X5, Lang.EDITOR_TITLE_CINEMATIC_LIST.text());
        this.dialogs = dialogs;
        this.plugin.injectLang(this);

        this.addItem(MenuItem.buildReturn(this, 40, (viewer, event) -> {
            this.runNextTick(() -> this.plugin.getEditorManager().openEditor(viewer.getPlayer()));
        }));
        this.addItem(MenuItem.buildNextPage(this, 44));
        this.addItem(MenuItem.buildPreviousPage(this, 36));
        this.addItem(MenuItem.background(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray()));
        this.addItem(MenuItem.background(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 36).toArray()));

        this.addItem(Material.ANVIL, LOCALE_CREATION, 42, (viewer, event, manager) -> {
            Player player = viewer.getPlayer();
            this.dialogs.show(player, CinematicDialogs.SCENE_CREATION, manager, () -> this.flush(player));
        });
    }

    @Override
    @NotNull
    public MenuFiller<CinematicProvider> createFiller(@NotNull MenuViewer viewer) {
        var autoFill = MenuFiller.builder(this);
        CinematicManager manager = this.getLink(viewer);

        autoFill.setSlots(IntStream.range(0, 36).toArray());
        autoFill.setItems(manager.getScenes());
        autoFill.setItemCreator(provider -> {
            CinematicScene scene = provider.getScene();

            return NightItem.fromType(Material.ITEM_FRAME)
                .localized(LOCALE_SCENE)
                .replacement(replacer -> replacer
                    .replace(GENERIC_INSPECTION, () -> Lang.inspection(Lang.INSPECTIONS_GENERIC_OVERVIEW, scene.isPlayable()))
                    .replace(GENERIC_NAME, scene::getName)
                    .replace(GENERIC_ID, scene::getId)
                    .replace(GENERIC_AMOUNT, () -> String.valueOf(scene.countFrames()))
                    .replace(GENERIC_TIME, () -> CinematicFrame.formatTicks(scene.getTotalDuration()))
                );
        });
        autoFill.setItemClick(provider -> (viewer1, event) -> {
            Player player = viewer1.getPlayer();

            if (event.getClick() == ClickType.DROP) {
                manager.deleteScene(provider);
                this.runNextTick(() -> this.flush(player));
                return;
            }

            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicOptions(player, provider));
        });

        return autoFill.build();
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        this.autoFill(viewer);
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }
}
