package com.github.foxeiz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.views.calls.AppVideoStreamRenderer
import org.webrtc.SurfaceViewRenderer
import kotlin.math.abs

@Suppress("DEPRECATION", "unused")
@SuppressLint("UseKtx", "ClickableViewAccessibility", "SetTextI18n")
@RequiresApi(Build.VERSION_CODES.M)
@AliucordPlugin
class SelfPip : Plugin() {
    private var targetRenderer: AppVideoStreamRenderer? = null
    private var originalParent: ViewGroup? = null
    private var originalLayoutParams: ViewGroup.LayoutParams? = null
    private var floatContainer: FrameLayout? = null
    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var isPiPActive = false
    private var scaleGestureDetector: ScaleGestureDetector? = null

    private var hideControlsRunnable: Runnable? = null

    private fun detachRenderer(renderer: AppVideoStreamRenderer): Boolean {
        return try {
            originalParent = renderer.parent as? ViewGroup
            originalLayoutParams = renderer.layoutParams
            originalParent?.removeView(renderer)
            logger.debug("renderer detached and layout saved")
            true
        } catch (e: Throwable) {
            logger.error("could not detach renderer", e)
            false
        }
    }

    private fun restoreRendererToParent() {
        val renderer = targetRenderer ?: return
        val parent = originalParent ?: return

        Utils.mainThread.post {
            try {
                if (renderer.parent == null) {
                    logger.debug("restoring renderer to original parent")
                    if (originalLayoutParams != null) {
                        renderer.layoutParams = originalLayoutParams
                    }
                    parent.addView(renderer)
                    parent.requestLayout()
                    parent.invalidate()
                }
            } catch (e: Throwable) {
                logger.error("could not restore renderer", e)
            }
        }
    }

    private fun updateFloatingLayout(params: WindowManager.LayoutParams) {
        floatContainer?.let { container ->
            windowManager?.updateViewLayout(container, params)
        }
    }

    override fun start(context: Context) {
        logger.info("starting self pip plugin")
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        patcher.after<SurfaceViewRenderer>(
            "surfaceCreated",
            SurfaceHolder::class.java
        ) {
            if (it.thisObject is AppVideoStreamRenderer) {
                logger.debug("target renderer detected via surface created")
                targetRenderer = it.thisObject as AppVideoStreamRenderer
            }
        }

        patcher.after<SurfaceViewRenderer>(
            "surfaceDestroyed",
            SurfaceHolder::class.java
        ) {
            if (it.thisObject == targetRenderer && !isPiPActive) {
                logger.debug("target renderer destroyed")
                targetRenderer = null
            }
        }

        patcher.after<Activity>("onPause") {
            if (isPiPActive || targetRenderer == null) return@after
            if (!Settings.canDrawOverlays(context)) {
                logger.warn("overlay permission missing")
                Utils.showToast("Grant Overlay Permission for PiP", true)
            } else {
                startFloatingWindow(context)
            }
        }
    }

    private fun startFloatingWindow(context: Context) {
        val renderer = targetRenderer ?: return
        isPiPActive = true
        logger.info("starting floating window")

        if (!detachRenderer(renderer)) {
            isPiPActive = false
            return
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayWidth = windowManager?.defaultDisplay?.width ?: 1280
        val displayHeight = windowManager?.defaultDisplay?.height ?: 720
        val initialWidth = renderer.width / 2
        val initialHeight = renderer.height / 2
        logger.debug("floating window initial size: ${initialWidth}x${initialHeight}, display size: ${displayWidth}x${displayHeight}, renderer size: ${renderer.width}x${renderer.height}")

        windowParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        windowParams!!.gravity = Gravity.TOP or Gravity.START
        windowParams!!.x = 100
        windowParams!!.y = 100

        floatContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)

            addView(
                renderer, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            val controlsLayout = createControlsOverlay(context)
            addView(
                controlsLayout, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            scaleGestureDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val params = windowParams!!
                        val scaleFactor = detector.scaleFactor

                        val newWidth = (params.width * scaleFactor).toInt()
                        val newHeight = (params.height * scaleFactor).toInt()
                        val minWidth = displayWidth / 4

                        if (newWidth in minWidth..displayWidth) {
                            params.width = newWidth
                            params.height = newHeight
                            updateFloatingLayout(params)
                        }
                        return true
                    }
                })

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isMove = false
                private var isScaling = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    scaleGestureDetector?.onTouchEvent(event)
                    isScaling = scaleGestureDetector?.isInProgress == true

