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
 * Renames a scene.
 *
 * <p>Not built on {@code GenericNameDialog} because that one also offers to rewrite an item's
 * display name, and a scene has no item.
 */
public class SceneNameDialog extends Dialog<CinematicProvider> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Name.Title").text(title("Cinematic", "Display Name"));

    private static final DialogElementLocale BODY = LangEntry.builder("Dialog.Cinematic.Name.Body").dialogElement(400,
        "Sets the scene's display name.",
        "",
        "This is only shown in the editor - the scene is still referenced by its "
            + SOFT_YELLOW.wrap("id") + " everywhere else."
    );

    private static final TextLocale INPUT_NAME = LangEntry.builder("Dialog.Cinematic.Name.Input.Name").text("Name");

    private static final String JSON_NAME = "name";

    @Override
    @NotNull
    public WrappedDialog create(@NotNull Player player, @NotNull CinematicProvider provider) {
        CinematicScene scene = provider.getScene();

        return Dialogs.create(builder -> {
            builder.base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_NAME, INPUT_NAME).initial(scene.getName()).maxLength(300).build())
                .build()
            );

            builder.type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build());

            builder.handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;

                scene.setName(nbtHolder.getText(JSON_NAME, scene.getName()));
                provider.save();
                viewer.callback();
            });
        });
    }
}
