package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.MoveCameraFrame;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.WaitFrame;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trips scenes through YAML.
 *
 * <p>Frames are written against {@link org.bukkit.configuration.ConfigurationSection} rather than
 * nightcore's {@code FileConfig}, so a plain {@link YamlConfiguration} is enough to exercise the
 * real serialization code without a running server.
 */
class SceneSerializationTest {

    /** Serializes a scene and reads it straight back, as loading the file again would. */
    private static CinematicScene roundTrip(CinematicScene scene) {
        YamlConfiguration config = new YamlConfiguration();
        scene.write(config);

        YamlConfiguration reloaded = new YamlConfiguration();
        assertDoesNotThrow(() -> reloaded.loadFromString(config.saveToString()));

        return CinematicScene.read(reloaded, scene.getId());
    }

    @Test
    void sceneMetadataSurvivesRoundTrip() {
        CinematicScene scene = new CinematicScene("vault_reveal");
        scene.setName("Vault Reveal");
        scene.setPropModel("crate_model_1");
        scene.setPlayerPoint(new WorldPoint("world", 10.5D, 64.0D, -20.25D, 90.0F, 15.0F));
        scene.setRewardOffsetY(2.5D);

        CinematicScene loaded = roundTrip(scene);

        assertEquals("vault_reveal", loaded.getId());
        assertEquals("Vault Reveal", loaded.getName());
        assertEquals("crate_model_1", loaded.getPropModel());
        assertEquals(scene.getPlayerPoint(), loaded.getPlayerPoint());
        assertEquals(2.5D, loaded.getRewardOffsetY());
    }

    @Test
    void frameOrderAndValuesSurviveRoundTrip() {
        CinematicScene scene = new CinematicScene("demo");
        scene.addFrame(new WaitFrame(15L));
        scene.addFrame(new MoveCameraFrame(60L, new WorldPoint("world", 1D, 2D, 3D, 45F, -10F), Easing.EASE_OUT));
        scene.addFrame(new WaitFrame(5L));

        CinematicScene loaded = roundTrip(scene);

        assertEquals(3, loaded.countFrames());
        assertEquals(FrameType.WAIT, loaded.getFrames().get(0).getType());
        assertEquals(FrameType.MOVE_CAMERA, loaded.getFrames().get(1).getType());
        assertEquals(FrameType.WAIT, loaded.getFrames().get(2).getType());

        MoveCameraFrame move = (MoveCameraFrame) loaded.getFrames().get(1);
        assertEquals(60L, move.getDuration());
        assertEquals(Easing.EASE_OUT, move.getEasing());
        assertEquals(new WorldPoint("world", 1D, 2D, 3D, 45F, -10F), move.getTarget());
    }

    /**
     * Ten or more frames is where naive string sorting would put "10" before "2".
     */
    @Test
    void frameOrderIsNumericNotLexicographic() {
        CinematicScene scene = new CinematicScene("long");
        for (int i = 0; i < 12; i++) {
            scene.addFrame(new WaitFrame(i));
        }

        CinematicScene loaded = roundTrip(scene);

        assertEquals(12, loaded.countFrames());
        for (int i = 0; i < 12; i++) {
            assertEquals(i, loaded.getFrames().get(i).getDuration(), "frame at index " + i);
        }
    }

    /** A frame with an unknown type is dropped; the rest of the scene still loads. */
    @Test
    void unknownFrameTypeIsSkipped() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
            Name: Partial
            Frames:
              '0':
                Type: WAIT
                Duration: 10
              '1':
                Type: SOME_FUTURE_FRAME
                Duration: 10
              '2':
                Type: WAIT
                Duration: 30
            """);

        CinematicScene scene = CinematicScene.read(config, "partial");

        assertEquals(2, scene.countFrames());
        assertEquals(40L, scene.getTotalDuration());
    }

    /** Every frame type must survive its own round trip and keep its defaults readable. */
    @Test
    void everyFrameTypeRoundTrips() {
        for (FrameType type : FrameType.values()) {
            CinematicScene scene = new CinematicScene("t");
            scene.addFrame(type.create());

            CinematicScene loaded = roundTrip(scene);

            assertEquals(1, loaded.countFrames(), type + " produced no frame");
            assertEquals(type, loaded.getFrames().get(0).getType(), type + " changed type");
        }
    }
}
