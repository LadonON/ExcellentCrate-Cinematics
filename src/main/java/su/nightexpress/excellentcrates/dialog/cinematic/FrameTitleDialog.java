package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.SendTitleFrame;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Edits a {@link SendTitleFrame}: the two lines of text and the vanilla title timings.
 */
public class FrameTitleDialog extends Dialog<FrameRef> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Frame.Title.Title").text(title("Frame", "Title"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Frame.Title.Body").dialogElement(400,
        "Shows a title and subtitle to the player watching the cinematic.",
        "",
        "The three timings are in ticks and are independent of the frame's own duration, so a title",
        "can keep fading out while later frames run.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Leave a line empty to show nothing on it."
    );

    private static final TextLocale INPUT_TITLE    = LangEntry.builder("Dialog.Cinematic.Frame.Title.Input.Title").text("Title");
    private static final TextLocale INPUT_SUBTITLE = LangEntry.builder("Dialog.Cinematic.Frame.Title.Input.Subtitle").text("Subtitle");
    private static final TextLocale INPUT_FADE_IN  = LangEntry.builder("Dialog.Cinematic.Frame.Title.Input.FadeIn").text("Fade In " + GRAY.wrap("(ticks)"));
    private static final TextLocale INPUT_STAY     = LangEntry.builder("Dialog.Cinematic.Frame.Title.Input.Stay").text("Stay " + GRAY.wrap("(ticks)"));
    private static final TextLocale INPUT_FADE_OUT = LangEntry.builder("Dialog.Cinematic.Frame.Title.Input.FadeOut").text("Fade Out " + GRAY.wrap("(ticks)"));

    private static final String JSON_TITLE    = "title";
    private static final String JSON_SUBTITLE = "subtitle";
    private static final String JSON_FADE_IN  = "fade_in";
    private static final String JSON_STAY     = "stay";
    private static final String JSON_FADE_OUT = "fade_out";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull FrameRef ref) {
        SendTitleFrame frame = ref.frame() instanceof SendTitleFrame title ? title : null;

        String currentTitle = frame == null ? "" : frame.getTitle();
        String currentSubtitle = frame == null ? "" : frame.getSubtitle();
        int currentFadeIn = frame == null ? 10 : frame.getFadeIn();
        int currentStay = frame == null ? 40 : frame.getStay();
        int currentFadeOut = frame == null ? 10 : frame.getFadeOut();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(
                    DialogInputs.text(JSON_TITLE, INPUT_TITLE).initial(currentTitle).maxLength(300).build(),
                    DialogInputs.text(JSON_SUBTITLE, INPUT_SUBTITLE).initial(currentSubtitle).maxLength(300).build(),
                    DialogInputs.text(JSON_FADE_IN, INPUT_FADE_IN).initial(String.valueOf(currentFadeIn)).build(),
                    DialogInputs.text(JSON_STAY, INPUT_STAY).initial(String.valueOf(currentStay)).build(),
                    DialogInputs.text(JSON_FADE_OUT, INPUT_FADE_OUT).initial(String.valueOf(currentFadeOut)).build()
                )
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                if (!(ref.frame() instanceof SendTitleFrame target)) return;

                target.setTitle(nbtHolder.getText(JSON_TITLE, target.getTitle()));
                target.setSubtitle(nbtHolder.getText(JSON_SUBTITLE, target.getSubtitle()));
                target.setFadeIn(nbtHolder.getInt(JSON_FADE_IN, target.getFadeIn()));
                target.setStay(nbtHolder.getInt(JSON_STAY, target.getStay()));
                target.setFadeOut(nbtHolder.getInt(JSON_FADE_OUT, target.getFadeOut()));

                ref.save();
                viewer.callback();
            });
        });
    }
}
