package com.github.foxeiz

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.patcher.instead
import com.discord.rtcconnection.audio.DiscordAudioManager

@Suppress("unused", "DEPRECATION")
@AliucordPlugin
class A2DPSucks : Plugin() {

    init {
        settingsTab = SettingsTab(
            PluginSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(settings, patcher)
    }

    private fun killSco(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            audioManager.isBluetoothScoOn = false
        } else {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }
    }

    private fun requestAudioFocus(manager: DiscordAudioManager, audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mediaAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val focusListener = manager.p
            if (focusListener != null) {
                val handler = Handler(Looper.getMainLooper())
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mediaAttrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusListener, handler)
                    .build()
                manager.q = focusReq
                audioManager.requestAudioFocus(focusReq)
            }
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus(manager: DiscordAudioManager, audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val savedReq = manager.q
            if (savedReq != null) audioManager.abandonAudioFocusRequest(savedReq)
        } else {
            val listener = manager.p
            if (listener != null) audioManager.abandonAudioFocus(listener)
        }
    }

    private fun captureAudioState(manager: DiscordAudioManager, audioManager: AudioManager) {
        manager.A = audioManager.isSpeakerphoneOn
        manager.B = audioManager.isMicrophoneMute
        manager.C = audioManager.isBluetoothScoOn
    }

    private fun restoreAudioState(manager: DiscordAudioManager, audioManager: AudioManager) {
        if (audioManager.isMicrophoneMute != manager.B) audioManager.isMicrophoneMute = manager.B
        if (audioManager.isSpeakerphoneOn != manager.A) audioManager.isSpeakerphoneOn = manager.A
        if (manager.C) manager.j()
    }

    private fun applyDeviceSwitch(
        manager: DiscordAudioManager,
        audioManager: AudioManager,
        targetDevice: DiscordAudioManager.DeviceTypes
    ) {
        if (targetDevice == DiscordAudioManager.DeviceTypes.BLUETOOTH_HEADSET) {
            manager.i(false)
            killSco(audioManager)
            if (audioManager.isSpeakerphoneOn) audioManager.isSpeakerphoneOn = false
        } else if (targetDevice == DiscordAudioManager.DeviceTypes.SPEAKERPHONE) {
            killSco(audioManager)
            manager.k()
            manager.i(false)
            if (!audioManager.isSpeakerphoneOn) audioManager.isSpeakerphoneOn = true
        } else {
            manager.i(true)
            killSco(audioManager)
            manager.k()
            if (audioManager.isSpeakerphoneOn) audioManager.isSpeakerphoneOn = false
        }

        synchronized(manager.i) {
            manager.t = targetDevice
            manager.u.k.onNext(targetDevice)
        }

        try {
            Utils.appActivity.volumeControlStream = AudioManager.STREAM_MUSIC
        } catch (e: Throwable) {
        }
    }

    override fun start(context: Context) {
        try {
            patcher.before<AudioTrack>(
                AudioAttributes::class.java,
                AudioFormat::class.java,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!
            ) {
                val attrs = it.args[0] as AudioAttributes
                if (attrs.usage == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
                    val mediaAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setFlags(attrs.flags)
                        .build()
                    it.args[0] = mediaAttrs
                }
            }
        } catch (e: Throwable) {
            logger.error("Failed to hook AudioTrack constructor", e)
        }

        if (settings.getBool("compatibilityMode", false)) {
            patcher.instead<DiscordAudioManager>("h", Boolean::class.javaPrimitiveType!!) { null }
            patcher.instead<DiscordAudioManager>("j") { null }
        } else {
            patcher.instead<DiscordAudioManager>("h", Boolean::class.javaPrimitiveType!!) { param ->
                val manager = param.thisObject as DiscordAudioManager
                val turningOn = param.args[0] as Boolean
                val audioManager = manager.e

                try {
                    if (!manager.e().b) return@instead null
                } catch (e: Throwable) {
                }

                if (turningOn) {
                    captureAudioState(manager, audioManager)
                    manager.i(false)
                    requestAudioFocus(manager, audioManager)
                    manager.l()
                } else {
                    manager.v = null
                    manager.b(DiscordAudioManager.DeviceTypes.INVALID)
                    synchronized(manager.i) {
                        manager.z = DiscordAudioManager.DeviceTypes.DEFAULT
                    }
                    manager.i(false)
                    abandonAudioFocus(manager, audioManager)
                    restoreAudioState(manager, audioManager)
                }
                return@instead null
            }

            patcher.before<DiscordAudioManager>(
                "b",
                DiscordAudioManager.DeviceTypes::class.java
            ) {
                val manager = it.thisObject as DiscordAudioManager
                val targetDevice = it.args[0] as DiscordAudioManager.DeviceTypes
                val audioManager = manager.e

                applyDeviceSwitch(manager, audioManager, targetDevice)

                it.result = null
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
