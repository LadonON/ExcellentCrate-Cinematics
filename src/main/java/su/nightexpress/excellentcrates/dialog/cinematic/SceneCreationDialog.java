package su.nightexpress.excellentcrates.dialog.cinematic;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.cinematic.CinematicManager;
import su.nightexpress.excellentcrates.dialog.generic.GenericCreationDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.TextLocale;

/**
 * Creates a new cinematic scene, reusing the shared creation dialog rather than forking it.
 */
public class SceneCreationDialog extends GenericCreationDialog<CinematicManager> {

    private static final TextLocale TITLE = LangEntry.builder("Dialog.Cinematic.Creation.Title").text(title("Cinematic", "Creation"));

    @Override
    @NotNull
    protected TextLocale title() {
        return TITLE;
    }

    @Override
    protected boolean canCreate(@NotNull CinematicManager source, @NotNull String id) {
        return source.canCreateScene(id);
    }

    @Override
    protected void create(@NotNull CinematicManager source, @NotNull String id) {
        source.createScene(id);
    }
}
