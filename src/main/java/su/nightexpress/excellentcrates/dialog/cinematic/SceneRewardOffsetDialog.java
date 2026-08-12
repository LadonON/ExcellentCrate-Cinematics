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

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Sets where the reward appears, as an offset from the prop.
 *
 * <p>Three numbers, so a dialog is the right tool — unlike the camera and prop positions, which use
 * the capture tool because typing coordinates for those is miserable.
 */
public class SceneRewardOffsetDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.RewardOffset.Title").text(title("Cinematic", "Reward Offset"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.RewardOffset.Body").dialogElement(400,
        "Sets where the reward is unveiled, relative to the " + SOFT_YELLOW.wrap("prop location") + ".",
        "",
        "For example, " + SOFT_YELLOW.wrap("Y = 1.5") + " reveals the reward one and a half blocks above the prop."
    );

    private static final TextLocale INPUT_X = LangEntry.builder("Dialog.Cinematic.RewardOffset.Input.X").text("X " + GRAY.wrap("(east / west)"));
    private static final TextLocale INPUT_Y = LangEntry.builder("Dialog.Cinematic.RewardOffset.Input.Y").text("Y " + GRAY.wrap("(up / down)"));
    private static final TextLocale INPUT_Z = LangEntry.builder("Dialog.Cinematic.RewardOffset.Input.Z").text("Z " + GRAY.wrap("(south / north)"));

    private static final String JSON_X = "x";
    private static final String JSON_Y = "y";
    private static final String JSON_Z = "z";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(
                    DialogInputs.text(JSON_X, INPUT_X).initial(String.valueOf(scene.getRewardOffsetX())).build(),
                    DialogInputs.text(JSON_Y, INPUT_Y).initial(String.valueOf(scene.getRewardOffsetY())).build(),
                    DialogInputs.text(JSON_Z, INPUT_Z).initial(String.valueOf(scene.getRewardOffsetZ())).build()
                )
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                scene.setRewardOffsetX(readDouble(nbtHolder.getText(JSON_X, ""), scene.getRewardOffsetX()));
                scene.setRewardOffsetY(readDouble(nbtHolder.getText(JSON_Y, ""), scene.getRewardOffsetY()));
                scene.setRewardOffsetZ(readDouble(nbtHolder.getText(JSON_Z, ""), scene.getRewardOffsetZ()));
                provider.save();
                viewer.callback();
            });
        });
    }

    /**
     * Keeps the previous value when the field is left blank or contains something unparseable,
     * rather than silently snapping the offset to zero.
     */
    static double readDouble(@NotNull String raw, double fallback) {
        return raw.isBlank() ? fallback : NumberUtil.getAnyDouble(raw, fallback);
    }
}
