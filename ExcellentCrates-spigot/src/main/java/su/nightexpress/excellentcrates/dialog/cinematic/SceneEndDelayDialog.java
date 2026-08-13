package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;
import su.nightexpress.nightcore.util.NumberUtil;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Sets how many ticks after the delegate opening finishes the player is actually teleported back -
 * the "end" delay of the cinematic, letting the delegate's final frame (or the model prop's held
 * pose) linger before the camera lock releases.
 */
public class SceneEndDelayDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.EndDelay.Title").text(title("Cinematic", "End Delay"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.EndDelay.Body").dialogElement(400,
        "How many ticks after the delegate opening finishes the player",
        "is teleported back. 20 ticks = 1 second.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Zero means the return happens the same tick the delegate",
        "    finishes. Useful for letting the final scene linger a moment."
    );

    private static final TextLocale INPUT_TICKS = LangEntry.builder("Dialog.Cinematic.EndDelay.Input.Ticks").text("Delay " + SOFT_YELLOW.wrap("(ticks)"));

    private static final String JSON_TICKS = "ticks";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_TICKS, INPUT_TICKS).initial(String.valueOf(scene.getEndDelay())).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                int ticks = NumberUtil.getAnyInteger(nbtHolder.getText(JSON_TICKS, ""), scene.getEndDelay());
                scene.setEndDelay(ticks);
                provider.save();
                viewer.callback();
            });
        });
    }
}
