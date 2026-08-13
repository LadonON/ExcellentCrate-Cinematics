package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.util.pos.WorldPos;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

/**
 * A cinematic scene: a stage location and which existing opening animation actually runs there.
 *
 * <p>The cinematic itself does not render anything or roll any reward. It teleports the player to
 * {@link #getStage()} and hands off to whichever opening the admin picked in {@link #getOpeningId()}
 * — {@code simple_roll}, {@code csgo}, or any other id under {@code openings/} — letting that
 * opening's own, already-built visuals and reward logic run exactly as they would if the player had
 * clicked a crate configured with that id directly. Once it finishes, the player is teleported back.
 *
 * <p>One scene corresponds to one file under {@code openings/cinematic/}, so a scene's id is its file
 * name and crates select it by that id — exactly how every other opening type is chosen.
 */
public class CinematicScene {

    public static final String KEY_NAME            = "Name";
    public static final String KEY_STAGE           = "Stage";
    public static final String KEY_OPENING_ID      = "Opening";
    public static final String KEY_CAMERA_HEIGHT   = "Camera_Height";
    public static final String KEY_CRATE_BLOCK     = "Crate_Block";
    public static final String KEY_MODEL_ID        = "Model";
    public static final String KEY_MODEL_ANIMATION = "Model_Animation";

    /** How far above the stage the locked camera sits when a scene does not say otherwise. */
    public static final double DEFAULT_CAMERA_HEIGHT = 1.7D;

    /** Which animation plays when the model prop spawns, when a scene does not say otherwise. */
    public static final String DEFAULT_MODEL_ANIMATION = "open";

    private final String id;

    private String     name;
    private WorldPoint stage;
    private String     openingId;
    private double     cameraHeight;
    private WorldPos   crateBlock;
    private String     modelId;
    private String     modelAnimation;

    public CinematicScene(@NotNull String id) {
        this.id = id.toLowerCase();
        this.name = id;
        this.stage = WorldPoint.empty();
        this.openingId = "";
        this.cameraHeight = DEFAULT_CAMERA_HEIGHT;
        this.crateBlock = WorldPos.empty();
        this.modelId = "";
        this.modelAnimation = DEFAULT_MODEL_ANIMATION;
    }

    @NotNull
    public static CinematicScene read(@NotNull ConfigurationSection config, @NotNull String id) {
        CinematicScene scene = new CinematicScene(id);

        scene.setName(config.getString(KEY_NAME, id));
        scene.setStage(WorldPoint.read(config, KEY_STAGE));
        scene.setOpeningId(config.getString(KEY_OPENING_ID, ""));
        scene.setCameraHeight(config.getDouble(KEY_CAMERA_HEIGHT, DEFAULT_CAMERA_HEIGHT));
        scene.setCrateBlock(WorldPos.deserialize(config.getString(KEY_CRATE_BLOCK, "")));
        scene.setModelId(config.getString(KEY_MODEL_ID, ""));
        scene.setModelAnimation(config.getString(KEY_MODEL_ANIMATION, DEFAULT_MODEL_ANIMATION));

        return scene;
    }

    public void write(@NotNull ConfigurationSection config) {
        config.set(KEY_NAME, this.name);
        this.stage.write(config, KEY_STAGE);
        config.set(KEY_OPENING_ID, this.openingId);
        config.set(KEY_CAMERA_HEIGHT, this.cameraHeight);
        config.set(KEY_CRATE_BLOCK, this.crateBlock.serialize());
        config.set(KEY_MODEL_ID, this.modelId);
        config.set(KEY_MODEL_ANIMATION, this.modelAnimation);
    }

    /**
     * A scene needs somewhere to send the player and something to run once they arrive.
     */
    public boolean isPlayable() {
        return !this.stage.isEmpty() && this.hasOpeningId();
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return where the player is teleported to while the delegate opening runs. This is "the
     * actual crate" from an admin's point of view — wherever they build the real reveal to happen,
     * decorated however they like.
     */
    @NotNull
    public WorldPoint getStage() {
        return this.stage;
    }

    public void setStage(@NotNull WorldPoint stage) {
        this.stage = stage;
    }

    /**
     * @return the id of the opening animation that actually runs at the stage — any existing
     * provider id such as {@code simple_roll} or {@code csgo}. Never another cinematic scene: that
     * would either loop back on itself or chain into another teleport, neither of which is
     * meaningful.
     */
    @NotNull
    public String getOpeningId() {
        return this.openingId;
    }

    public void setOpeningId(@NotNull String openingId) {
        this.openingId = openingId.toLowerCase();
    }

    public boolean hasOpeningId() {
        return !this.openingId.isBlank();
    }

    /**
     * @return how far above the stage the player's camera is locked while the delegate opening runs.
     */
    public double getCameraHeight() {
        return this.cameraHeight;
    }

    public void setCameraHeight(double cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    /**
     * @return the actual crate block at the stage, if the admin captured one by right-clicking it
     * with the capture tool instead of clicking air. Passed through to the delegate opening so
     * block-anchored visuals — Simple Roll's reward display, most notably — render on top of it
     * instead of floating in front of wherever the locked camera happens to face.
     */
    @NotNull
    public WorldPos getCrateBlock() {
        return this.crateBlock;
    }

    public void setCrateBlock(@NotNull WorldPos crateBlock) {
        this.crateBlock = crateBlock;
    }

    public boolean hasCrateBlock() {
        return !this.crateBlock.isEmpty();
    }

    /**
     * @return the ModelEngine blueprint id spawned at the stage's crate block when the hand-off
     * happens, or blank if this scene has no model prop. Optional - a scene works without one, this
     * only replaces whatever built-in reveal the delegate opening renders with a custom model, held
     * on its {@link #getModelAnimation()} animation until the delegate finishes.
     */
    @NotNull
    public String getModelId() {
        return this.modelId;
    }

    public void setModelId(@NotNull String modelId) {
        this.modelId = modelId;
    }

    public boolean hasModel() {
        return !this.modelId.isBlank();
    }

    /**
     * @return the animation played on the model prop the instant it spawns. Whether it holds on its
     * last frame afterwards, loops, or does anything else is entirely up to how that animation's
     * loop mode is authored in the blueprint - this plugin only starts it once.
     */
    @NotNull
    public String getModelAnimation() {
        return this.modelAnimation;
    }

    public void setModelAnimation(@NotNull String modelAnimation) {
        this.modelAnimation = modelAnimation;
    }
}
