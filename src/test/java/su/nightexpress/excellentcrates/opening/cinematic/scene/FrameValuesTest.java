package su.nightexpress.excellentcrates.opening.cinematic.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import su.nightexpress.excellentcrates.opening.cinematic.scene.impl.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Value clamping on frames.
 *
 * <p>These bounds exist so a typo in a dialog produces a visibly corrected value rather than a
 * silently broken scene — a negative duration would run the timeline backwards, a zero scale would
 * make the prop vanish with no obvious cause, and an out-of-range pitch is ignored by the client.
 */
class FrameValuesTest {

    @ParameterizedTest
    @EnumSource(FrameType.class)
    void negativeDurationIsClampedToZero(FrameType type) {
        CinematicFrame frame = type.create();

        frame.setDuration(-40L);

        assertEquals(0L, frame.getDuration(), type + " allowed a negative duration");
    }

    @ParameterizedTest
    @EnumSource(FrameType.class)
    void defaultFrameReportsItsOwnType(FrameType type) {
        assertEquals(type, type.create().getType());
    }

    @ParameterizedTest
    @EnumSource(FrameType.class)
    void everyFrameHasANonBlankSummary(FrameType type) {
        assertFalse(type.create().getSummary().isBlank(), type + " has a blank editor summary");
    }

    @Test
    void propScaleCannotCollapseToZero() {
        PropTransformFrame frame = (PropTransformFrame) PropTransformFrame.createDefault();

        frame.setScale(0D);
        assertTrue(frame.getScale() > 0D, "a zero scale would make the prop invisible");

        frame.setScale(-3D);
        assertTrue(frame.getScale() > 0D, "a negative scale would mirror the prop");

        frame.setScale(2.5D);
        assertEquals(2.5D, frame.getScale(), "a valid scale should pass through untouched");
    }

    @Test
    void soundPitchIsClampedToTheVanillaRange() {
        PlaySoundFrame frame = (PlaySoundFrame) PlaySoundFrame.createDefault();

        frame.setPitch(0.1F);
        assertEquals(0.5F, frame.getPitch());

        frame.setPitch(9.0F);
        assertEquals(2.0F, frame.getPitch());

        frame.setPitch(1.5F);
        assertEquals(1.5F, frame.getPitch());
    }

    @Test
    void soundVolumeCannotBeNegative() {
        PlaySoundFrame frame = (PlaySoundFrame) PlaySoundFrame.createDefault();

        frame.setVolume(-2.0F);
        assertEquals(0.0F, frame.getVolume());
    }

    @Test
    void particleCountAndSpeedCannotBeNegative() {
        ParticleFrame frame = (ParticleFrame) ParticleFrame.createDefault();

        frame.setCount(-10);
        assertEquals(0, frame.getCount());

        frame.setSpeed(-1.0D);
        assertEquals(0.0D, frame.getSpeed());
    }

    @Test
    void titleTimingsCannotBeNegative() {
        SendTitleFrame frame = (SendTitleFrame) SendTitleFrame.createDefault();

        frame.setFadeIn(-5);
        frame.setStay(-5);
        frame.setFadeOut(-5);

        assertEquals(0, frame.getFadeIn());
        assertEquals(0, frame.getStay());
        assertEquals(0, frame.getFadeOut());
    }

    /** Instant frames must default to zero duration so they can be stacked onto one moment. */
    @Test
    void instantFrameKindsDefaultToZeroDuration() {
        assertEquals(0L, FrameType.PLAY_SOUND.create().getDuration());
        assertEquals(0L, FrameType.SEND_TITLE.create().getDuration());
        assertEquals(0L, FrameType.PARTICLE.create().getDuration());
        assertEquals(0L, FrameType.TELEPORT_CAMERA.create().getDuration());
    }

    /** Timed frame kinds need a non-zero default, or a new frame would do nothing visible. */
    @Test
    void timedFrameKindsDefaultToANonZeroDuration() {
        assertTrue(FrameType.WAIT.create().getDuration() > 0L);
        assertTrue(FrameType.MOVE_CAMERA.create().getDuration() > 0L);
        assertTrue(FrameType.PROP_TRANSFORM.create().getDuration() > 0L);
        assertTrue(FrameType.SPAWN_REWARD.create().getDuration() > 0L);
    }

    @Test
    void tickFormattingReadsAsSeconds() {
        assertEquals("1.0s", CinematicFrame.formatTicks(20L));
        assertEquals("0.0s", CinematicFrame.formatTicks(0L));
        assertEquals("2.5s", CinematicFrame.formatTicks(50L));
    }

    @Test
    void unknownFrameTypeNameIsRejected() {
        assertNull(FrameType.readType(new org.bukkit.configuration.file.YamlConfiguration(), "missing"));
    }
}
