package su.nightexpress.excellentcrates.opening.cinematic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.crate.cost.Cost;
import su.nightexpress.excellentcrates.crate.impl.CrateSource;
import su.nightexpress.excellentcrates.opening.AbstractProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene;
import su.nightexpress.nightcore.config.FileConfig;

import java.io.File;

/**
 * Opening provider backing one file under {@code openings/cinematic/}.
 *
 * <p>Registered in {@code ProviderRegistry} exactly like the Inventory, Simple Roll and Selectable
 * providers, so a cinematic scene appears in the crate's "Opening Animation" picker automatically.
 *
 * <p>One provider owns exactly one {@link CinematicScene}, so "scene" and "opening id" are the same
 * thing from an admin's point of view.
 */
public class CinematicProvider extends AbstractProvider {

    private CinematicScene scene;

    /** Retained so the GUI editor can persist scene changes back to the originating file. */
    private FileConfig config;

    public CinematicProvider(@NotNull CratesPlugin plugin, @NotNull String id) {
        super(plugin, id);
        this.scene = new CinematicScene(id);
    }

    @Override
    public void load(@NotNull FileConfig config) {
        this.config = config;

        // A brand-new file keeps whatever scene the factory pre-seeded (see OpeningUtils) and gets
        // the full key layout written out, so it is obvious what can be hand-edited. Existing files
        // are only read - never rewritten on load.
        if (config.getKeys(false).isEmpty()) {
            this.scene.write(config);
            return;
        }

        this.scene = CinematicScene.read(config, this.id);
    }

    /**
     * Writes the scene back to its file. Called by the editor after every change, mirroring how
     * crates are marked dirty and flushed.
     */
    public void save() {
        if (this.config == null) return;

        this.scene.write(this.config);
        this.config.saveChanges();
    }

    @Override
    @NotNull
    public CinematicOpening createOpening(@NotNull Player player, @NotNull CrateSource source, @Nullable Cost cost) {
        return new CinematicOpening(this.plugin, player, source, cost, this.scene);
    }

    @NotNull
    public CinematicScene getScene() {
        return this.scene;
    }

    /**
     * @return the file this scene was loaded from, or {@code null} if it was registered
     * programmatically by an addon rather than loaded from disk.
     */
    @Nullable
    public File getFile() {
        return this.config == null ? null : this.config.getFile();
    }
}
