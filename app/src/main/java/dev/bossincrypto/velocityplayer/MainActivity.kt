package dev.bossincrypto.velocityplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Rect
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var playerView: PlayerView
    private lateinit var controls: View
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var fullscreen = false
    private var fill = false
    private val prefs by lazy { getSharedPreferences("playback", MODE_PRIVATE) }
    private val openVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::openUri) }
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { updatePip(player) }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.player_view); controls = findViewById(R.id.controls_scroll)
        findViewById<Button>(R.id.open).setOnClickListener { openVideo.launch(arrayOf("video/*")) }
        findViewById<Button>(R.id.rewind).setOnClickListener { seekBy(-10_000) }
        findViewById<Button>(R.id.forward).setOnClickListener { seekBy(10_000) }
        findViewById<Button>(R.id.speed).setOnClickListener { showSpeeds(it as Button) }
        findViewById<Button>(R.id.scale).setOnClickListener { toggleScale(it as Button) }
        findViewById<Button>(R.id.fullscreen).setOnClickListener { setFullscreen(!fullscreen) }
        findViewById<Button>(R.id.pip).setOnClickListener { enterPip() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreen) setFullscreen(false) else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        connect()
    }

    private fun connect() {
        val token = SessionToken(this, android.content.ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync().also { future ->
            future.addListener({ if (!isFinishing && !isDestroyed) { controller = future.get().also { c -> playerView.player = c; c.addListener(listener); c.setPlaybackSpeed(SpeedPolicy.normalize(prefs.getFloat("speed", 1f))); handleIntent(intent) } } }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun handleIntent(value: Intent?) { value?.data?.let(::openUri) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
    private fun openUri(uri: Uri) {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        controller?.apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); play() }
    }
    private fun seekBy(delta: Long) { controller?.let { it.seekTo(SeekPolicy.target(it.currentPosition, delta, it.duration.takeIf { d -> d > 0 } ?: Long.MAX_VALUE)) } }
    private fun showSpeeds(button: Button) {
        val values = floatArrayOf(.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,2.5f,3f,4f)
        val selected = values.indices.minByOrNull { kotlin.math.abs(values[it] - (controller?.playbackParameters?.speed ?: 1f)) } ?: 3
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.speed).setSingleChoiceItems(values.map { "${it}×" }.toTypedArray(), selected) { d, which ->
            val speed = SpeedPolicy.normalize(values[which]); controller?.setPlaybackSpeed(speed); prefs.edit().putFloat("speed", speed).apply(); button.text = getString(R.string.speed_value, speed); d.dismiss()
        }.show()
    }
    private fun toggleScale(button: Button) { fill = !fill; playerView.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT; button.setText(if(fill) R.string.fill else R.string.fit) }
    private fun setFullscreen(enabled: Boolean) { fullscreen = enabled; controls.visibility = if (enabled) View.GONE else View.VISIBLE; val i = WindowInsetsControllerCompat(window, window.decorView); if(enabled) { i.hide(WindowInsetsCompat.Type.systemBars()); i.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } else i.show(WindowInsetsCompat.Type.systemBars()) }
    private fun pipParams(autoEnter: Boolean = false): PictureInPictureParams {
        val rect = Rect().also(playerView::getGlobalVisibleRect)
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).setSourceRectHint(rect)
        if (Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(autoEnter)
        return builder.build()
    }
    private fun updatePip(player: Player) { if (Build.VERSION.SDK_INT >= 31) setPictureInPictureParams(pipParams(AutoPipPolicy.shouldEnable(player.isPlaying, player.videoSize.width > 0))) }
    private fun enterPip() { if (controller?.videoSize?.width != 0) enterPictureInPictureMode(pipParams()) }
    override fun onPictureInPictureModeChanged(inPip: Boolean, newConfig: Configuration) { super.onPictureInPictureModeChanged(inPip, newConfig); controls.visibility = if(inPip || fullscreen) View.GONE else View.VISIBLE; playerView.useController = !inPip }

    override fun onDestroy() { playerView.player = null; controller?.removeListener(listener); controller = null; controllerFuture?.let(MediaController::releaseFuture); controllerFuture = null; super.onDestroy() }
}
