package com.shirulot.myshader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 独立的 Shader 练习页：只负责把 OpenGL Surface 和少量状态文字放到屏幕上。
 * 具体的 Shader 编译、纹理上传和绘制由 ShaderRenderer 处理，便于后续只改 .frag 文件。
 */
class ShaderDemoActivity : ComponentActivity() {
    private lateinit var shaderSurfaceView: ShaderSurfaceView
    private lateinit var statusText: TextView
    private lateinit var controlPanel: LinearLayout
    private lateinit var expandControlsButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        statusText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            text = getString(R.string.shader_demo_status_loading)
            textSize = 13f
            setTextIsSelectable(true)
        }

        val fragmentShaderAsset = intent.getStringExtra(EXTRA_FRAGMENT_SHADER_ASSET)
            ?: DEFAULT_FRAGMENT_SHADER_ASSET
        val demoTitle = intent.getStringExtra(EXTRA_DEMO_TITLE)
            ?: getString(R.string.demo_passthrough_title)
        val initialWhitenStrength = intent.getFloatExtra(
            EXTRA_INITIAL_WHITEN_STRENGTH,
            DEFAULT_WHITEN_STRENGTH,
        )
        val initialBrightenStrength = intent.getFloatExtra(
            EXTRA_INITIAL_BRIGHTEN_STRENGTH,
            DEFAULT_BRIGHTEN_STRENGTH,
        )
        val showWhitenStrengthControl = intent.getBooleanExtra(
            EXTRA_SHOW_WHITEN_STRENGTH_CONTROL,
            false,
        )
        val showBrightenStrengthControl = intent.getBooleanExtra(
            EXTRA_SHOW_BRIGHTEN_STRENGTH_CONTROL,
            false,
        )
        val showBlurStrengthControl = intent.getBooleanExtra(
            EXTRA_SHOW_BLUR_STRENGTH_CONTROL,
            false,
        )
        val showWarmthStrengthControl = intent.getBooleanExtra(
            EXTRA_SHOW_WARMTH_STRENGTH_CONTROL,
            false,
        )
        val showSaturationStrengthControl = intent.getBooleanExtra(
            EXTRA_SHOW_SATURATION_STRENGTH_CONTROL,
            false,
        )

        shaderSurfaceView = ShaderSurfaceView(this, fragmentShaderAsset) { message ->
            // GLSurfaceView 的回调运行在 GL 线程，状态文字必须切回主线程更新。
            runOnUiThread {
                if (!isFinishing) {
                    statusText.text = message
                }
            }
        }
        // 不展示的控件仍使用默认 0.00，避免改变基础或原图直通的默认画面。
        shaderSurfaceView.setWhitenStrength(initialWhitenStrength)
        shaderSurfaceView.setBrightenStrength(initialBrightenStrength)
        shaderSurfaceView.setBlurStrength(DEFAULT_BLUR_STRENGTH)
        shaderSurfaceView.setSaturationStrength(DEFAULT_SATURATION_STRENGTH)

        GlobalScope.launch(IO) {
            val landmarks = FaceAnalysis.detectLessonFace(resources)
            val faceCenterRegion = FaceAnalysis.calculateFaceRegion(landmarks)
            shaderSurfaceView.setFaceCenterRegion(
                faceCenterRegion.centerX,
                faceCenterRegion.centerY,
                faceCenterRegion.width,
                faceCenterRegion.height
            )
            Log.i("FaceAnalysis", "pointCount=${landmarks.size / 2}, center=$faceCenterRegion")
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_background))
        }
        root.addView(
            shaderSurfaceView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val panelHorizontalPadding = dp(16)
        val panelVerticalPadding = dp(12)
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                panelHorizontalPadding,
                panelVerticalPadding,
                panelHorizontalPadding,
                panelVerticalPadding,
            )
            setBackgroundColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_panel))
            addView(LinearLayout(this@ShaderDemoActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@ShaderDemoActivity).apply {
                    setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
                    text = getString(R.string.shader_demo_title_format, demoTitle)
                    textSize = 17f
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(TextView(this@ShaderDemoActivity).apply {
                    setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
                    setText(R.string.shader_demo_controls_collapse)
                    textSize = 13f
                    contentDescription = getString(R.string.shader_demo_controls_collapse_content_description)
                    setPadding(dp(12), dp(8), 0, dp(8))
                    setOnClickListener { collapseControls() }
                })
            })
            addView(statusText)
            addSourceImageControl(this)
            if (showWhitenStrengthControl) {
                addWhitenStrengthControl(this, initialWhitenStrength)
            }
            if (showBrightenStrengthControl) {
                addBrightenStrengthControl(this, initialBrightenStrength)
            }
            if (showBlurStrengthControl) {
                addBlurStrengthControl(this)
            }
            if (showWarmthStrengthControl) {
                addWarmthStrengthControl(this)
            }
            if (showSaturationStrengthControl) {
                addSaturationStrengthControl(this)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(controlPanel) { view, windowInsets ->
            val navigationBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // 保留面板原有间距，并把交互内容抬到三键导航或手势条的安全区域内。
            view.setPadding(
                panelHorizontalPadding,
                panelVerticalPadding,
                panelHorizontalPadding,
                panelVerticalPadding + navigationBarInsets.bottom,
            )
            windowInsets
        }
        root.addView(
            controlPanel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )
        // 面板收起后，只在底部保留展开入口，完整控制内容不再覆盖测试图片。
        expandControlsButton = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            setBackgroundColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_panel))
            setText(R.string.shader_demo_controls_expand)
            textSize = 13f
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.shader_demo_controls_expand_content_description)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            visibility = android.view.View.GONE
            setOnClickListener { expandControls() }
        }
        root.addView(
            expandControlsButton,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply {
                bottomMargin = dp(12)
            },
        )
        ViewCompat.setOnApplyWindowInsetsListener(expandControlsButton) { view, windowInsets ->
            val navigationBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            (view.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = dp(12) + navigationBarInsets.bottom
                view.layoutParams = this
            }
            windowInsets
        }

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        shaderSurfaceView.onResume()
    }

    override fun onPause() {
        shaderSurfaceView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        // 让 GPU 资源在 GL 线程释放，避免 Activity 退出时留下纹理和 Program。
        shaderSurfaceView.release()
        super.onDestroy()
    }

    /** 让完整面板下滑出屏，收起后不再覆盖测试图片。 */
    private fun collapseControls() {
        controlPanel.animate().cancel()
        expandControlsButton.animate().cancel()
        controlPanel.animate()
            .translationY(controlPanel.height.toFloat())
            .setDuration(CONTROL_PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                // INVISIBLE 保留已测量高度，展开时可以从底部稳定地上滑回来。
                controlPanel.visibility = android.view.View.INVISIBLE
                controlPanel.translationY = 0f
                expandControlsButton.apply {
                    alpha = 0f
                    translationY = dp(EXPAND_BUTTON_ENTER_OFFSET_DP).toFloat()
                    visibility = android.view.View.VISIBLE
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(CONTROL_PANEL_ANIMATION_DURATION_MS)
                        .start()
                }
            }
            .start()
    }

    /** 隐藏底部入口，并让完整面板从屏幕底部上滑回来。 */
    private fun expandControls() {
        controlPanel.animate().cancel()
        expandControlsButton.animate().cancel()
        expandControlsButton.animate()
            .alpha(0f)
            .translationY(dp(EXPAND_BUTTON_ENTER_OFFSET_DP).toFloat())
            .setDuration(CONTROL_PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                expandControlsButton.visibility = android.view.View.GONE
                controlPanel.apply {
                    translationY = height.toFloat()
                    visibility = android.view.View.VISIBLE
                    animate()
                        .translationY(0f)
                        .setDuration(CONTROL_PANEL_ANIMATION_DURATION_MS)
                        .start()
                }
            }
            .start()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun addSourceImageControl(panel: LinearLayout) {
        val sourceImageText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            text = getString(R.string.shader_demo_source_image_label)
            textSize = 13f
        }
        val originalImageButton = sourceImageRadioButton(R.string.shader_demo_source_image_original)
        val detailImageButton = sourceImageRadioButton(R.string.shader_demo_source_image_detail)
        val sourceImageGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            addView(originalImageButton)
            addView(detailImageButton)
            setOnCheckedChangeListener { _, checkedId ->
                val selectedImageRes = when (checkedId) {
                    originalImageButton.id -> R.drawable.lesson_face
                    detailImageButton.id -> R.drawable.lesson_face_detail
                    else -> return@setOnCheckedChangeListener
                }
                // 两张图片对所有 Demo 共用同一纹理入口，Shader 本身无需修改。
                shaderSurfaceView.setSourceImage(selectedImageRes)
            }
        }
        // 原练习图保持默认选中，新图片可在任意 Demo 中随时切换。
        originalImageButton.isChecked = true
        panel.addView(
            sourceImageText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            sourceImageGroup,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun sourceImageRadioButton(textRes: Int): RadioButton =
        RadioButton(this).apply {
            id = android.view.View.generateViewId()
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            setText(textRes)
            textSize = 13f
        }

    private fun addWhitenStrengthControl(panel: LinearLayout, initialValue: Float) {
        val strengthText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            textSize = 13f
            text = getString(R.string.shader_demo_whiten_strength_label, initialValue)
        }
        val strengthSeekBar = SeekBar(this).apply {
            max = WHITEN_STRENGTH_PROGRESS_MAX
            progress = whitenStrengthToProgress(initialValue)
            contentDescription = getString(R.string.shader_demo_whiten_strength_content_description)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = whitenStrengthFromProgress(progress)
                    strengthText.text = getString(R.string.shader_demo_whiten_strength_label, value)
                    shaderSurfaceView.setWhitenStrength(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            strengthText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            strengthSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /** 将提亮强度映射为 0.00 到 1.00，并上传给 Shader uniform。 */
    private fun addBrightenStrengthControl(panel: LinearLayout, initialValue: Float) {
        val strengthText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            textSize = 13f
            text = getString(R.string.shader_demo_brighten_strength_label, initialValue)
        }
        val strengthSeekBar = SeekBar(this).apply {
            max = BRIGHTEN_STRENGTH_PROGRESS_MAX
            progress = brightenStrengthToProgress(initialValue)
            contentDescription = getString(R.string.shader_demo_brighten_strength_content_description)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = brightenStrengthFromProgress(progress)
                    strengthText.text = getString(R.string.shader_demo_brighten_strength_label, value)
                    // 将提亮强度上传给当前 Shader。
                    shaderSurfaceView.setBrightenStrength(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            strengthText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            strengthSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun addBlurStrengthControl(panel: LinearLayout) {
        val strengthText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            textSize = 13f
            text = getString(R.string.shader_demo_blur_strength_label, DEFAULT_BLUR_STRENGTH)
        }
        val strengthSeekBar = SeekBar(this).apply {
            max = BLUR_STRENGTH_PROGRESS_MAX
            progress = BLUR_STRENGTH_DEFAULT_PROGRESS
            contentDescription = getString(R.string.shader_demo_blur_strength_content_description)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = blurStrengthFromProgress(progress)
                    strengthText.text = getString(R.string.shader_demo_blur_strength_label, value)
                    shaderSurfaceView.setBlurStrength(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            strengthText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            strengthSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /** 暖色补正映射为 0.00 到 1.00，并上传给 Shader uniform。 */
    private fun addWarmthStrengthControl(panel: LinearLayout) {
        val strengthText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            textSize = 13f
            text = getString(R.string.shader_demo_warmth_strength_label, DEFAULT_WARMTH_STRENGTH)
        }
        val strengthSeekBar = SeekBar(this).apply {
            max = WARMTH_STRENGTH_PROGRESS_MAX
            progress = WARMTH_STRENGTH_DEFAULT_PROGRESS
            contentDescription = getString(R.string.shader_demo_warmth_strength_content_description)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = warmthStrengthFromProgress(progress)
                    strengthText.text = getString(R.string.shader_demo_warmth_strength_label, value)
                    // 将暖色补正强度上传给当前 Shader。
                    shaderSurfaceView.setWarmthStrength(value)

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            strengthText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            strengthSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    /** 饱和度映射为 0.00 到 0.30，并上传给 Shader uniform。 */
    private fun addSaturationStrengthControl(panel: LinearLayout) {
        val strengthText = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
            textSize = 13f
            text = getString(R.string.shader_demo_saturation_strength_label, DEFAULT_SATURATION_STRENGTH)
        }
        val strengthSeekBar = SeekBar(this).apply {
            max = SATURATION_STRENGTH_PROGRESS_MAX
            progress = SATURATION_STRENGTH_DEFAULT_PROGRESS
            contentDescription = getString(R.string.shader_demo_saturation_strength_content_description)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = saturationStrengthFromProgress(progress)
                    strengthText.text = getString(R.string.shader_demo_saturation_strength_label, value)
                    // 将饱和度强度上传给当前 Shader。
                    shaderSurfaceView.setSaturationStrength(value)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        panel.addView(
            strengthText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        panel.addView(
            strengthSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun blurStrengthFromProgress(progress: Int): Float =
        (BLUR_STRENGTH_MIN + progress * BLUR_STRENGTH_STEP).coerceAtMost(BLUR_STRENGTH_MAX)

    private fun warmthStrengthFromProgress(progress: Int): Float =
        (WARMTH_STRENGTH_MIN + progress * WARMTH_STRENGTH_STEP).coerceAtMost(WARMTH_STRENGTH_MAX)

    private fun saturationStrengthFromProgress(progress: Int): Float =
        (SATURATION_STRENGTH_MIN + progress * SATURATION_STRENGTH_STEP)
            .coerceAtMost(SATURATION_STRENGTH_MAX)

    /** 将初始美白值映射为 SeekBar 进度，保证页面首次显示与 Shader 初值一致。 */
    private fun whitenStrengthToProgress(value: Float): Int =
        ((value.coerceIn(WHITEN_STRENGTH_MIN, WHITEN_STRENGTH_MAX) - WHITEN_STRENGTH_MIN) /
                WHITEN_STRENGTH_STEP).toInt()

    private fun whitenStrengthFromProgress(progress: Int): Float =
        (WHITEN_STRENGTH_MIN + progress * WHITEN_STRENGTH_STEP)
            .coerceAtMost(WHITEN_STRENGTH_MAX)

    private fun brightenStrengthToProgress(value: Float): Int =
        ((value.coerceIn(BRIGHTEN_STRENGTH_MIN, BRIGHTEN_STRENGTH_MAX) - BRIGHTEN_STRENGTH_MIN) /
                BRIGHTEN_STRENGTH_STEP).toInt()

    private fun brightenStrengthFromProgress(progress: Int): Float =
        (BRIGHTEN_STRENGTH_MIN + progress * BRIGHTEN_STRENGTH_STEP)
            .coerceAtMost(BRIGHTEN_STRENGTH_MAX)

    companion object {
        private const val EXTRA_FRAGMENT_SHADER_ASSET = "fragment_shader_asset"
        private const val EXTRA_DEMO_TITLE = "demo_title"
        private const val EXTRA_INITIAL_WHITEN_STRENGTH = "initial_whiten_strength"
        private const val EXTRA_INITIAL_BRIGHTEN_STRENGTH = "initial_brighten_strength"
        private const val EXTRA_SHOW_WHITEN_STRENGTH_CONTROL = "show_whiten_strength_control"
        private const val EXTRA_SHOW_BRIGHTEN_STRENGTH_CONTROL = "show_brighten_strength_control"
        private const val EXTRA_SHOW_BLUR_STRENGTH_CONTROL = "show_blur_strength_control"
        private const val EXTRA_SHOW_WARMTH_STRENGTH_CONTROL = "show_warmth_strength_control"
        private const val EXTRA_SHOW_SATURATION_STRENGTH_CONTROL = "show_saturation_strength_control"
        private const val DEFAULT_FRAGMENT_SHADER_ASSET = "main.frag"
        private const val WHITEN_STRENGTH_MIN = 0f
        private const val WHITEN_STRENGTH_MAX = 0.15f
        private const val WHITEN_STRENGTH_STEP = 0.01f
        private const val WHITEN_STRENGTH_PROGRESS_MAX = 15
        private const val DEFAULT_WHITEN_STRENGTH = 0f
        private const val BRIGHTEN_STRENGTH_MIN = 0f
        private const val BRIGHTEN_STRENGTH_MAX = 1f
        private const val BRIGHTEN_STRENGTH_STEP = 0.01f
        private const val BRIGHTEN_STRENGTH_PROGRESS_MAX = 100
        private const val DEFAULT_BRIGHTEN_STRENGTH = 0f
        private const val BLUR_STRENGTH_MIN = 0f
        private const val BLUR_STRENGTH_MAX = 1f
        private const val BLUR_STRENGTH_STEP = 0.01f
        private const val BLUR_STRENGTH_PROGRESS_MAX = 100
        private const val BLUR_STRENGTH_DEFAULT_PROGRESS = 0
        private const val DEFAULT_BLUR_STRENGTH = 0f
        private const val WARMTH_STRENGTH_MIN = 0f
        private const val WARMTH_STRENGTH_MAX = 1f
        private const val WARMTH_STRENGTH_STEP = 0.01f
        private const val WARMTH_STRENGTH_PROGRESS_MAX = 100
        private const val WARMTH_STRENGTH_DEFAULT_PROGRESS = 0
        private const val DEFAULT_WARMTH_STRENGTH = 0f
        private const val SATURATION_STRENGTH_MIN = 0f
        private const val SATURATION_STRENGTH_MAX = 0.3f
        private const val SATURATION_STRENGTH_STEP = 0.01f
        private const val SATURATION_STRENGTH_PROGRESS_MAX = 30
        private const val SATURATION_STRENGTH_DEFAULT_PROGRESS = 0
        private const val DEFAULT_SATURATION_STRENGTH = 0f
        private const val CONTROL_PANEL_ANIMATION_DURATION_MS = 220L
        private const val EXPAND_BUTTON_ENTER_OFFSET_DP = 12

        /** 统一构造跳转参数，避免调用方拼错 Intent extra。 */
        fun createIntent(
            context: Context,
            fragmentShaderAsset: String,
            demoTitle: String,
            initialWhitenStrength: Float,
            initialBrightenStrength: Float,
            showWhitenStrengthControl: Boolean,
            showBrightenStrengthControl: Boolean,
            showBlurStrengthControl: Boolean,
            showWarmthStrengthControl: Boolean,
            showSaturationStrengthControl: Boolean,
        ): Intent = Intent(context, ShaderDemoActivity::class.java).apply {
            putExtra(EXTRA_FRAGMENT_SHADER_ASSET, fragmentShaderAsset)
            putExtra(EXTRA_DEMO_TITLE, demoTitle)
            putExtra(EXTRA_INITIAL_WHITEN_STRENGTH, initialWhitenStrength)
            putExtra(EXTRA_INITIAL_BRIGHTEN_STRENGTH, initialBrightenStrength)
            putExtra(EXTRA_SHOW_WHITEN_STRENGTH_CONTROL, showWhitenStrengthControl)
            putExtra(EXTRA_SHOW_BRIGHTEN_STRENGTH_CONTROL, showBrightenStrengthControl)
            putExtra(EXTRA_SHOW_BLUR_STRENGTH_CONTROL, showBlurStrengthControl)
            putExtra(EXTRA_SHOW_WARMTH_STRENGTH_CONTROL, showWarmthStrengthControl)
            putExtra(EXTRA_SHOW_SATURATION_STRENGTH_CONTROL, showSaturationStrengthControl)
        }
    }
}
