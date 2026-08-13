package su.nightexpress.excellentcrates.dialog.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.dialog.Dialog;
import su.nightexpress.excellentcrates.hooks.impl.ModelEngineHook;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.single.WrappedSingleOptionEntry;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.*;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Picks which ModelEngine blueprint, if any, spawns as the crate model at the stage.
 *
 * <p>Lists every blueprint ModelEngine currently has registered, plus a "None" entry so the field
 * stays optional - a scene works fine without a model, exactly as before this existed.
 */
public class SceneModelDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Model.Title").text(title("Cinematic", "Model"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Model.Body").dialogElement(400,
        "Which ModelEngine blueprint spawns at the stage's crate block when the player arrives.",
        "",
        SOFT_YELLOW.wrap("→ ") + "Optional - pick " + SOFT_YELLOW.wrap("None") + " to render nothing of your own and",
        "    leave the reveal entirely to the delegate opening.",
        SOFT_YELLOW.wrap("→ ") + "Requires the ModelEngine plugin to be installed."
    );

    private static final TextLocale INPUT_MODEL = LangEntry.builder("Dialog.Cinematic.Model.Input.Model").text(SOFT_YELLOW.wrap("Model"));

    private static final String JSON_ID = "id";
    private static final String NONE_ID = "";

    private final CratesPlugin plugin;

    public SceneModelDialog(@NotNull CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        List<WrappedSingleOptionEntry> entries = new ArrayList<>();
        entries.add(new WrappedSingleOptionEntry(NONE_ID, CoreLang.OTHER_NONE.text(), !scene.hasModel()));
        ModelEngineHook.getModelIds().forEach(id ->
            entries.add(new WrappedSingleOptionEntry(id, id, scene.getModelId().equalsIgnoreCase(id)))
        );

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.singleOption(JSON_ID, INPUT_MODEL, entries).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                scene.setModelId(nbtHolder.getText(JSON_ID, scene.getModelId()));
                provider.save();
                viewer.callback();
            });
        });
    }
}
