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
 * Sets how many ticks after the player arrives at the stage the model prop spawns and plays its
 * animation - the "beginning" delay of the cinematic, before anything else happens.
 */
public class SceneStartDelayDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.StartDelay.Title").text(title("Cinematic", "Start Delay"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.StartDelay.Body").dialogElement(400,
        "How many ticks after the player arrives at the stage the model",
        "prop spawns and plays its animation. 20 ticks = 1 second.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Zero means the model appears the instant the player arrives."
    );

    private static final TextLocale INPUT_TICKS = LangEntry.builder("Dialog.Cinematic.StartDelay.Input.Ticks").text("Delay " + SOFT_YELLOW.wrap("(ticks)"));

    private static final String JSON_TICKS = "ticks";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_TICKS, INPUT_TICKS).initial(String.valueOf(scene.getStartDelay())).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                int ticks = NumberUtil.getAnyInteger(nbtHolder.getText(JSON_TICKS, ""), scene.getStartDelay());
                scene.setStartDelay(ticks);
                provider.save();
                viewer.callback();
            });
        });
    }
}
