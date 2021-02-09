package com.example.rubberbanddemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.example.rubberbanddemo.databinding.ActivityRubberBandDemoBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RubberBandDemoActivity : AppCompatActivity() {


    private lateinit var binding: ActivityRubberBandDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRubberBandDemoBinding.inflate(layoutInflater)
        initExplayer(binding.playerView1, binding.speedSeekBar1, binding.pitchSeekBar1, "PreludeCm.mp3")
        initExplayer(binding.playerView2, binding.speedSeekBar2, binding.pitchSeekBar2, "SchumannOp26.mp3")
        initExplayer(binding.playerView3, binding.speedSeekBar3, binding.pitchSeekBar3, "Etude.mp3")
        setContentView(binding.root)
    }


    private fun initExplayer(playerView: StyledPlayerView, speedSeek: SeekBar, pitchSeek: SeekBar, file: String) {
        val player = SimpleExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri("file:///android_asset/${file}")
        player.setMediaItem(mediaItem)
        player.prepare()
        playerView.player = player
        speedSeek.init(100, 50, player) { p ->
            val speed = p.toFloat() / 50
            PlaybackParameters(speed, this.pitch)
        }
        pitchSeek.init(100, 50, player) { p ->
            val pitch = p.toFloat() / 50
            PlaybackParameters(this.speed, pitch)
        }
    }


    private fun SeekBar.init(max: Int, start: Int, player: Player?, update: PlaybackParameters.(Int) -> PlaybackParameters) {
        this.max = max
        this.progress = start
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                player?.let { player ->
                    val parameters = player.playbackParameters.update(progress)
                    lifecycleScope.launch {
                        withContext(Dispatchers.Default) {
                            player.setPlaybackParameters(parameters)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}