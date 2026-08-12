package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.ParticleFrame;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Edits a {@link ParticleFrame}.
 */
public class FrameParticleDialog extends Dialog<FrameRef> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Title").text(title("Frame", "Particle"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Body").dialogElement(400,
        "Emits a burst of particles at the prop.",
        "",
        "The particle is a key such as " + SOFT_YELLOW.wrap("minecraft:cloud") + " or " + SOFT_YELLOW.wrap("minecraft:end_rod") + ".",
        "",
        SOFT_YELLOW.wrap("→ ") + "Particles that need extra data (such as " + SOFT_YELLOW.wrap("dust") + ") are skipped at playback.",
        SOFT_YELLOW.wrap("→ ") + "The offsets spread the particles out; speed controls how fast they drift."
    );

    private static final TextLocale INPUT_PARTICLE = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.Particle").text("Particle Key");
    private static final TextLocale INPUT_COUNT    = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.Count").text("Count");
    private static final TextLocale INPUT_OFFSET_X = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.OffsetX").text("Offset X");
    private static final TextLocale INPUT_OFFSET_Y = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.OffsetY").text("Offset Y");
    private static final TextLocale INPUT_OFFSET_Z = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.OffsetZ").text("Offset Z");
    private static final TextLocale INPUT_SPEED    = LangEntry.builder("Dialog.Cinematic.Frame.Particle.Input.Speed").text("Speed " + GRAY.wrap("(0 = still)"));

    private static final String JSON_PARTICLE = "particle";
    private static final String JSON_COUNT    = "count";
    private static final String JSON_OFFSET_X = "offset_x";
    private static final String JSON_OFFSET_Y = "offset_y";
    private static final String JSON_OFFSET_Z = "offset_z";
    private static final String JSON_SPEED    = "speed";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull FrameRef ref) {
        ParticleFrame frame = ref.frame() instanceof ParticleFrame particle ? particle : null;

        String currentKey = frame == null ? "" : frame.getParticleKey();
        int currentCount = frame == null ? 0 : frame.getCount();
        double currentX = frame == null ? 0D : frame.getOffsetX();
        double currentY = frame == null ? 0D : frame.getOffsetY();
        double currentZ = frame == null ? 0D : frame.getOffsetZ();
        double currentSpeed = frame == null ? 0D : frame.getSpeed();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(
                    DialogInputs.text(JSON_PARTICLE, INPUT_PARTICLE).initial(currentKey).maxLength(200).build(),
                    DialogInputs.text(JSON_COUNT, INPUT_COUNT).initial(String.valueOf(currentCount)).build(),
                    DialogInputs.text(JSON_OFFSET_X, INPUT_OFFSET_X).initial(String.valueOf(currentX)).build(),
                    DialogInputs.text(JSON_OFFSET_Y, INPUT_OFFSET_Y).initial(String.valueOf(currentY)).build(),
                    DialogInputs.text(JSON_OFFSET_Z, INPUT_OFFSET_Z).initial(String.valueOf(currentZ)).build(),
                    DialogInputs.text(JSON_SPEED, INPUT_SPEED).initial(String.valueOf(currentSpeed)).build()
                )
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                if (!(ref.frame() instanceof ParticleFrame target)) return;

                String key = nbtHolder.getText(JSON_PARTICLE, target.getParticleKey());
                if (!key.isBlank()) target.setParticleKey(key.trim());

                target.setCount(nbtHolder.getInt(JSON_COUNT, target.getCount()));
                target.setOffsetX(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_X, ""), target.getOffsetX()));
                target.setOffsetY(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_Y, ""), target.getOffsetY()));
                target.setOffsetZ(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_OFFSET_Z, ""), target.getOffsetZ()));
                target.setSpeed(SceneRewardOffsetDialog.readDouble(nbtHolder.getText(JSON_SPEED, ""), target.getSpeed()));

                ref.save();
                viewer.callback();
            });
        });
    }
}
