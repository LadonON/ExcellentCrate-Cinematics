package su.nightexpress.excellentcrates.editor.cinematic;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.cinematic.CaptureTarget;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.config.Lang;
import su.nightexpress.excellentcrates.dialog.DialogKey;
import su.nightexpress.excellentcrates.dialog.DialogRegistry;
import su.nightexpress.excellentcrates.dialog.cinematic.CinematicDialogs;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.Easing;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.*;
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
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.RED;

/**
 * Settings for a single frame.
 *
 * <p>Which buttons appear depends on the frame's type, following the split the rest of the editor
 * uses: picks and toggles (easing, firework) are handled by clicking the icon, freeform numbers and
 * text open a dialog, and locations use the capture tool.
 */
public class CinematicFrameMenu extends LinkedMenu<CratesPlugin, FrameRef> implements LangContainer {

    /** Slot holding whatever options are specific to the frame's type. */
    private static final int SLOT_TYPE_OPTIONS = 13;

    private static final IconLocale LOCALE_DURATION = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Duration").name("Duration")
        .appendCurrent("Current", GENERIC_TIME).br()
        .appendInfo("How long this frame lasts.").br()
        .appendInfo("A duration of " + TagWrappers.SOFT_YELLOW.wrap("0") + " runs the", "next frame in the same tick.").br()
        .appendClick("Click to change")
        .build();

    private static final IconLocale LOCALE_EASING = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Easing").name("Easing")
        .appendCurrent("Current", GENERIC_VALUE).br()
        .appendInfo("How the movement accelerates.").br()
        .appendInfo(TagWrappers.SOFT_YELLOW.wrap("Ease In / Out") + " looks the most", "natural for camera moves.").br()
        .appendClick("Click to cycle")
        .build();

    private static final IconLocale LOCALE_TARGET = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Target").name("Camera Target")
        .appendCurrent("Set", GENERIC_STATE).br()
        .appendInfo("Where the camera ends up.").br()
        .appendInfo("Stand there, look the right", "way, then right-click with", "the capture tool.").br()
        .appendClick("Click to get capture tool")
        .build();

    private static final IconLocale LOCALE_SOUND = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Sound").name("Sound")
        .appendCurrent("Sound", GENERIC_VALUE)
        .appendCurrent("Volume", GENERIC_AMOUNT)
        .appendCurrent("Pitch", GENERIC_CURRENT).br()
        .appendClick("Click to edit")
        .build();

    private static final IconLocale LOCALE_TITLE = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Title").name("Title Text")
        .appendCurrent("Title", GENERIC_NAME)
        .appendCurrent("Subtitle", GENERIC_VALUE).br()
        .appendClick("Click to edit")
        .build();

    private static final IconLocale LOCALE_PARTICLE = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Particle").name("Particle")
        .appendCurrent("Particle", GENERIC_VALUE)
        .appendCurrent("Count", GENERIC_AMOUNT).br()
        .appendClick("Click to edit")
        .build();

    private static final IconLocale LOCALE_TRANSFORM = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Transform").name("Prop Transform")
        .appendCurrent("Offset", GENERIC_VALUE)
        .appendCurrent("Rotation", GENERIC_CURRENT)
        .appendCurrent("Scale", GENERIC_AMOUNT).br()
        .appendClick("Click to edit")
        .build();

    private static final IconLocale LOCALE_FIREWORK = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Firework").name("Firework")
        .appendCurrent("Status", GENERIC_STATE).br()
        .appendInfo("Sets off a firework when the", "reward is revealed.").br()
        .appendClick("Click to toggle")
        .build();

    private static final IconLocale LOCALE_WAIT_INFO = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.WaitInfo").name("Wait")
        .appendInfo("Holds the timeline still.").br()
        .appendInfo("The camera and prop stay", "exactly where the previous", "frames left them.")
        .build();

    private static final IconLocale LOCALE_DELETE = LangEntry.iconBuilder("Editor.Button.Cinematic.Frame.Delete")
        .accentColor(RED)
        .name("Delete Frame")
        .appendInfo("Removes this frame from", "the timeline.").br()
        .appendClick("Press [" + TagWrappers.KEY.apply("key.drop") + "] to delete")
        .build();

