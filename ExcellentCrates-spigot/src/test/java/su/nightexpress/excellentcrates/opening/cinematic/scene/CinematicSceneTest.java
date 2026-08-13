package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;
import su.nightexpress.excellentcrates.util.pos.WorldPos;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cinematic scene is a stage location, which existing opening id it delegates to, how far above
 * the stage the locked camera sits, and an optional crate block for block-anchored delegates to
 * render on top of. This covers serialization round-tripping all of that, playability requiring the
 * stage and opening id, and every field loading sensibly from a file written before it existed.
 */
class CinematicSceneTest {

    private static CinematicScene roundTrip(CinematicScene scene) {
        YamlConfiguration config = new YamlConfiguration();
        scene.write(config);

        YamlConfiguration reloaded = new YamlConfiguration();
        assertDoesNotThrow(() -> reloaded.loadFromString(config.saveToString()));

        return CinematicScene.read(reloaded, scene.getId());
    }

    @Test
    void sceneSurvivesRoundTrip() {
        CinematicScene scene = new CinematicScene("vault_reveal");
        scene.setName("Vault Reveal");
        scene.setStage(new WorldPoint("world", 10.5D, 64.0D, -20.25D, 90.0F, 15.0F));
        scene.setOpeningId("simple_roll");
        scene.setCameraHeight(2.25D);
        scene.setCrateBlock(new WorldPos("world", 10, 63, -20));
        scene.setOpeningDelay(20);
        scene.setEndDelay(60);

        CinematicScene loaded = roundTrip(scene);

        assertEquals("vault_reveal", loaded.getId());
        assertEquals("Vault Reveal", loaded.getName());
        assertEquals(scene.getStage(), loaded.getStage());
        assertEquals("simple_roll", loaded.getOpeningId());
        assertEquals(2.25D, loaded.getCameraHeight());
        assertEquals(scene.getCrateBlock(), loaded.getCrateBlock());
        assertTrue(loaded.hasCrateBlock());
        assertEquals(20, loaded.getOpeningDelay());
        assertEquals(60, loaded.getEndDelay());
    }

