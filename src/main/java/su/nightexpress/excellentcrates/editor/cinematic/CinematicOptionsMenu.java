package su.nightexpress.excellentcrates.editor.cinematic;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.cinematic.CaptureTarget;
import su.nightexpress.excellentcrates.cinematic.CinematicManager;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.dialog.DialogRegistry;
import su.nightexpress.excellentcrates.dialog.cinematic.CinematicDialogs;
import su.nightexpress.excellentcrates.hooks.HookId;
import su.nightexpress.excellentcrates.hooks.impl.NexoHook;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.click.ClickResult;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.util.stream.IntStream;

import static su.nightexpress.excellentcrates.Placeholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Per-scene settings — structurally a peer of {@code CrateOptionsMenu}.
 *
 * <p>Positions use the capture-tool idiom rather than a dialog, because typing coordinates and
 * angles by hand is the worst part of building a camera move. Only the reward offset, which is three
 * small relative numbers, is typed.
 */
public class CinematicOptionsMenu extends LinkedMenu<CratesPlugin, CinematicProvider> implements LangContainer {

    private static final IconLocale LOCALE_NAME = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Name").name("Name")
        .appendCurrent("Current", GENERIC_NAME).br()
        .appendInfo("Sets scene display name.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_PLAYER_LOCATION = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.PlayerLocation").name("Viewer Position")
        .appendCurrent("Set", GENERIC_STATE).br()
        .appendInfo("Where the player is placed", "while the cinematic plays.").br()
        .appendInfo("Stand where you want them,", "look the right way, then", "right-click with the tool.").br()
        .appendClick("Click to get capture tool")
        .build();

    private static final IconLocale LOCALE_PROP_LOCATION = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.PropLocation").name("Prop Position")
        .appendCurrent("Set", GENERIC_STATE).br()
        .appendInfo("Where the crate prop is spawned.").br()
        .appendInfo("Defaults to the viewer position", "when left unset.").br()
        .appendClick("Click to get capture tool")
        .build();

    private static final IconLocale LOCALE_PROP_MODEL = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.PropModel").name("Prop Model")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("The Nexo block or furniture", "shown as the crate prop.").br()
        .appendInfo("Drop a " + SOFT_YELLOW.wrap("Nexo item") + " on " + SOFT_YELLOW.wrap("this"), "button to read its id.").br()
        .rawLore(DARK_GRAY.wrap("Press " + GOLD.wrap("[" + TagWrappers.KEY.apply("key.drop") + "]") + " to unset."))
        .build();

    private static final IconLocale LOCALE_PROP_MODEL_LOCKED = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.PropModelLocked")
        .accentColor(RED)
        .name("Prop Model")
        .appendCurrent("Status", RED.wrap("Unavailable")).br()
        .appendInfo("Requires " + SOFT_YELLOW.wrap(HookId.NEXO) + " to be installed.").br()
        .appendInfo("Without it, the prop falls back", "to a display entity showing", "the crate's item.")
        .build();

    private static final IconLocale LOCALE_REWARD_OFFSET = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.RewardOffset").name("Reward Offset")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("Where the reward is unveiled,", "relative to the prop.").br()
        .appendClick("Click to edit")
        .build();

    private static final IconLocale LOCALE_FRAMES = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Frames").name("Timeline")
        .appendCurrent("Frames", GENERIC_AMOUNT)
        .appendCurrent("Duration", GENERIC_TIME).br()
        .appendInfo("Add and reorder the frames", "that make up the scene!").br()
        .appendClick("Click to open")
        .build();

    private static final IconLocale LOCALE_PREVIEW = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Preview")
        .accentColor(GREEN)
        .name("Preview Scene")
        .appendInfo("Plays the scene on you right now.").br()
        .appendInfo("No key is spent and no reward", "is rolled - this is just the show.").br()
        .appendClick("Click to play")
        .build();

    private static final IconLocale LOCALE_DELETE = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Delete")
        .accentColor(RED)
        .name("Delete Scene")
        .appendInfo("Permanently deletes the scene.").br()
        .appendClick("Press [" + TagWrappers.KEY.apply("key.drop") + "] to delete")
        .build();

    private final DialogRegistry dialogs;

    public CinematicOptionsMenu(@NotNull CratesPlugin plugin, @NotNull DialogRegistry dialogs) {
        super(plugin, MenuType.GENERIC_9X5, Lang.EDITOR_TITLE_CINEMATIC_SETTINGS.text());
        this.dialogs = dialogs;
        this.plugin.injectLang(this);

        this.addItem(MenuItem.buildReturn(this, 40, (viewer, event) -> {
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicList(viewer.getPlayer()));
        }));

