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

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Sets which animation plays on the model prop the instant it spawns at the stage.
 *
 * <p>Whether it holds on its last frame afterwards, loops, or does anything else is entirely up to
 * how that animation's loop mode is authored in the blueprint - this only picks which one starts.
 */
public class SceneModelAnimationDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.ModelAnimation.Title").text(title("Cinematic", "Model Animation"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.ModelAnimation.Body").dialogElement(400,
        "The animation played on the model prop the instant it spawns.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Whether it holds on its last frame, loops, or does anything",
        "    else afterwards is set on the animation itself in Blockbench -",
        "    this only picks which animation starts."
    );

    private static final TextLocale INPUT_ANIMATION = LangEntry.builder("Dialog.Cinematic.ModelAnimation.Input.Animation").text(SOFT_YELLOW.wrap("Animation"));

    private static final String JSON_ANIMATION = "animation";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_ANIMATION, INPUT_ANIMATION).initial(scene.getModelAnimation()).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                String animation = nbtHolder.getText(JSON_ANIMATION, scene.getModelAnimation());
                scene.setModelAnimation(animation.isBlank() ? CinematicScene.DEFAULT_MODEL_ANIMATION : animation);
                provider.save();
                viewer.callback();
            });
        });
    }
}
