package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Sets how long a frame occupies the timeline.
 */
public class FrameDurationDialog extends Dialog<FrameRef> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Frame.Duration.Title").text(title("Frame", "Duration"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Frame.Duration.Body").dialogElement(400,
        "Sets how long this frame lasts, in ticks. There are " + SOFT_YELLOW.wrap("20 ticks") + " in a second.",
        "",
        SOFT_YELLOW.wrap("→ ") + "A duration of " + SOFT_YELLOW.wrap("0") + " makes the frame instant, so the next frame runs in the same tick.",
        "That is how you stack a sound, a title and a camera cut onto one moment."
    );

    private static final TextLocale INPUT_DURATION = LangEntry.builder("Dialog.Cinematic.Frame.Duration.Input.Duration")
        .text("Duration " + GRAY.wrap("(in ticks)"));

    private static final String JSON_DURATION = "duration";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull FrameRef ref) {
        CinematicFrame frame = ref.frame();
        long current = frame == null ? 0L : frame.getDuration();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_DURATION, INPUT_DURATION).initial(String.valueOf(current)).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                // Re-resolve: the frame may have been deleted or moved while the dialog was open.
                CinematicFrame target = ref.frame();
                if (target == null) return;

                target.setDuration(nbtHolder.getInt(JSON_DURATION, (int) target.getDuration()));
                ref.save();
                viewer.callback();
            });
        });
    }
}
