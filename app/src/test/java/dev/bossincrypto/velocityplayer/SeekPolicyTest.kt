package dev.bossincrypto.velocityplayer
import org.junit.Assert.assertEquals
import org.junit.Test
class SeekPolicyTest { @Test fun clampsTargetsToMediaBounds() { assertEquals(0L, SeekPolicy.target(2_000, -10_000, 60_000)); assertEquals(60_000L, SeekPolicy.target(55_000, 10_000, 60_000)); assertEquals(30_000L, SeekPolicy.target(20_000, 10_000, 60_000)) } }