                    if (isScaling) {
                        isMove = false
                        return true
                    }

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = windowParams!!.x
                            initialY = windowParams!!.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isMove = false
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val diffX = (event.rawX - initialTouchX).toInt()
                            val diffY = (event.rawY - initialTouchY).toInt()

                            if (abs(diffX) > 10 || abs(diffY) > 10) {
                                isMove = true
                                windowParams!!.x = initialX + diffX
                                windowParams!!.y = initialY + diffY
                                updateFloatingLayout(windowParams!!)
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!isMove && !isScaling) {
                                logger.info("tap toggled controls")
                                if (controlsLayout.visibility == View.VISIBLE) {
                                    hideControls(controlsLayout)
                                } else {
                                    showControls(controlsLayout)
                                }
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager?.addView(floatContainer, windowParams)
            logger.info("floating window added")
        } catch (e: Throwable) {
            logger.error("could not add floating window", e)
            isPiPActive = false
            restoreRendererToParent()
        }
    }

    private fun createControlsOverlay(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            isClickable = false

            val btnSize = 120

            createButton(context, android.R.drawable.ic_menu_close_clear_cancel) {
                logger.info("close button clicked")
                stopFloatingWindow()
            }.apply {
                addView(
                    this,
                    LinearLayout.LayoutParams(btnSize, btnSize).apply { marginEnd = 30 })
            }

            createButton(context, android.R.drawable.ic_menu_revert) {
                logger.info("restore button clicked")
                restoreToApp(context)
            }.apply {
                addView(
                    this,
                    LinearLayout.LayoutParams(btnSize, btnSize).apply { marginEnd = 30 })
            }

            createButton(context, android.R.drawable.ic_menu_help) {
                logger.info("placeholder button clicked")
                Utils.showToast("Placeholder", false)
            }.apply {
                addView(
                    this,
                    LinearLayout.LayoutParams(btnSize, btnSize)
                )
            }
        }
    }

    private fun createButton(context: Context, iconRes: Int, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#404040"))
            }
            setPadding(20, 20, 20, 20)
            setOnClickListener { onClick() }
        }
    }

    private fun showControls(controls: View) {
        controls.visibility = View.VISIBLE
        hideControlsRunnable?.let { controls.removeCallbacks(it) }
        hideControlsRunnable = Runnable { hideControls(controls) }
        controls.postDelayed(hideControlsRunnable, 5000)
    }

    private fun hideControls(controls: View) {
        controls.visibility = View.GONE
        hideControlsRunnable?.let { controls.removeCallbacks(it) }
        hideControlsRunnable = null
    }

    private fun restoreToApp(context: Context) {
        logger.info("restoring app to foreground")
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            stopFloatingWindow()
        } catch (e: Throwable) {
            logger.error("could not restore app", e)
        }
    }

    private fun stopFloatingWindow() {
        logger.info("stopping floating window")
        val container = floatContainer
        val renderer = targetRenderer

        hideControlsRunnable?.let { container?.removeCallbacks(it) }
        hideControlsRunnable = null

        try {
            if (container != null) {
                windowManager?.removeView(container)
                if (renderer != null) container.removeView(renderer)
            }
        } catch (e: Throwable) {
            logger.error("could not remove floating window", e)
        }

        if (renderer != null && originalParent != null) {
            restoreRendererToParent()
        }

        floatContainer = null
        isPiPActive = false
        targetRenderer = null
        originalLayoutParams = null
    }

    override fun stop(context: Context) {
        if (floatContainer != null) {
            stopFloatingWindow()
        }
        patcher.unpatchAll()
    }
}
