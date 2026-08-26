package com.shirulot.myshader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 独立的 Shader 练习页：只负责把 OpenGL Surface 和少量状态文字放到屏幕上。
 * 具体的 Shader 编译、纹理上传和绘制由 ShaderRenderer 处理，便于后续只改 .frag 文件。
 */
class ShaderDemoActivity : ComponentActivity() {
    private lateinit var shaderSurfaceView: ShaderSurfaceView
    private lateinit var statusText: TextView

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

        shaderSurfaceView = ShaderSurfaceView(this, fragmentShaderAsset) { message ->
            // GLSurfaceView 的回调运行在 GL 线程，状态文字必须切回主线程更新。
            runOnUiThread {
                if (!isFinishing) {
                    statusText.text = message
                }
            }
        }
        // 所有页面都显示两个控件；Shader 未声明对应 uniform 时渲染器会保持原画面。
        shaderSurfaceView.setWhitenStrength(initialWhitenStrength)
        shaderSurfaceView.setBlurStrength(DEFAULT_BLUR_STRENGTH)

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
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                panelHorizontalPadding,
                panelVerticalPadding,
                panelHorizontalPadding,
                panelVerticalPadding,
            )
            setBackgroundColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_panel))
            addView(TextView(this@ShaderDemoActivity).apply {
                setTextColor(ContextCompat.getColor(this@ShaderDemoActivity, R.color.shader_demo_text))
                text = getString(R.string.shader_demo_title_format, demoTitle)
                textSize = 17f
            })
            addView(statusText)
            addSourceImageControl(this)
            addWhitenStrengthControl(this, initialWhitenStrength)
            addBlurStrengthControl(this)
        }
        ViewCompat.setOnApplyWindowInsetsListener(panel) { view, windowInsets ->
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
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

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

    private fun blurStrengthFromProgress(progress: Int): Float =
        (BLUR_STRENGTH_MIN + progress * BLUR_STRENGTH_STEP).coerceAtMost(BLUR_STRENGTH_MAX)

    /** 将初始美白值映射为 SeekBar 进度，保证页面首次显示与 Shader 初值一致。 */
    private fun whitenStrengthToProgress(value: Float): Int =
        ((value.coerceIn(WHITEN_STRENGTH_MIN, WHITEN_STRENGTH_MAX) - WHITEN_STRENGTH_MIN) /
            WHITEN_STRENGTH_STEP).toInt()

    private fun whitenStrengthFromProgress(progress: Int): Float =
        (WHITEN_STRENGTH_MIN + progress * WHITEN_STRENGTH_STEP)
            .coerceAtMost(WHITEN_STRENGTH_MAX)

    companion object {
        private const val EXTRA_FRAGMENT_SHADER_ASSET = "fragment_shader_asset"
        private const val EXTRA_DEMO_TITLE = "demo_title"
        private const val EXTRA_INITIAL_WHITEN_STRENGTH = "initial_whiten_strength"
        private const val DEFAULT_FRAGMENT_SHADER_ASSET = "shaders/lesson_01_passthrough.frag"
        private const val WHITEN_STRENGTH_MIN = 0f
        private const val WHITEN_STRENGTH_MAX = 1f
        private const val WHITEN_STRENGTH_STEP = 0.01f
        private const val WHITEN_STRENGTH_PROGRESS_MAX = 100
        private const val DEFAULT_WHITEN_STRENGTH = 0f
        private const val BLUR_STRENGTH_MIN = 0f
        private const val BLUR_STRENGTH_MAX = 1f
        private const val BLUR_STRENGTH_STEP = 0.01f
        private const val BLUR_STRENGTH_PROGRESS_MAX = 100
        private const val BLUR_STRENGTH_DEFAULT_PROGRESS = 0
        private const val DEFAULT_BLUR_STRENGTH = 0f

        /** 统一构造跳转参数，避免调用方拼错 Intent extra。 */
        fun createIntent(
            context: Context,
            fragmentShaderAsset: String,
            demoTitle: String,
            initialWhitenStrength: Float,
        ): Intent = Intent(context, ShaderDemoActivity::class.java).apply {
            putExtra(EXTRA_FRAGMENT_SHADER_ASSET, fragmentShaderAsset)
            putExtra(EXTRA_DEMO_TITLE, demoTitle)
            putExtra(EXTRA_INITIAL_WHITEN_STRENGTH, initialWhitenStrength)
        }
    }
}
