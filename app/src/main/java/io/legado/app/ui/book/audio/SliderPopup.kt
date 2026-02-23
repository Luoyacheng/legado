package io.legado.app.ui.book.audio

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.databinding.PopupSeekBarBinding
import io.legado.app.model.AudioPlay
import io.legado.app.service.AudioPlayService
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import kotlin.math.roundToInt

class SliderPopup(private val context: Context, private val name: Int) :
    PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
    companion object {
        const val TIMER = 1
        const val SPEED = 2
    }

    private val binding = PopupSeekBarBinding.inflate(LayoutInflater.from(context))
    init {
        contentView = binding.root
        isTouchable = true
        isOutsideTouchable = false
        isFocusable = true
        setProcess()
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (name == TIMER) {
                    setProcessTimerText(progress)
                    if (fromUser) {
                        AudioPlay.setTimer(progress)
                    }
                    return
                }
                val speed = (progress / 10f).roundToInt() / 10f
                setProcessSpeedText(speed)
                if (fromUser) {
                    // 设置播放速度 (转换为0.5-2.0范围)
                    AudioPlay.setSpeed(speed)
                }
            }
        })
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        if (name == TIMER) {
            binding.seekBar.progress = AudioPlayService.timeMinute
        }
        else {
            binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
        }
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        if (name == TIMER) {
            binding.seekBar.progress = AudioPlayService.timeMinute
        }
        else {
            binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
        }
    }

    private fun setProcessTimerText(process: Int) {
        binding.tvSeekValue.text = context.getString(R.string.timer_m, process)
    }

    @SuppressLint("SetTextI18n")
    private fun setProcessSpeedText(speed: Float) {
        binding.tvSeekValue.text = "%.1fX".format(speed)
    }

    private fun setProcess() {
        if (name == TIMER) {
            binding.seekBar.max = 180
            setProcessTimerText(0)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.seekBar.min = 50
        }
        binding.seekBar.max = 200
        binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
        setProcessSpeedText(AudioPlayService.playSpeed)
    }
}