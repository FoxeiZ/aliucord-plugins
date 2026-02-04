package com.github.foxeiz

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.patcher.instead
import com.aliucord.utils.ReflectUtils
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

    // Helper for reflection to keep code clean
    private fun invokePrivate(instance: Any, methodName: String, vararg args: Any?): Any? {
        val method = instance.javaClass.declaredMethods.firstOrNull { it.name == methodName }
        method?.isAccessible = true
        return method?.invoke(instance, *args)
    }

    override fun start(context: Context) {
        logger.info("Starting A2DPSucks plugin")

        if (settings.getBool("compatibilityMode", false)) {
            logger.warn("Compatibility Mode enabled")
            patcher.instead<DiscordAudioManager>("h", Boolean::class.javaPrimitiveType!!) { null }
            patcher.instead<DiscordAudioManager>("j") { null }
        } else {
            logger.info("Standard Mode enabled")

            patcher.instead<DiscordAudioManager>("h", Boolean::class.javaPrimitiveType!!) { param ->
                val manager = param.thisObject as DiscordAudioManager
                val turningOn = param.args[0] as Boolean
                val audioManager = manager.e

                try {
                    val hasPerms = invokePrivate(manager, "e")?.let {
                        ReflectUtils.getField(
                            it,
                            "d"
                        ) as Boolean
                    }
                    hasPerms?.let {
                        if (!it) {
                            logger.warn("Missing permissions, skipping h()")
                            return@instead null
                        }
                    }

                    if (turningOn) {
                        logger.info("h(true) called. Hijacking to force MEDIA setup.")

                        val looper = Looper.myLooper()
                        val handler =
                            if (looper != null) Handler(looper) else Handler(Looper.getMainLooper())

                        ReflectUtils.setField(manager, "A", audioManager.isSpeakerphoneOn)
                        ReflectUtils.setField(manager, "B", audioManager.isMicrophoneMute)
                        ReflectUtils.setField(manager, "C", audioManager.isBluetoothScoOn)

                        manager.i(false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val mediaAttrs = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()

                            val focusListener = ReflectUtils.getField(
                                manager,
                                "p"
                            ) as? AudioManager.OnAudioFocusChangeListener

                            if (focusListener != null) {
                                val focusReq =
                                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                        .setAudioAttributes(mediaAttrs)
                                        .setAcceptsDelayedFocusGain(true)
                                        .setOnAudioFocusChangeListener(focusListener, handler)
                                        .build()

                                ReflectUtils.setField(manager, "q", focusReq)
                                audioManager.requestAudioFocus(focusReq)
                                logger.debug("Requested AudioFocus: USAGE_MEDIA")
                            }
                        } else {

                            audioManager.requestAudioFocus(
                                null,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN
                            )
                        }
                        invokePrivate(manager, "l")
                    } else {
                        logger.info("h(false) called. Cleaning up.")

                        ReflectUtils.setField(manager, "v", null)

                        val bMethod = manager.javaClass.declaredMethods.first { it.name == "b" }
                        bMethod.isAccessible = true
                        bMethod.invoke(manager, DiscordAudioManager.DeviceTypes.INVALID)

                        synchronized(manager.i) {
                            ReflectUtils.setField(
                                manager,
                                "z",
                                DiscordAudioManager.DeviceTypes.DEFAULT
                            )
                        }
                        manager.i(false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val savedReq = ReflectUtils.getField(manager, "q") as? AudioFocusRequest
                            if (savedReq != null) {
                                audioManager.abandonAudioFocusRequest(savedReq)
                            }
                        } else {
                            val listener = ReflectUtils.getField(
                                manager,
                                "p"
                            ) as? AudioManager.OnAudioFocusChangeListener
                            if (listener != null) audioManager.abandonAudioFocus(listener)
                        }

                        val savedMic = ReflectUtils.getField(manager, "B") as Boolean
                        val savedSpeaker = ReflectUtils.getField(manager, "A") as Boolean
                        val savedSco = ReflectUtils.getField(manager, "C") as Boolean

                        if (audioManager.isMicrophoneMute != savedMic) audioManager.isMicrophoneMute =
                            savedMic
                        if (audioManager.isSpeakerphoneOn != savedSpeaker) audioManager.isSpeakerphoneOn =
                            savedSpeaker

                        if (savedSco) {
                            invokePrivate(manager, "j")
                        }
                    }
                } catch (t: Throwable) {
                    logger.error("Error hijacking h()", t)
                }
                return@instead null
            }

            patcher.before<DiscordAudioManager>(
                "b",
                DiscordAudioManager.DeviceTypes::class.java
            ) {
                val manager = it.thisObject as DiscordAudioManager
                val targetDevice = it.args[0] as DiscordAudioManager.DeviceTypes

                if (targetDevice == DiscordAudioManager.DeviceTypes.BLUETOOTH_HEADSET ||
                    targetDevice == DiscordAudioManager.DeviceTypes.SPEAKERPHONE
                ) {

                    logger.info("High Quality Device ($targetDevice). Enforcing Media logic.")

                    manager.i(false)
                    killSco(manager.e)

                    if (targetDevice != DiscordAudioManager.DeviceTypes.BLUETOOTH_HEADSET) {
                        try {
                            invokePrivate(manager, "k")
                        } catch (e: Throwable) {
                        }
                    }

                    val isSpeaker = (targetDevice == DiscordAudioManager.DeviceTypes.SPEAKERPHONE)
                    if (manager.e.isSpeakerphoneOn != isSpeaker) {
                        manager.e.isSpeakerphoneOn = isSpeaker
                    }

                    synchronized(manager.i) {
                        manager.t = targetDevice
                        manager.u.k.onNext(targetDevice)
                    }

                    try {
                        Utils.appActivity.volumeControlStream = AudioManager.STREAM_MUSIC
                    } catch (e: Throwable) {
                    }

                    it.result = null
                } else {
                    logger.info("Standard Device ($targetDevice). Enforcing Communication.")
                    killSco(manager.e)
                    manager.i(true)
                    return@before
                }
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