        this.addItem(MenuItem.background(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray()));
        this.addItem(MenuItem.background(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 36).toArray()));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        CinematicProvider provider = this.getLink(player);
        CinematicScene scene = provider.getScene();
        CinematicManager manager = this.plugin.getCinematicManager();
        Runnable flush = () -> this.flush(player);

        viewer.addItem(NightItem.fromType(Material.NAME_TAG)
            .localized(LOCALE_NAME)
            .replacement(replacer -> replacer.replace(GENERIC_NAME, scene::getName))
            .toMenuItem().setSlots(10).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_NAME, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.COMPASS)
            .localized(LOCALE_PLAYER_LOCATION)
            .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_YES_NO.get(!scene.getPlayerPoint().isEmpty())))
            .toMenuItem().setSlots(11).setHandler((viewer1, event) -> {
                manager.giveCaptureTool(player, provider, CaptureTarget.SCENE_PLAYER);
                this.runNextTick(player::closeInventory);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.ARMOR_STAND)
            .localized(LOCALE_PROP_LOCATION)
            .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_YES_NO.get(!scene.getPropPoint().isEmpty())))
            .toMenuItem().setSlots(12).setHandler((viewer1, event) -> {
                manager.giveCaptureTool(player, provider, CaptureTarget.SCENE_PROP);
                this.runNextTick(player::closeInventory);
            }).build()
        );

        this.addPropModelButton(viewer, player, provider, scene, flush);

        viewer.addItem(NightItem.fromType(Material.CHEST)
            .localized(LOCALE_REWARD_OFFSET)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () ->
                scene.getRewardOffsetX() + ", " + scene.getRewardOffsetY() + ", " + scene.getRewardOffsetZ()))
            .toMenuItem().setSlots(14).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_REWARD_OFFSET, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.WRITABLE_BOOK)
            .localized(LOCALE_FRAMES)
            .replacement(replacer -> replacer
                .replace(GENERIC_AMOUNT, () -> String.valueOf(scene.countFrames()))
                .replace(GENERIC_TIME, () -> CinematicFrame.formatTicks(scene.getTotalDuration()))
            )
            .toMenuItem().setSlots(15).setHandler((viewer1, event) -> {
                this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrames(player, provider));
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.ENDER_EYE)
            .localized(LOCALE_PREVIEW)
            .toMenuItem().setSlots(16).setHandler((viewer1, event) -> {
                // Close first: the scene puts the player in spectator mode, which an open menu hides.
                this.runNextTick(() -> {
                    player.closeInventory();
                    if (!manager.startPreview(player, provider)) {
                        Lang.CINEMATIC_PREVIEW_ERROR_UNPLAYABLE.message().send(player);
                    }
                });
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.BARRIER)
            .localized(LOCALE_DELETE)
            .toMenuItem().setSlots(44).setHandler((viewer1, event) -> {
                if (event.getClick() != ClickType.DROP) return;

                manager.deleteScene(provider);
                this.runNextTick(() -> this.plugin.getEditorManager().openCinematicList(player));
            }).build()
        );
    }

    /**
     * Renders the prop-model button.
     *
     * <p>Without Nexo the button is visibly disabled and says why, rather than silently doing
     * nothing when clicked.
     */
    private void addPropModelButton(@NotNull MenuViewer viewer, @NotNull Player player,
                                    @NotNull CinematicProvider provider, @NotNull CinematicScene scene, @NotNull Runnable flush) {
        if (!NexoHook.isInstalled()) {
            viewer.addItem(NightItem.fromType(Material.STRUCTURE_VOID)
                .localized(LOCALE_PROP_MODEL_LOCKED)
                .toMenuItem().setSlots(13).build()
            );
            return;
        }

        viewer.addItem(NightItem.fromType(Material.ITEM_FRAME)
            .localized(LOCALE_PROP_MODEL)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () ->
                scene.hasPropModel() ? scene.getPropModel() : CoreLang.OTHER_NONE.text()))
            .toMenuItem().setSlots(13).setHandler((viewer1, event) -> {
                ItemStack cursor = event.getCursor();

                if (cursor == null || cursor.getType().isAir()) {
                    if (event.getClick() == ClickType.DROP) {
                        scene.setPropModel("");
                        provider.save();
                        this.runNextTick(flush);
                    }
                    return;
                }

                // Read the Nexo id straight off the held item, so no id ever has to be typed.
                String modelId = NexoHook.getIdFromItem(cursor);
                if (modelId != null) {
                    scene.setPropModel(modelId);
                    provider.save();
                }

                Players.addItem(player, cursor);
                event.getView().setCursor(null);
                this.runNextTick(flush);
            }).build()
        );
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    /**
     * Allows the admin to pick an item up from their own inventory, which is what makes dropping a
     * Nexo item onto the model button possible. Mirrors {@code CrateOptionsMenu}.
     */
    @Override
    public void onClick(@NotNull MenuViewer viewer, @NotNull ClickResult result, @NotNull InventoryClickEvent event) {
        super.onClick(viewer, result, event);
        if (result.isInventory() && !event.isShiftClick()) {
            event.setCancelled(false);
        }
    }
}
