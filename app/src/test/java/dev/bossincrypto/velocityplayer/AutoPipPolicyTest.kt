package dev.bossincrypto.velocityplayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class AutoPipPolicyTest { @Test fun entersOnlyForActiveVideo() { assertTrue(AutoPipPolicy.shouldEnable(true, true)); assertFalse(AutoPipPolicy.shouldEnable(false, true)); assertFalse(AutoPipPolicy.shouldEnable(true, false)) } }