    /**
     * Scene files written while cinematics could still spawn a ModelEngine prop carry {@code Model},
     * {@code Model_Animation}, {@code Model_Yaw} and {@code Start_Delay} keys. They must load without
     * throwing, and writing the scene back must strip them rather than preserve dead options.
     */
    @Test
    void legacyModelKeysAreIgnoredAndStrippedOnWrite() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CinematicScene.KEY_NAME, "Legacy");
        config.set(CinematicScene.KEY_OPENING_ID, "simple_roll");
        config.set("Model", "common_crate");
        config.set("Model_Animation", "open");
        config.set("Model_Yaw", 135.0D);
        config.set("Start_Delay", 40);

        CinematicScene loaded = assertDoesNotThrow(() -> CinematicScene.read(config, "legacy"));
        assertEquals("simple_roll", loaded.getOpeningId());

        YamlConfiguration rewritten = new YamlConfiguration();
        loaded.write(rewritten);

        assertFalse(rewritten.contains("Model"));
        assertFalse(rewritten.contains("Model_Animation"));
        assertFalse(rewritten.contains("Model_Yaw"));
        assertFalse(rewritten.contains("Start_Delay"));
    }

    @Test
    void newSceneHasNoCrateBlock() {
        CinematicScene scene = new CinematicScene("vault");

        assertFalse(scene.hasCrateBlock());
    }

    /**
     * A scene file written before {@code Crate_Block} existed has no such key. It must still load
     * without throwing, landing on "no crate block set" rather than some garbage position.
     */
    @Test
    void sceneWithoutCrateBlockKeyHasNoCrateBlock() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CinematicScene.KEY_NAME, "Legacy");

        CinematicScene loaded = CinematicScene.read(config, "legacy");

        assertFalse(loaded.hasCrateBlock());
    }

    @Test
    void crateBlockCanBeCleared() {
        CinematicScene scene = new CinematicScene("vault");
        scene.setCrateBlock(new WorldPos("world", 10, 63, -20));
        assertTrue(scene.hasCrateBlock());

        scene.setCrateBlock(WorldPos.empty());

        assertFalse(scene.hasCrateBlock());
    }

    /**
     * A scene file written before {@code Camera_Height} existed has no such key. It must still load
     * without throwing, landing on the same default a brand-new scene starts with.
     */
    @Test
    void sceneWithoutCameraHeightKeyUsesDefault() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CinematicScene.KEY_NAME, "Legacy");

        CinematicScene loaded = CinematicScene.read(config, "legacy");

        assertEquals(CinematicScene.DEFAULT_CAMERA_HEIGHT, loaded.getCameraHeight());
    }

    @Test
    void newSceneUsesDefaultCameraHeight() {
        CinematicScene scene = new CinematicScene("vault");

        assertEquals(CinematicScene.DEFAULT_CAMERA_HEIGHT, scene.getCameraHeight());
        assertEquals(1.7D, scene.getCameraHeight());
    }

    /**
     * Ids are stored lowercase so a scene reliably matches a provider id regardless of how an admin
     * typed it in the editor's dialog.
     */
    @Test
    void openingIdIsLowercased() {
        CinematicScene scene = new CinematicScene("vault");
        scene.setOpeningId("CSGO");

        assertEquals("csgo", scene.getOpeningId());
    }

    @Test
    void newSceneIsNotPlayable() {
        CinematicScene scene = new CinematicScene("vault");

        assertFalse(scene.isPlayable());
        assertFalse(scene.hasOpeningId());
    }

    @Test
    void sceneNeedsBothStageAndOpeningIdToBePlayable() {
        CinematicScene stageOnly = new CinematicScene("vault");
        stageOnly.setStage(new WorldPoint("world", 0, 0, 0, 0, 0));
        assertFalse(stageOnly.isPlayable(), "a stage with no opening id should not be playable");

        CinematicScene openingOnly = new CinematicScene("vault");
        openingOnly.setOpeningId("simple_roll");
        assertFalse(openingOnly.isPlayable(), "an opening id with no stage should not be playable");

        CinematicScene both = new CinematicScene("vault");
        both.setStage(new WorldPoint("world", 0, 0, 0, 0, 0));
        both.setOpeningId("simple_roll");
        assertTrue(both.isPlayable());
    }

    /**
     * A scene file written before the {@code Opening} key existed has no such key. It must still
     * load without throwing, and correctly report itself as not yet playable.
     */
    @Test
    void sceneWithoutOpeningKeyStillLoads() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CinematicScene.KEY_NAME, "Legacy");

        CinematicScene loaded = CinematicScene.read(config, "legacy");

        assertFalse(loaded.hasOpeningId());
        assertFalse(loaded.isPlayable());
    }

    /**
     * A scene file written before {@code Opening_Delay} and {@code End_Delay} existed has neither
     * key. It must still load without throwing, landing on the same defaults a brand-new scene starts
     * with - so old scenes keep behaving exactly as they did before these settings existed.
     */
    @Test
    void sceneWithoutTimingKeysUsesDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CinematicScene.KEY_NAME, "Legacy");

        CinematicScene loaded = CinematicScene.read(config, "legacy");

        assertEquals(CinematicScene.DEFAULT_OPENING_DELAY, loaded.getOpeningDelay());
        assertEquals(CinematicScene.DEFAULT_END_DELAY, loaded.getEndDelay());
    }

    @Test
    void newSceneUsesDefaultTiming() {
        CinematicScene scene = new CinematicScene("vault");

        assertEquals(0, scene.getOpeningDelay());
        assertEquals(0, scene.getEndDelay());
    }

    /**
     * Negative delays make no sense as a wait time, so they are clamped to zero rather than allowed
     * to silently do nothing (or worse, be misread as "immediately" by some future caller that
     * doesn't expect a negative tick count).
     */
    @Test
    void negativeDelaysAreClampedToZero() {
        CinematicScene scene = new CinematicScene("vault");

        scene.setOpeningDelay(-5);
        scene.setEndDelay(-5);

        assertEquals(0, scene.getOpeningDelay());
        assertEquals(0, scene.getEndDelay());
    }
}
