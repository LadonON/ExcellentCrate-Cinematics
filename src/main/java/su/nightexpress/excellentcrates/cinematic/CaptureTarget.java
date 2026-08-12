package su.nightexpress.excellentcrates.cinematic;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.Enums;

/**
 * What a capture tool writes when the admin right-clicks.
 *
 * <p>One tool implementation serves all three, which is why the target is stamped onto the item
 * rather than expressed as three separate tools.
 */
public enum CaptureTarget {

    /** Where the viewer's body is parked for the duration of the cinematic. */
    SCENE_PLAYER("Viewer Position"),

    /** Where the crate prop is spawned. */
    SCENE_PROP("Prop Position"),

    /**
     * The destination of a camera keyframe. Only this target uses the frame index stamped alongside
     * it on the tool.
     */
    FRAME_CAMERA("Camera Target");

    private final String name;

    CaptureTarget(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return the target named by {@code name}, or {@link #SCENE_PLAYER} if it is unrecognised —
     * only reachable via a hand-crafted item, so a harmless default is preferable to throwing.
     */
    @NotNull
    public static CaptureTarget byName(@NotNull String name) {
        return Enums.parse(name, CaptureTarget.class).orElse(SCENE_PLAYER);
    }

    @NotNull
    public String getName() {
        return this.name;
    }
}
