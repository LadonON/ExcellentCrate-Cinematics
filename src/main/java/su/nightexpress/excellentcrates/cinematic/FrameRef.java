package su.nightexpress.excellentcrates.cinematic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;
import su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicFrame;

/**
 * Points at one frame inside a scene.
 *
 * <p>Frames have no identity of their own — a frame <i>is</i> its position in the timeline — so
 * menus and dialogs address one by scene plus index rather than by holding the object. Resolving
 * through {@link #frame()} on each use means a frame that was deleted or moved from another screen
 * shows up as {@code null} instead of being silently edited in a detached copy.
 *
 * @param index position in {@link su.nightexpress.excellentcrates.opening.cinematic.scene.CinematicScene#getFrames()}.
 */
public record FrameRef(@NotNull CinematicProvider provider, int index) {

    /**
     * @return the referenced frame, or {@code null} if the index is no longer valid.
     */
    @Nullable
    public CinematicFrame frame() {
        return this.provider.getScene().getFrame(this.index);
    }

    /**
     * Persists the owning scene. Called after any edit made through this reference.
     */
    public void save() {
        this.provider.save();
    }
}
