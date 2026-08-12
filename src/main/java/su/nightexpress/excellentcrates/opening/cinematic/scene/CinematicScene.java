package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A complete cinematic: where the viewer stands, where the crate prop sits, and the ordered list of
 * frames that make up the timeline.
 *
 * <p>One scene corresponds to one file under {@code openings/cinematic/}, so a scene's id is its
 * file name and crates select it by that id — exactly how the Inventory, Simple Roll and Selectable
 * openings are chosen today.
 */
public class CinematicScene {

    public static final String KEY_NAME            = "Name";
    public static final String KEY_PLAYER_LOCATION = "Player_Location";
    public static final String KEY_PROP_LOCATION   = "Prop.Location";
    public static final String KEY_PROP_MODEL      = "Prop.Model";
    public static final String KEY_REWARD_OFFSET_X = "Reward_Offset.X";
    public static final String KEY_REWARD_OFFSET_Y = "Reward_Offset.Y";
    public static final String KEY_REWARD_OFFSET_Z = "Reward_Offset.Z";
    public static final String KEY_FRAMES          = "Frames";

    private static final double DEFAULT_REWARD_OFFSET_Y = 1.0D;

    private final String                id;
    private final List<CinematicFrame>  frames;

    private String     name;
    private WorldPoint playerPoint;
    private WorldPoint propPoint;
    private String     propModel;
    private double     rewardOffsetX, rewardOffsetY, rewardOffsetZ;

    public CinematicScene(@NotNull String id) {
        this.id = id.toLowerCase();
        this.frames = new ArrayList<>();
        this.name = id;
        this.playerPoint = WorldPoint.empty();
        this.propPoint = WorldPoint.empty();
        this.propModel = "";
        this.rewardOffsetY = DEFAULT_REWARD_OFFSET_Y;
    }

    /**
     * Loads a scene, skipping any frame whose {@code Type} is missing or unrecognised so that a
     * single bad entry cannot take the whole scene — or the crate using it — offline.
     */
    @NotNull
    public static CinematicScene read(@NotNull ConfigurationSection config, @NotNull String id) {
        CinematicScene scene = new CinematicScene(id);

        scene.setName(config.getString(KEY_NAME, id));
        scene.setPlayerPoint(WorldPoint.read(config, KEY_PLAYER_LOCATION));
        scene.setPropPoint(WorldPoint.read(config, KEY_PROP_LOCATION));
        scene.setPropModel(config.getString(KEY_PROP_MODEL, ""));
        scene.setRewardOffsetX(config.getDouble(KEY_REWARD_OFFSET_X, 0D));
        scene.setRewardOffsetY(config.getDouble(KEY_REWARD_OFFSET_Y, DEFAULT_REWARD_OFFSET_Y));
        scene.setRewardOffsetZ(config.getDouble(KEY_REWARD_OFFSET_Z, 0D));

        ConfigurationSection framesSection = config.getConfigurationSection(KEY_FRAMES);
        if (framesSection != null) {
            // Frame keys are numeric indexes. getKeys() has no ordering guarantee, so sort them
            // numerically - otherwise "10" would sort before "2" and the timeline would scramble.
            List<String> keys = new ArrayList<>(framesSection.getKeys(false));
            keys.sort(Comparator.comparingInt(CinematicScene::parseIndex));

            for (String key : keys) {
                FrameType type = FrameType.readType(framesSection, key);
                if (type == null) continue;

                scene.frames.add(type.read(framesSection, key));
            }
        }

        return scene;
    }

    public void write(@NotNull ConfigurationSection config) {
        config.set(KEY_NAME, this.name);
        this.playerPoint.write(config, KEY_PLAYER_LOCATION);
        this.propPoint.write(config, KEY_PROP_LOCATION);
        config.set(KEY_PROP_MODEL, this.propModel);
        config.set(KEY_REWARD_OFFSET_X, this.rewardOffsetX);
        config.set(KEY_REWARD_OFFSET_Y, this.rewardOffsetY);
        config.set(KEY_REWARD_OFFSET_Z, this.rewardOffsetZ);

        // Drop the whole section first: frames may have been deleted or reordered, and leftover
        // keys from the previous layout would be read back as extra frames.
        config.set(KEY_FRAMES, null);

        ConfigurationSection framesSection = config.createSection(KEY_FRAMES);
        for (int index = 0; index < this.frames.size(); index++) {
            this.frames.get(index).write(framesSection, String.valueOf(index));
        }
    }

