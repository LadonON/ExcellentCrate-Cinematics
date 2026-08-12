package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.PropTransformFrame;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Edits the numeric side of a {@link PropTransformFrame}. The easing curve is picked in the frame
 * menu, since it is a choice rather than a value to type.
 */
public class FrameTransformDialog extends Dialog<FrameRef> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Title").text(title("Frame", "Prop Transform"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Body").dialogElement(400,
        "Moves, spins and scales the crate prop over this frame's duration.",
        "",
        "Offsets are relative to the scene's " + SOFT_YELLOW.wrap("prop location") + ", so the whole scene can be",
        "rebuilt somewhere else without touching any frame.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Rotation is in degrees and is added to the prop's base facing.",
        SOFT_YELLOW.wrap("→ ") + "Scale " + SOFT_YELLOW.wrap("1.0") + " is the model's normal size."
    );

    private static final TextLocale INPUT_OFFSET_X = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.OffsetX").text("Offset X");
    private static final TextLocale INPUT_OFFSET_Y = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.OffsetY").text("Offset Y");
    private static final TextLocale INPUT_OFFSET_Z = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.OffsetZ").text("Offset Z");
    private static final TextLocale INPUT_YAW      = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.Yaw").text("Yaw " + GRAY.wrap("(degrees)"));
    private static final TextLocale INPUT_PITCH    = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.Pitch").text("Pitch " + GRAY.wrap("(degrees)"));
    private static final TextLocale INPUT_SCALE    = LangEntry.builder("Dialog.Cinematic.Frame.Transform.Input.Scale").text("Scale");

    private static final String JSON_OFFSET_X = "offset_x";
    private static final String JSON_OFFSET_Y = "offset_y";
    private static final String JSON_OFFSET_Z = "offset_z";
    private static final String JSON_YAW      = "yaw";
    private static final String JSON_PITCH    = "pitch";
    private static final String JSON_SCALE    = "scale";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull FrameRef ref) {
        PropTransformFrame frame = ref.frame() instanceof PropTransformFrame transform ? transform : null;

        double currentX = frame == null ? 0D : frame.getOffsetX();
        double currentY = frame == null ? 0D : frame.getOffsetY();
        double currentZ = frame == null ? 0D : frame.getOffsetZ();
        float currentYaw = frame == null ? 0F : frame.getYaw();
        float currentPitch = frame == null ? 0F : frame.getPitch();
        double currentScale = frame == null ? 1D : frame.getScale();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(
                    DialogInputs.text(JSON_OFFSET_X, INPUT_OFFSET_X).initial(String.valueOf(currentX)).build(),
                    DialogInputs.text(JSON_OFFSET_Y, INPUT_OFFSET_Y).initial(String.valueOf(currentY)).build(),
                    DialogInputs.text(JSON_OFFSET_Z, INPUT_OFFSET_Z).initial(String.valueOf(currentZ)).build(),
                    DialogInputs.text(JSON_YAW, INPUT_YAW).initial(String.valueOf(currentYaw)).build(),
                    DialogInputs.text(JSON_PITCH, INPUT_PITCH).initial(String.valueOf(currentPitch)).build(),
                    DialogInputs.text(JSON_SCALE, INPUT_SCALE).initial(String.valueOf(currentScale)).build()
                )
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                if (!(ref.frame() instanceof PropTransformFrame target)) return;

                target.setOffsetX(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_X, ""), target.getOffsetX()));
                target.setOffsetY(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_Y, ""), target.getOffsetY()));
                target.setOffsetZ(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_Z, ""), target.getOffsetZ()));
                target.setYaw((float) SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_YAW, ""), target.getYaw()));
                target.setPitch((float) SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_PITCH, ""), target.getPitch()));
                target.setScale(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_SCALE, ""), target.getScale()));

                ref.save();
                viewer.callback();
            });
        });
    }
}
