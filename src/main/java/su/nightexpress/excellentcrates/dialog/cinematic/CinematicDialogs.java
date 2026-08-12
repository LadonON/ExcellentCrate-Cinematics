package su.nightexpress.excellentcrates.dialog.cinematic;

import su.nightexpress.excellentcrates.cinematic.CinematicManager;
import su.nightexpress.excellentcrates.cinematic.FrameRef;
import su.nightexpress.excellentcrates.dialog.DialogKey;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;

/**
 * Dialog keys for the cinematic editor, mirroring {@code CrateDialogs} and {@code RewardDialogs}.
 *
 * <p>Dialogs are used only for freeform text and numbers. Anything that is a toggle or a pick — the
 * easing curve, the firework flag, the frame type — stays in the chest menus, matching how the rest
 * of the editor splits the two.
 */
public class CinematicDialogs {

    public static final DialogKey<CinematicManager>  SCENE_CREATION      = new DialogKey<>("cinematic_scene_creation");
    public static final DialogKey<CinematicProvider> SCENE_NAME          = new DialogKey<>("cinematic_scene_name");
    public static final DialogKey<CinematicProvider> SCENE_REWARD_OFFSET = new DialogKey<>("cinematic_scene_reward_offset");

    public static final DialogKey<FrameRef> FRAME_DURATION  = new DialogKey<>("cinematic_frame_duration");
    public static final DialogKey<FrameRef> FRAME_SOUND     = new DialogKey<>("cinematic_frame_sound");
    public static final DialogKey<FrameRef> FRAME_TITLE     = new DialogKey<>("cinematic_frame_title");
    public static final DialogKey<FrameRef> FRAME_PARTICLE  = new DialogKey<>("cinematic_frame_particle");
    public static final DialogKey<FrameRef> FRAME_TRANSFORM = new DialogKey<>("cinematic_frame_transform");
}
