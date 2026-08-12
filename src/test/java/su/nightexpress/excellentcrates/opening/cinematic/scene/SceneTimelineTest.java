package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.junit.jupiter.api.Test;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.WaitFrame;
import su.nightexpress.excellentcrates.util.pos.WorldPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Timeline bookkeeping: frame order is the timeline, so reordering and removal have to be exact.
 */
class SceneTimelineTest {

    private static CinematicScene sceneWithDurations(long... durations) {
        CinematicScene scene = new CinematicScene("test");
        for (long duration : durations) {
            scene.addFrame(new WaitFrame(duration));
        }
        return scene;
    }

    private static List<Long> durationsOf(CinematicScene scene) {
        return scene.getFrames().stream().map(CinematicFrame::getDuration).toList();
    }

    @Test
    void totalDurationIsTheSumOfFrames() {
        assertEquals(0L, sceneWithDurations().getTotalDuration());
        assertEquals(75L, sceneWithDurations(20L, 40L, 15L).getTotalDuration());
    }

    @Test
    void idIsLowercasedSoLookupsMatchTheFileName() {
        assertEquals("vault_reveal", new CinematicScene("Vault_Reveal").getId());
    }

    @Test
    void movingAFrameSwapsItWithItsNeighbour() {
        CinematicScene scene = sceneWithDurations(1L, 2L, 3L);

        assertTrue(scene.moveFrame(2, -1));
        assertEquals(List.of(1L, 3L, 2L), durationsOf(scene));

        assertTrue(scene.moveFrame(0, 1));
        assertEquals(List.of(3L, 1L, 2L), durationsOf(scene));
    }

    /** Holding shift-click at either end of the list should simply stop, not error or wrap. */
    @Test
    void movingPastEitherEndIsANoOp() {
        CinematicScene scene = sceneWithDurations(1L, 2L, 3L);

        assertFalse(scene.moveFrame(0, -1), "first frame cannot move up");
        assertFalse(scene.moveFrame(2, 1), "last frame cannot move down");
        assertFalse(scene.moveFrame(1, 0), "a zero move changes nothing");
        assertEquals(List.of(1L, 2L, 3L), durationsOf(scene));
    }

    @Test
    void movingAnOutOfRangeIndexIsRejected() {
        CinematicScene scene = sceneWithDurations(1L, 2L);

        assertFalse(scene.moveFrame(-1, 1));
        assertFalse(scene.moveFrame(5, -1));
        assertEquals(List.of(1L, 2L), durationsOf(scene));
    }

    @Test
    void removingAFrameShiftsTheRestDown() {
        CinematicScene scene = sceneWithDurations(1L, 2L, 3L);

        assertTrue(scene.removeFrame(1));
        assertEquals(List.of(1L, 3L), durationsOf(scene));
        assertEquals(2, scene.countFrames());
    }

    @Test
    void removingAnOutOfRangeIndexIsRejected() {
        CinematicScene scene = sceneWithDurations(1L);

        assertFalse(scene.removeFrame(-1));
        assertFalse(scene.removeFrame(1));
        assertEquals(1, scene.countFrames());
    }

    @Test
    void getFrameReturnsNullOutsideTheTimeline() {
        CinematicScene scene = sceneWithDurations(1L);

        assertNotNull(scene.getFrame(0));
        assertNull(scene.getFrame(-1));
        assertNull(scene.getFrame(1));
    }

    /**
     * A scene needs a viewer position and at least one frame. Anything less and
     * {@code CinematicOpening} grants the reward without animation rather than stranding the player.
     */
    @Test
    void playabilityNeedsBothAViewerPositionAndFrames() {
        CinematicScene scene = new CinematicScene("test");
        assertFalse(scene.isPlayable(), "empty scene");

        scene.addFrame(new WaitFrame(20L));
        assertFalse(scene.isPlayable(), "frames but no viewer position");

        scene.setPlayerPoint(new WorldPoint("world", 0D, 64D, 0D, 0F, 0F));
        assertTrue(scene.isPlayable());

        scene.removeFrame(0);
        assertFalse(scene.isPlayable(), "viewer position but no frames");
    }

    @Test
    void propModelIsOptionalAndBlankCountsAsUnset() {
        CinematicScene scene = new CinematicScene("test");
        assertFalse(scene.hasPropModel());

        scene.setPropModel("   ");
        assertFalse(scene.hasPropModel(), "whitespace should not count as a model");

        scene.setPropModel("crate_model_1");
        assertTrue(scene.hasPropModel());
    }
}
