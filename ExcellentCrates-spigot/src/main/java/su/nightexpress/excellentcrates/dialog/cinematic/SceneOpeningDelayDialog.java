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
 * Sets how many ticks after the player arrives at the stage the delegate opening actually starts.
 *
 * <p>Example: a 40-tick (2s) opening delay holds the player on the stage for 2 seconds before the
 * real opening animation begins. Zero starts it the instant they arrive.
 */
public class SceneOpeningDelayDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.OpeningDelay.Title").text(title("Cinematic", "Opening Delay"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.OpeningDelay.Body").dialogElement(400,
        "How many ticks after the model prop's animation triggers the",
        "delegate opening animation actually starts. 20 ticks = 1 second.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Added on top of Start Delay, not counted from arrival - a 40-",
        "    tick start delay plus a 20-tick opening delay starts the",
        "    opening 3 seconds after the player arrives."
    );

    private static final TextLocale INPUT_TICKS = LangEntry.builder("Dialog.Cinematic.OpeningDelay.Input.Ticks").text("Delay " + SOFT_YELLOW.wrap("(ticks)"));

    private static final String JSON_TICKS = "ticks";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_TICKS, INPUT_TICKS).initial(String.valueOf(scene.getOpeningDelay())).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                int ticks = NumberUtil.getAnyInteger(nbtHolder.getText(JSON_TICKS, ""), scene.getOpeningDelay());
                scene.setOpeningDelay(ticks);
                provider.save();
                viewer.callback();
            });
        });
    }
}