    private final DialogRegistry dialogs;

    public CinematicFrameMenu(@NotNull CratesPlugin plugin, @NotNull DialogRegistry dialogs) {
        super(plugin, MenuType.GENERIC_9X5, Lang.EDITOR_TITLE_CINEMATIC_FRAME.text());
        this.dialogs = dialogs;
        this.plugin.injectLang(this);

        this.addItem(MenuItem.buildReturn(this, 40, (viewer, event) -> {
            CinematicProvider provider = this.getLink(viewer).provider();
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrames(viewer.getPlayer(), provider));
        }));

        this.addItem(MenuItem.background(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray()));
        this.addItem(MenuItem.background(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 36).toArray()));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        FrameRef ref = this.getLink(player);
        CinematicFrame frame = ref.frame();

        // The frame can vanish if it was deleted from another screen; bail back to the timeline.
        if (frame == null) {
            this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrames(player, ref.provider()));
            return;
        }

        viewer.addItem(NightItem.fromType(Material.CLOCK)
            .localized(LOCALE_DURATION)
            .replacement(replacer -> replacer.replace(GENERIC_TIME, () -> CinematicFrame.formatTicks(frame.getDuration())))
            .toMenuItem().setSlots(10).setHandler((viewer1, event) -> {
                this.showDialog(player, CinematicDialogs.FRAME_DURATION, ref);
            }).build()
        );

        this.addEasingButton(viewer, player, ref, frame);
        this.addTargetButton(viewer, player, ref, frame);
        this.addTypeOptionsButton(viewer, player, ref, frame);

        viewer.addItem(NightItem.fromType(Material.BARRIER)
            .localized(LOCALE_DELETE)
            .toMenuItem().setSlots(44).setHandler((viewer1, event) -> {
                if (event.getClick() != ClickType.DROP) return;

                ref.provider().getScene().removeFrame(ref.index());
                ref.save();
                this.runNextTick(() -> this.plugin.getEditorManager().openCinematicFrames(player, ref.provider()));
            }).build()
        );
    }

    /** Only movement frames interpolate, so only they carry an easing curve. */
    private void addEasingButton(@NotNull MenuViewer viewer, @NotNull Player player, @NotNull FrameRef ref, @NotNull CinematicFrame frame) {
        if (!(frame instanceof MoveCameraFrame) && !(frame instanceof PropTransformFrame)) return;

        viewer.addItem(NightItem.fromType(Material.COMPARATOR)
            .localized(LOCALE_EASING)
            .replacement(replacer -> replacer.replace(GENERIC_VALUE, () -> currentEasing(frame).getName()))
            .toMenuItem().setSlots(11).setHandler((viewer1, event) -> {
                CinematicFrame target = ref.frame();
                if (target == null) return;

                Easing next = nextEasing(currentEasing(target));
                if (target instanceof MoveCameraFrame move) move.setEasing(next);
                else if (target instanceof PropTransformFrame transform) transform.setEasing(next);

                ref.save();
                this.runNextTick(() -> this.flush(player));
            }).build()
        );
    }

    /** Camera frames aim at a world position, captured rather than typed. */
    private void addTargetButton(@NotNull MenuViewer viewer, @NotNull Player player, @NotNull FrameRef ref, @NotNull CinematicFrame frame) {
        boolean hasTarget;
        if (frame instanceof MoveCameraFrame move) hasTarget = !move.getTarget().isEmpty();
        else if (frame instanceof TeleportCameraFrame teleport) hasTarget = !teleport.getTarget().isEmpty();
        else return;

        viewer.addItem(NightItem.fromType(Material.ENDER_EYE)
            .localized(LOCALE_TARGET)
            .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_YES_NO.get(hasTarget)))
            .toMenuItem().setSlots(12).setHandler((viewer1, event) -> {
                this.plugin.getCinematicManager().giveCaptureTool(player, ref.provider(), CaptureTarget.FRAME_CAMERA, ref.index());
                this.runNextTick(player::closeInventory);
            }).build()
        );
    }

    /**
     * Renders the one button that is specific to this frame's type.
     */
    private void addTypeOptionsButton(@NotNull MenuViewer viewer, @NotNull Player player, @NotNull FrameRef ref, @NotNull CinematicFrame frame) {
        switch (frame) {
            case PlaySoundFrame sound -> viewer.addItem(NightItem.fromType(Material.NOTE_BLOCK)
                .localized(LOCALE_SOUND)
                .replacement(replacer -> replacer
                    .replace(GENERIC_VALUE, sound::getSoundKey)
                    .replace(GENERIC_AMOUNT, () -> String.valueOf(sound.getVolume()))
                    .replace(GENERIC_CURRENT, () -> String.valueOf(sound.getPitch()))
                )
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).setHandler((viewer1, event) -> {
                    this.showDialog(player, CinematicDialogs.FRAME_SOUND, ref);
                }).build()
            );

            case SendTitleFrame title -> viewer.addItem(NightItem.fromType(Material.WRITABLE_BOOK)
                .localized(LOCALE_TITLE)
                .replacement(replacer -> replacer
                    .replace(GENERIC_NAME, () -> title.getTitle().isBlank() ? CoreLang.OTHER_NONE.text() : title.getTitle())
                    .replace(GENERIC_VALUE, () -> title.getSubtitle().isBlank() ? CoreLang.OTHER_NONE.text() : title.getSubtitle())
                )
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).setHandler((viewer1, event) -> {
                    this.showDialog(player, CinematicDialogs.FRAME_TITLE, ref);
                }).build()
            );

            case ParticleFrame particle -> viewer.addItem(NightItem.fromType(Material.BLAZE_POWDER)
                .localized(LOCALE_PARTICLE)
                .replacement(replacer -> replacer
                    .replace(GENERIC_VALUE, particle::getParticleKey)
                    .replace(GENERIC_AMOUNT, () -> String.valueOf(particle.getCount()))
                )
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).setHandler((viewer1, event) -> {
                    this.showDialog(player, CinematicDialogs.FRAME_PARTICLE, ref);
                }).build()
            );

            case PropTransformFrame transform -> viewer.addItem(NightItem.fromType(Material.ARMOR_STAND)
                .localized(LOCALE_TRANSFORM)
                .replacement(replacer -> replacer
                    .replace(GENERIC_VALUE, () -> transform.getOffsetX() + ", " + transform.getOffsetY() + ", " + transform.getOffsetZ())
                    .replace(GENERIC_CURRENT, () -> transform.getYaw() + " / " + transform.getPitch())
                    .replace(GENERIC_AMOUNT, () -> String.valueOf(transform.getScale()))
                )
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).setHandler((viewer1, event) -> {
                    this.showDialog(player, CinematicDialogs.FRAME_TRANSFORM, ref);
                }).build()
            );

            case SpawnRewardFrame spawn -> viewer.addItem(NightItem.fromType(Material.FIREWORK_ROCKET)
                .localized(LOCALE_FIREWORK)
                .replacement(replacer -> replacer.replace(GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(spawn.isFirework())))
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).setHandler((viewer1, event) -> {
                    if (!(ref.frame() instanceof SpawnRewardFrame target)) return;

                    target.setFirework(!target.isFirework());
                    ref.save();
                    this.runNextTick(() -> this.flush(player));
                }).build()
            );

            case WaitFrame ignored -> viewer.addItem(NightItem.fromType(Material.CLOCK)
                .localized(LOCALE_WAIT_INFO)
                .toMenuItem().setSlots(SLOT_TYPE_OPTIONS).build()
            );

            default -> {
                // Camera frames are fully covered by the duration, easing and target buttons.
            }
        }
    }

    private void showDialog(@NotNull Player player, @NotNull DialogKey<FrameRef> key, @NotNull FrameRef ref) {
        this.dialogs.show(player, key, ref, () -> this.flush(player));
    }

    @NotNull
    private static Easing currentEasing(@NotNull CinematicFrame frame) {
        if (frame instanceof MoveCameraFrame move) return move.getEasing();
        if (frame instanceof PropTransformFrame transform) return transform.getEasing();
        return Easing.LINEAR;
    }

    /**
     * @return the next curve in declaration order, wrapping around at the end.
     */
    @NotNull
    private static Easing nextEasing(@NotNull Easing easing) {
        Easing[] values = Easing.values();
        return values[(easing.ordinal() + 1) % values.length];
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }
}
