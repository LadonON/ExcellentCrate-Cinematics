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
 * Sets how far above the stage the player's camera is locked while the delegate opening runs.
 */
public class SceneCameraHeightDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.CameraHeight.Title").text(title("Cinematic", "Camera Height"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.CameraHeight.Body").dialogElement(400,
        "How far above the stage, in blocks, the player's locked camera sits.",
        "",
        SOFT_YELLOW.wrap("→ ") + "The stage location itself is unaffected - only where the",
        "    camera is placed above it changes.",
        SOFT_YELLOW.wrap("→ ") + "A negative value puts the camera below the stage instead."
    );

    private static final TextLocale INPUT_HEIGHT = LangEntry.builder("Dialog.Cinematic.CameraHeight.Input.Height").text("Height " + SOFT_YELLOW.wrap("(blocks)"));

    private static final String JSON_HEIGHT = "height";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_HEIGHT, INPUT_HEIGHT).initial(String.valueOf(scene.getCameraHeight())).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                double height = NumberUtil.getAnyDouble(nbtHolder.getText(JSON_HEIGHT, ""), scene.getCameraHeight());
                scene.setCameraHeight(height);
                provider.save();
                viewer.callback();
            });
        });
    }
}
