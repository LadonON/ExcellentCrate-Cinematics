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
 * Sets which way the model prop faces when it spawns at the stage, independent of whichever way the
 * admin happened to be looking when they captured the stage location.
 */
public class SceneModelYawDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.ModelYaw.Title").text(title("Cinematic", "Model Orientation"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.ModelYaw.Body").dialogElement(400,
        "Which way the model prop faces when it spawns, in degrees.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Uses the same convention as a player's own facing:",
        "    0 = south, 90 = west, 180 = north, 270 = east."
    );

    private static final TextLocale INPUT_YAW = LangEntry.builder("Dialog.Cinematic.ModelYaw.Input.Yaw").text("Yaw " + SOFT_YELLOW.wrap("(degrees)"));

    private static final String JSON_YAW = "yaw";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_YAW, INPUT_YAW).initial(String.valueOf(scene.getModelYaw())).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                double yaw = NumberUtil.getAnyDouble(nbtHolder.getText(JSON_YAW, ""), scene.getModelYaw());
                scene.setModelYaw(yaw);
                provider.save();
                viewer.callback();
            });
        });
    }
}
