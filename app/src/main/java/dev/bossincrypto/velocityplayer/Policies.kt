package dev.bossincrypto.velocityplayer
import kotlin.math.round
object SpeedPolicy { fun normalize(value: Float): Float = if (!value.isFinite()) 1f else (round(value.coerceIn(.25f, 4f) * 4f) / 4f) }
object SeekPolicy { fun target(positionMs: Long, deltaMs: Long, durationMs: Long): Long = (positionMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L)) }
object AutoPipPolicy { fun shouldEnable(isPlaying: Boolean, hasVideo: Boolean) = isPlaying && hasVideo }
