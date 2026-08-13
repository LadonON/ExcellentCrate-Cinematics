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
}
