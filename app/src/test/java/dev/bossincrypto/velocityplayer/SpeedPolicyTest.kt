package dev.bossincrypto.velocityplayer
import org.junit.Assert.assertEquals
import org.junit.Test
class SpeedPolicyTest {
 @Test fun clampsAndRoundsSpeed() { assertEquals(0.25f, SpeedPolicy.normalize(0.1f)); assertEquals(4f, SpeedPolicy.normalize(8f)); assertEquals(1.25f, SpeedPolicy.normalize(1.26f)) }
 @Test fun rejectsNonFiniteSpeed() { assertEquals(1f, SpeedPolicy.normalize(Float.NaN)); assertEquals(1f, SpeedPolicy.normalize(Float.POSITIVE_INFINITY)) }
}