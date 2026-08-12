package su.nightexpress.excellentcrates.util.pos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Camera positions are stored as a single string, so parsing has to be exact — a dropped decimal
 * moves the camera, and a bad parse must not take a whole scene down.
 */
class WorldPointTest {

    @Test
    void roundTripPreservesEveryComponent() {
        WorldPoint point = new WorldPoint("world_nether", 128.5D, 64.25D, -33.125D, 91.5F, -12.75F);

        WorldPoint parsed = WorldPoint.deserialize(point.serialize());

        assertEquals(point, parsed);
        assertEquals("world_nether", parsed.getWorldName());
        assertEquals(128.5D, parsed.getX());
        assertEquals(64.25D, parsed.getY());
        assertEquals(-33.125D, parsed.getZ());
        assertEquals(91.5F, parsed.getYaw());
        assertEquals(-12.75F, parsed.getPitch());
    }

    /** The world name is last and unsplit, so a comma in it must survive. */
    @Test
    void worldNameWithCommasSurvives() {
        WorldPoint point = new WorldPoint("my,odd,world", 1D, 2D, 3D, 0F, 0F);

        assertEquals("my,odd,world", WorldPoint.deserialize(point.serialize()).getWorldName());
    }

    @Test
    void emptyPointIsFlaggedAsUnset() {
        assertTrue(WorldPoint.empty().isEmpty());
        assertTrue(WorldPoint.deserialize("").isEmpty());
        assertTrue(WorldPoint.deserialize(null).isEmpty());
    }

    /**
     * A captured position at the world origin is a real position, not "unset" — otherwise the editor
     * would claim the admin never set it.
     */
    @Test
    void originInARealWorldIsNotEmpty() {
        assertFalse(new WorldPoint("world", 0D, 0D, 0D, 0F, 0F).isEmpty());
    }

    @Test
    void malformedInputYieldsEmptyInsteadOfThrowing() {
        assertTrue(WorldPoint.deserialize("1,2,3").isEmpty(), "too few components");
        assertTrue(WorldPoint.deserialize("garbage").isEmpty());
        assertTrue(assertDoesNotThrow(() -> WorldPoint.deserialize("a,b,c,d,e,world")).getWorldName().equals("world"));
    }

    /** Unparseable numbers default to zero rather than aborting the load. */
    @Test
    void unparseableNumbersDefaultToZero() {
        WorldPoint point = WorldPoint.deserialize("a,b,c,d,e,world");

        assertEquals(0D, point.getX());
        assertEquals(0D, point.getY());
        assertEquals(0F, point.getYaw());
        assertFalse(point.isEmpty(), "a named world still counts as set");
    }

    @Test
    void copyIsEqualButDistinct() {
        WorldPoint point = new WorldPoint("world", 5D, 6D, 7D, 45F, 10F);
        WorldPoint copy = point.copy();

        assertEquals(point, copy);
        assertNotSame(point, copy);
        assertEquals(point.hashCode(), copy.hashCode());
    }
}
