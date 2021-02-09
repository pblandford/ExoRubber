package com.example.rubberbanddemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import com.example.rubberbanddemo.databinding.ActivityRubberBandDemoBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer

class RubberBandDemoActivity : AppCompatActivity() {


    private lateinit var binding:ActivityRubberBandDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRubberBandDemoBinding.inflate(layoutInflater)
        initExplayer()
        initSeekBars()
        setContentView(binding.root)
    }


    private fun initExplayer() {
        val player = SimpleExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri("file:///android_asset/PreludeCm.mp3")
        player.setMediaItem(mediaItem)
        player.prepare()
        binding.playerView.player = player
    }

    private fun initSeekBars() {
        binding.speedSeekBar.init(100, 50 ) { p ->
            val speed = p.toFloat() / 50
            PlaybackParameters(speed, this.pitch)
        }
        binding.pitchSeekBar.init(100, 50 ) { p ->
            val pitch = p.toFloat() / 50
            PlaybackParameters(this.speed, pitch)
        }
    }

    private fun SeekBar.init(max:Int, start:Int, set: PlaybackParameters.(Int)->PlaybackParameters) {
        this.max = max
        this.progress = start
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.playerView.player?.let { player ->
                    val parameters = player.playbackParameters.set(progress)
                    player.setPlaybackParameters(parameters)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }
}