package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.single.WrappedSingleOptionEntry;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Picks which existing opening animation actually runs at the scene's stage.
 *
 * <p>Lists every loaded opening id except other cinematic scenes — delegating to another cinematic
 * would either loop back on itself or chain teleports indefinitely, so those are left off the list
 * entirely rather than merely discouraged.
 */
public class SceneOpeningDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Opening.Title").text(title("Cinematic", "Opening Animation"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Opening.Body").dialogElement(400,
        "Which opening animation actually runs once the player arrives at the stage.",
        "",
        "This is the same kind of animation a crate would normally use directly -"
            + " " + SOFT_YELLOW.wrap("simple_roll") + ", " + SOFT_YELLOW.wrap("csgo") + ", or any other id under "
            + SOFT_YELLOW.wrap("openings/") + ".",
        "",
        SOFT_YELLOW.wrap("→ ") + "It rolls and grants the reward itself, exactly as it would for",
        "    a crate configured with that id directly."
    );

    private static final TextLocale INPUT_OPENING = LangEntry.builder("Dialog.Cinematic.Opening.Input.Opening").text(SOFT_YELLOW.wrap("Opening"));

    private static final String JSON_ID = "id";

    private final CratesPlugin plugin;

    public SceneOpeningDialog(@NotNull CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        List<WrappedSingleOptionEntry> entries = new ArrayList<>();
        this.plugin.getCinematicManager().getDelegatableOpeningIds().forEach(id ->
            entries.add(new WrappedSingleOptionEntry(id, id, scene.getOpeningId().equalsIgnoreCase(id)))
        );

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.singleOption(JSON_ID, INPUT_OPENING, entries).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                scene.setOpeningId(nbtHolder.getText(JSON_ID, scene.getOpeningId()));
                provider.save();
                viewer.callback();
            });
        });
    }
}
