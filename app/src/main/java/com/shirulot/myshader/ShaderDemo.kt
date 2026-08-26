package com.shirulot.myshader

import androidx.annotation.StringRes

/** 一个首页条目对应一个独立的片元 Shader 文件。 */
data class ShaderDemo(
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
    val fragmentShaderAsset: String,
    // 所有 Demo 默认从原图开始；未声明 whitenStrength 的 Shader 不受影响。
    val initialWhitenStrength: Float = 0f,
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
        ShaderDemo(R.string.demo_passthrough_title, R.string.demo_passthrough_description, "shaders/lesson_01_passthrough.frag"),
    )

    val groups: List<ShaderDemoGroup> = listOf(
        ShaderDemoGroup(
            titleRes = R.string.demo_group_whitening,
            demos = listOf(
                ShaderDemo(R.string.demo_01_title, R.string.demo_01_description, "shaders/demo_01_solid_red.frag"),
                ShaderDemo(R.string.demo_02_title, R.string.demo_02_description, "shaders/demo_02_uv_gradient.frag"),
                ShaderDemo(R.string.demo_03_title, R.string.demo_03_description, "shaders/demo_03_shift_coordinate.frag"),
                ShaderDemo(R.string.demo_04_title, R.string.demo_04_description, "shaders/demo_04_darken.frag"),
                ShaderDemo(R.string.demo_05_title, R.string.demo_05_description, "shaders/demo_05_brighten.frag"),
                ShaderDemo(R.string.demo_06_title, R.string.demo_06_description, "shaders/demo_06_mix_white.frag"),
                ShaderDemo(R.string.demo_07_title, R.string.demo_07_description, "shaders/demo_07_conditional_white.frag"),
                ShaderDemo(R.string.demo_08_title, R.string.demo_08_description, "shaders/demo_08_side_by_side.frag"),
                ShaderDemo(R.string.demo_09_title, R.string.demo_09_description, "shaders/demo_09_binary_mask.frag"),
                ShaderDemo(R.string.demo_10_title, R.string.demo_10_description, "shaders/demo_10_grayscale_mask.frag"),
            ),
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_skin_smoothing,
            demos = listOf(
                ShaderDemo(R.string.demo_11_title, R.string.demo_11_description, "shaders/demo_11_two_tap_blur.frag"),
                ShaderDemo(R.string.demo_12_title, R.string.demo_12_description, "shaders/demo_12_three_tap_blur.frag"),
                ShaderDemo(R.string.demo_13_title, R.string.demo_13_description, "shaders/demo_13_skin_weighted_blur.frag"),
                ShaderDemo(R.string.demo_14_title, R.string.demo_14_description, "shaders/demo_14_edge_protected_blur.frag"),
                ShaderDemo(R.string.demo_15_title, R.string.demo_15_description, "shaders/demo_15_nine_tap_edge_protected_blur.frag"),
                ShaderDemo(R.string.demo_16_title, R.string.demo_16_description, "shaders/demo_16_nine_tap_edge_protected_blur.frag"),
                ShaderDemo(R.string.demo_17_title, R.string.demo_17_description, "shaders/demo_17_bilateral_edge_protected_blur.frag"),
            ),
            initiallyExpanded = false,
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_local_color_difference_debug,
            demos = emptyList(),
            initiallyExpanded = false,
        ),
    )
}
