package su.nightexpress.excellentcrates.dialog.cinematic;

import su.nightexpress.excellentcrates.cinematic.CinematicManager;
import su.nightexpress.excellentcrates.dialog.DialogKey;
import su.nightexpress.excellentcrates.opening.cinematic.CinematicProvider;

/**
 * Dialog keys for the cinematic editor.
 */
public class CinematicDialogs {

    public static final DialogKey<CinematicManager>  SCENE_CREATION     = new DialogKey<>("cinematic_scene_creation");
    public static final DialogKey<CinematicProvider> SCENE_NAME         = new DialogKey<>("cinematic_scene_name");
    public static final DialogKey<CinematicProvider> SCENE_OPENING      = new DialogKey<>("cinematic_scene_opening");
    public static final DialogKey<CinematicProvider> SCENE_CAMERA_HEIGHT   = new DialogKey<>("cinematic_scene_camera_height");
    public static final DialogKey<CinematicProvider> SCENE_MODEL           = new DialogKey<>("cinematic_scene_model");
    public static final DialogKey<CinematicProvider> SCENE_MODEL_ANIMATION = new DialogKey<>("cinematic_scene_model_animation");
    public static final DialogKey<CinematicProvider> SCENE_MODEL_YAW       = new DialogKey<>("cinematic_scene_model_yaw");
    public static final DialogKey<CinematicProvider> SCENE_START_DELAY     = new DialogKey<>("cinematic_scene_start_delay");
    public static final DialogKey<CinematicProvider> SCENE_OPENING_DELAY   = new DialogKey<>("cinematic_scene_opening_delay");
    public static final DialogKey<CinematicProvider> SCENE_END_DELAY       = new DialogKey<>("cinematic_scene_end_delay");
}
