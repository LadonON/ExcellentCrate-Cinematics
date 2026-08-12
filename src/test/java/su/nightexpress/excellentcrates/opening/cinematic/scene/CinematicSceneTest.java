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

        CinematicScene loaded = roundTrip(scene);

        assertEquals("vault_reveal", loaded.getId());
        assertEquals("Vault Reveal", loaded.getName());
        assertEquals(scene.getStage(), loaded.getStage());
        assertEquals("simple_roll", loaded.getOpeningId());
        assertEquals(2.25D, loaded.getCameraHeight());
        assertEquals(scene.getCrateBlock(), loaded.getCrateBlock());
        assertTrue(loaded.hasCrateBlock());
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
}
