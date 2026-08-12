package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.PlaySoundFrame;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Edits a {@link PlaySoundFrame}.
 */
public class FrameSoundDialog extends Dialog<FrameRef> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Frame.Sound.Title").text(title("Frame", "Sound"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Frame.Sound.Body").dialogElement(400,
        "Plays a sound for the player watching the cinematic.",
        "",
        "The sound is a key such as " + SOFT_YELLOW.wrap("minecraft:block.beacon.activate") + ".",
        "Sounds added by your resource pack work too.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Pitch is clamped between " + SOFT_YELLOW.wrap("0.5") + " and " + SOFT_YELLOW.wrap("2.0") + ", as in vanilla."
    );

    private static final TextLocale INPUT_SOUND  = LangEntry.builder("Dialog.Cinematic.Frame.Sound.Input.Sound").text("Sound Key");
    private static final TextLocale INPUT_VOLUME = LangEntry.builder("Dialog.Cinematic.Frame.Sound.Input.Volume").text("Volume");
    private static final TextLocale INPUT_PITCH  = LangEntry.builder("Dialog.Cinematic.Frame.Sound.Input.Pitch").text("Pitch " + GRAY.wrap("(0.5 - 2.0)"));

    private static final String JSON_SOUND  = "sound";
    private static final String JSON_VOLUME = "volume";
    private static final String JSON_PITCH  = "pitch";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull FrameRef ref) {
        PlaySoundFrame frame = ref.frame() instanceof PlaySoundFrame sound ? sound : null;
        String currentKey = frame == null ? "" : frame.getSoundKey();
        float currentVolume = frame == null ? 1F : frame.getVolume();
        float currentPitch = frame == null ? 1F : frame.getPitch();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(
                    DialogInputs.text(JSON_SOUND, INPUT_SOUND).initial(currentKey).maxLength(200).build(),
                    DialogInputs.text(JSON_VOLUME, INPUT_VOLUME).initial(String.valueOf(currentVolume)).build(),
                    DialogInputs.text(JSON_PITCH, INPUT_PITCH).initial(String.valueOf(currentPitch)).build()
                )
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                if (!(ref.frame() instanceof PlaySoundFrame target)) return;

                String key = nbtHolder.getText(JSON_SOUND, target.getSoundKey());
                if (!key.isBlank()) target.setSoundKey(key.trim());

                target.setVolume((float) SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_VOLUME, ""), target.getVolume()));
                target.setPitch((float) SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_PITCH, ""), target.getPitch()));

                ref.save();
                viewer.callback();
            });
        });
    }
}
