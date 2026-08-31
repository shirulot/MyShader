package com.shirulot.myshader

import androidx.annotation.StringRes

/** 一个首页条目对应一个独立的片元 Shader 文件。 */
data class ShaderDemo(
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
    val fragmentShaderAsset: String,
    // 所有 Demo 默认从原图开始；未声明 whitenStrength 的 Shader 不受影响。
    val initialWhitenStrength: Float = 0f,
    // 滑条展示按 Demo 的实际最终输出决定；渲染器仍会安全忽略未声明的 uniform。
    val showWhitenStrengthControl: Boolean = false,
    val showBlurStrengthControl: Boolean = false,
    // 暖色滑条控制 Shader 的 warmthStrength uniform。
    val showWarmthStrengthControl: Boolean = false,
)

/** 一组可展开/收起的 Shader Demo。 */
data class ShaderDemoGroup(
    @get:StringRes val titleRes: Int,
    val demos: List<ShaderDemo>,
    val initiallyExpanded: Boolean = false,
)

/** 集中维护 Demo 顺序，避免首页和渲染页各自保存一份映射。 */
object ShaderDemoCatalog {
    val standaloneItems: List<ShaderDemo> = listOf(
        // main.frag 当前练习使用 blurStrength，因此独立入口展示磨皮滑条。
        ShaderDemo(
            R.string.demo_passthrough_title,
            R.string.demo_passthrough_description,
            "main.frag",
            showBlurStrengthControl = true,
            showWarmthStrengthControl = true,
        ),
    )

    val groups: List<ShaderDemoGroup> = listOf(
        ShaderDemoGroup(
            titleRes = R.string.demo_group_basics,
            demos = listOf(
                ShaderDemo(R.string.demo_01_title, R.string.demo_01_description, "shaders/basic/demo_01_solid_red.frag"),
                ShaderDemo(R.string.demo_02_title, R.string.demo_02_description, "shaders/basic/demo_02_uv_gradient.frag"),
                ShaderDemo(R.string.demo_03_title, R.string.demo_03_description, "shaders/basic/demo_03_shift_coordinate.frag"),
                ShaderDemo(R.string.demo_04_title, R.string.demo_04_description, "shaders/basic/demo_04_darken.frag"),
                ShaderDemo(R.string.demo_05_title, R.string.demo_05_description, "shaders/basic/demo_05_brighten.frag"),
            ),
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_whitening,
            demos = listOf(
                ShaderDemo(R.string.demo_06_title, R.string.demo_06_description, "shaders/whitening/demo_06_mix_white.frag", showWhitenStrengthControl = true),
                ShaderDemo(R.string.demo_07_title, R.string.demo_07_description, "shaders/whitening/demo_07_conditional_white.frag", showWhitenStrengthControl = true),
                ShaderDemo(R.string.demo_08_title, R.string.demo_08_description, "shaders/whitening/demo_08_side_by_side.frag", showWhitenStrengthControl = true),
                ShaderDemo(R.string.demo_09_title, R.string.demo_09_description, "shaders/whitening/demo_09_binary_mask.frag", showWhitenStrengthControl = true),
                ShaderDemo(R.string.demo_10_title, R.string.demo_10_description, "shaders/whitening/demo_10_grayscale_mask.frag", showWhitenStrengthControl = true),
            ),
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_skin_smoothing,
            demos = listOf(
                ShaderDemo(R.string.demo_11_title, R.string.demo_11_description, "shaders/skin_smoothing/demo_11_two_tap_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_12_title, R.string.demo_12_description, "shaders/skin_smoothing/demo_12_three_tap_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_13_title, R.string.demo_13_description, "shaders/skin_smoothing/demo_13_skin_weighted_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_14_title, R.string.demo_14_description, "shaders/skin_smoothing/demo_14_edge_protected_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_15_title, R.string.demo_15_description, "shaders/skin_smoothing/demo_15_nine_tap_edge_protected_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_16_title, R.string.demo_16_description, "shaders/skin_smoothing/demo_16_nine_tap_edge_protected_blur.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_17_title, R.string.demo_17_description, "shaders/skin_smoothing/demo_17_bilateral_edge_protected_blur.frag", showBlurStrengthControl = true),
            ),
            initiallyExpanded = false,
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_local_color_difference_debug,
            demos = listOf(
                // 此 Demo 最终输出局部色差灰度图，resultRgb 未参与输出，因此不展示滑条。
                ShaderDemo(R.string.demo_18_title, R.string.demo_18_description, "shaders/local_color_difference_debug/demo_18_edge_difference_debug.frag"),
                ShaderDemo(R.string.demo_19_title, R.string.demo_19_description, "shaders/local_color_difference_debug/demo_19_warm_skin_smoothing.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_20_title, R.string.demo_20_description, "shaders/local_color_difference_debug/demo_20_local_warm_tone_debug.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_21_title, R.string.demo_21_description, "shaders/local_color_difference_debug/demo_21_warmth_strength_uniform.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
            ),
            initiallyExpanded = false,
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_brightness_and_chroma,
            demos = listOf(
                ShaderDemo(R.string.demo_22_title, R.string.demo_22_description, "shaders/brightness_and_chroma/demo_22_luminance_chroma_debug.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                ShaderDemo(R.string.demo_23_title, R.string.demo_23_description, "shaders/brightness_and_chroma/demo_23_luminance_range_warmth.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                ShaderDemo(R.string.demo_24_title, R.string.demo_24_description, "shaders/brightness_and_chroma/demo_24_midtone_weight_debug.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                ShaderDemo(R.string.demo_25_title, R.string.demo_25_description, "shaders/brightness_and_chroma/demo_25_warmth_weight_debug.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                ShaderDemo(R.string.demo_26_title, R.string.demo_26_description, "shaders/brightness_and_chroma/demo_26_chroma_enhancement.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                ShaderDemo(R.string.demo_27_title, R.string.demo_27_description, "shaders/brightness_and_chroma/demo_27_warm_tone_result.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                // Demo 28 归档当前 main.frag，最终输出使用磨皮结果和暖色/饱和度结果。
                ShaderDemo(R.string.demo_28_title, R.string.demo_28_description, "shaders/brightness_and_chroma/demo_28_warm_saturation_range.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
            ),
            initiallyExpanded = false,
        ),
    )
}
