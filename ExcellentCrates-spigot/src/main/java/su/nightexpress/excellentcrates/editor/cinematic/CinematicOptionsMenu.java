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
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.excellentcrates.util.pos.WorldPos;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.util.stream.IntStream;

import static su.nightexpress.excellentcrates.Placeholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.DARK_GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GOLD;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.RED;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Per-scene settings — structurally a peer of {@code CrateOptionsMenu}.
 *
 * <p>The core settings: where the stage is, which existing opening animation runs there, how far
 * above the stage the locked camera sits, and — optionally — which block a block-anchored delegate
 * (Simple Roll, most notably) should render its reveal on top of. The stage and crate block both use
 * the capture-tool idiom, because typing coordinates and angles by hand is the worst part of setting
 * up a teleport destination.
 */
public class CinematicOptionsMenu extends LinkedMenu<CratesPlugin, CinematicProvider> implements LangContainer {

    private static final IconLocale LOCALE_NAME = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Name").name("Name")
        .appendCurrent("Current", GENERIC_NAME).br()
        .appendInfo("Sets scene display name.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_STAGE = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Stage").name("Stage Location")
        .appendCurrent("Set", GENERIC_STATE).br()
        .appendInfo("Where the player is teleported", "while the opening animation plays.").br()
        .appendInfo("Stand where you want the player,", "look the right way, and right-click", "with the tool.").br()
        .appendInfo("Right-click the crate's actual", "block instead of air to also set", SOFT_YELLOW.wrap("Crate Block") + " in the same click.").br()
        .appendClick("Click to get capture tool")
        .build();

    private static final IconLocale LOCALE_CRATE_BLOCK = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.CrateBlock").name("Crate Block")
        .appendCurrent("Set", GENERIC_STATE).br()
        .appendInfo("The block Simple Roll (and other", "block-anchored openings) render", "their reveal on top of.").br()
        .appendInfo("Set by right-clicking the block", "itself with the " + SOFT_YELLOW.wrap("Stage Location") + " tool", "instead of clicking air.").br()
        .rawLore(DARK_GRAY.wrap("Press " + GOLD.wrap("[" + TagWrappers.KEY.apply("key.drop") + "]") + " to unset."))
        .build();

    private static final IconLocale LOCALE_OPENING = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Opening").name("Opening Animation")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("Which opening animation actually", "runs once the player arrives -", SOFT_YELLOW.wrap("simple_roll") + ", " + SOFT_YELLOW.wrap("csgo") + ", or any other", "id under " + SOFT_YELLOW.wrap("openings/") + ".").br()
        .appendInfo("It rolls and grants the reward", "itself, exactly as it would for a", "crate configured with that id", "directly.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_CAMERA_HEIGHT = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.CameraHeight").name("Camera Height")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("How far above the stage, in blocks,", "the locked camera sits.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_MODEL = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.Model").name("Model")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("Which ModelEngine blueprint spawns", "on the crate block at the stage.").br()
        .appendInfo("Optional, and requires the", "ModelEngine plugin.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_MODEL_ANIMATION = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.ModelAnimation").name("Model Animation")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("Which animation plays on the model", "the instant it spawns.").br()
        .appendInfo("Whether it holds on its last frame", "afterwards is set on the animation", "itself in Blockbench.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_MODEL_YAW = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.ModelYaw").name("Model Orientation")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("Which way the model prop faces when", "it spawns, in degrees.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_START_DELAY = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.StartDelay").name("Start Delay")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("How many ticks after arrival the model", "prop spawns and plays its animation.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_OPENING_DELAY = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.OpeningDelay").name("Opening Delay")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("How many ticks after the model's", "animation triggers the delegate", "opening animation actually starts.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_END_DELAY = LangEntry.iconBuilder("Editor.Button.Cinematic.Scene.EndDelay").name("End Delay")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("How many ticks after the delegate", "opening finishes the player is", "teleported back.").br()
        .appendClick("Click to change")
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
            .toMenuItem().setSlots(11).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_NAME, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.ENDER_EYE)
            .localized(LOCALE_STAGE)
            .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_YES_NO.get(!scene.getStage().isEmpty())))
            .toMenuItem().setSlots(13).setHandler((viewer1, event) -> {
                manager.giveCaptureTool(player, provider);
                this.runNextTick(player::closeInventory);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.CHEST)
            .localized(LOCALE_OPENING)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () ->
                scene.hasOpeningId() ? scene.getOpeningId() : CoreLang.OTHER_NONE.text()))
            .toMenuItem().setSlots(15).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_OPENING, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.SPYGLASS)
            .localized(LOCALE_CAMERA_HEIGHT)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> String.valueOf(scene.getCameraHeight())))
            .toMenuItem().setSlots(22).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_CAMERA_HEIGHT, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.REPEATER)
            .localized(LOCALE_START_DELAY)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> String.valueOf(scene.getStartDelay())))
            .toMenuItem().setSlots(23).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_START_DELAY, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.COMPARATOR)
            .localized(LOCALE_OPENING_DELAY)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> String.valueOf(scene.getOpeningDelay())))
            .toMenuItem().setSlots(24).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_OPENING_DELAY, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.OBSERVER)
            .localized(LOCALE_END_DELAY)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> String.valueOf(scene.getEndDelay())))
            .toMenuItem().setSlots(25).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_END_DELAY, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.ARMOR_STAND)
            .localized(LOCALE_MODEL)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () ->
                scene.hasModel() ? scene.getModelId() : CoreLang.OTHER_NONE.text()))
            .toMenuItem().setSlots(29).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_MODEL, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.CLOCK)
            .localized(LOCALE_MODEL_ANIMATION)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, scene::getModelAnimation))
            .toMenuItem().setSlots(31).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_MODEL_ANIMATION, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(Material.COMPASS)
            .localized(LOCALE_MODEL_YAW)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> String.valueOf(scene.getModelYaw())))
            .toMenuItem().setSlots(33).setHandler((viewer1, event) -> {
                this.dialogs.show(player, CinematicDialogs.SCENE_MODEL_YAW, provider, flush);
            }).build()
        );

        viewer.addItem(NightItem.fromType(scene.hasCrateBlock() ? Material.CHEST_MINECART : Material.STRUCTURE_VOID)
            .localized(LOCALE_CRATE_BLOCK)
            .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_YES_NO.get(scene.hasCrateBlock())))
            .toMenuItem().setSlots(20).setHandler((viewer1, event) -> {
                if (event.getClick() != ClickType.DROP || !scene.hasCrateBlock()) return;

                scene.setCrateBlock(WorldPos.empty());
                provider.save();
                this.runNextTick(flush);
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

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }
}