    /**
     * Parses a frame key into a sort index. Non-numeric keys from a hand-edited file sort last
     * rather than throwing.
     */
    private static int parseIndex(@NotNull String key) {
        try {
            return Integer.parseInt(key);
        }
        catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * @return the combined length of every frame, in ticks. This is how long a full playthrough
     * takes and what the editor shows as the scene's runtime.
     */
    public long getTotalDuration() {
        long total = 0L;
        for (CinematicFrame frame : this.frames) {
            total += frame.getDuration();
        }
        return total;
    }

    /**
     * A scene needs somewhere to put the viewer and at least one frame to be worth running.
     */
    public boolean isPlayable() {
        return !this.playerPoint.isEmpty() && !this.frames.isEmpty();
    }

    public void addFrame(@NotNull CinematicFrame frame) {
        this.frames.add(frame);
    }

    @Nullable
    public CinematicFrame getFrame(int index) {
        return index < 0 || index >= this.frames.size() ? null : this.frames.get(index);
    }

    /**
     * @return {@code true} if a frame was removed.
     */
    public boolean removeFrame(int index) {
        if (index < 0 || index >= this.frames.size()) return false;

        this.frames.remove(index);
        return true;
    }

    /**
     * Shifts a frame through the timeline by {@code delta} positions.
     *
     * <p>Vanilla inventories have no drag-and-drop, so the editor reorders with shift-click
     * (up) and shift-right-click (down), both of which land here.
     *
     * @return {@code true} if the frame moved. Moving the first frame up or the last frame down is
     * a no-op rather than an error, so holding shift-click at either end simply stops.
     */
    public boolean moveFrame(int index, int delta) {
        int target = index + delta;
        if (index < 0 || index >= this.frames.size()) return false;
        if (target < 0 || target >= this.frames.size()) return false;
        if (delta == 0) return false;

        Collections.swap(this.frames, index, target);
        return true;
    }

    public int countFrames() {
        return this.frames.size();
    }

    /**
     * @return the live, ordered frame list. Its order <i>is</i> the timeline order.
     */
    @NotNull
    public List<CinematicFrame> getFrames() {
        return this.frames;
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
     * @return where the viewer's body is parked for the duration of the cinematic.
     */
    @NotNull
    public WorldPoint getPlayerPoint() {
        return this.playerPoint;
    }

    public void setPlayerPoint(@NotNull WorldPoint playerPoint) {
        this.playerPoint = playerPoint;
    }

    /**
     * @return where the crate prop is spawned. Falls back to the player point when unset.
     */
    @NotNull
    public WorldPoint getPropPoint() {
        return this.propPoint;
    }

    public void setPropPoint(@NotNull WorldPoint propPoint) {
        this.propPoint = propPoint;
    }

    /**
     * @return the Nexo block or furniture id used for the prop. Empty means "use the vanilla
     * display-entity fallback", which is also what happens when Nexo is not installed.
     */
    @NotNull
    public String getPropModel() {
        return this.propModel;
    }

    public void setPropModel(@NotNull String propModel) {
        this.propModel = propModel;
    }

    public boolean hasPropModel() {
        return !this.propModel.isBlank();
    }

    public double getRewardOffsetX() {
        return this.rewardOffsetX;
    }

    public void setRewardOffsetX(double rewardOffsetX) {
        this.rewardOffsetX = rewardOffsetX;
    }

    public double getRewardOffsetY() {
        return this.rewardOffsetY;
    }

    public void setRewardOffsetY(double rewardOffsetY) {
        this.rewardOffsetY = rewardOffsetY;
    }

    public double getRewardOffsetZ() {
        return this.rewardOffsetZ;
    }

    public void setRewardOffsetZ(double rewardOffsetZ) {
        this.rewardOffsetZ = rewardOffsetZ;
    }
}
