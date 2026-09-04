package com.shirulot.myshader

import androidx.annotation.StringRes

/** 一个首页条目对应一个独立的片元 Shader 文件。 */
data class ShaderDemo(
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
    val fragmentShaderAsset: String,
    // 所有 Demo 默认从原图开始；未声明 whitenStrength 的 Shader 不受影响。
    val initialWhitenStrength: Float = 0f,
    // 提亮默认关闭；未声明 brightenStrength 的 Shader 不受影响。
    val initialBrightenStrength: Float = 0f,
    // 滑条展示按 Demo 的实际最终输出决定；归档入口 Demo 时需同步对应控制配置。
    // 渲染器仍会安全忽略未声明的 uniform。
    val showWhitenStrengthControl: Boolean = false,
    val showBrightenStrengthControl: Boolean = false,
    val showBlurStrengthControl: Boolean = false,
    // 暖色补正滑条控制 Shader 的 warmthStrength uniform。
    val showWarmthStrengthControl: Boolean = false,
    // 饱和度滑条控制 Shader 的 saturationStrength uniform。
    val showSaturationStrengthControl: Boolean = false,
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
        // 当前 main.frag 的两个 uniform 尚未参与最终输出，因此不展示无效滑条。
        ShaderDemo(
            R.string.demo_passthrough_title,
            R.string.demo_passthrough_description,
            "main.frag",
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
                // Demo 09 只输出二值 mask，没有 whitenStrength uniform，因此不展示无效滑条。
                ShaderDemo(R.string.demo_09_title, R.string.demo_09_description, "shaders/whitening/demo_09_binary_mask.frag"),
                ShaderDemo(R.string.demo_10_title, R.string.demo_10_description, "shaders/whitening/demo_10_grayscale_mask.frag", showWhitenStrengthControl = true),
            ),
        ),
        ShaderDemoGroup(
            titleRes = R.string.demo_group_skin_smoothing,
            demos = listOf(
                // Demo 11-13 使用固定采样权重，没有 blurStrength uniform，因此不展示无效滑条。
                ShaderDemo(R.string.demo_11_title, R.string.demo_11_description, "shaders/skin_smoothing/demo_11_two_tap_blur.frag"),
                ShaderDemo(R.string.demo_12_title, R.string.demo_12_description, "shaders/skin_smoothing/demo_12_three_tap_blur.frag"),
                ShaderDemo(R.string.demo_13_title, R.string.demo_13_description, "shaders/skin_smoothing/demo_13_skin_weighted_blur.frag"),
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
                // Demo 29 归档独立 saturationStrength 版本；当前 Shader 实际使用暖色补正和饱和度。
                ShaderDemo(R.string.demo_29_title, R.string.demo_29_description, "shaders/brightness_and_chroma/demo_29_saturation_control.frag", showWarmthStrengthControl = true, showSaturationStrengthControl = true),
            ),
            initiallyExpanded = false,
        ),
        // 图像输入练习放在人脸分析与区域遮罩之前，保持输入坐标到区域处理的学习顺序。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_image_input_and_color_management,
            demos = listOf(
                // Demo 34 归档 UV 旋转与前摄镜像版本；当前没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_34_title, R.string.demo_34_description, "shaders/image_input_and_color_management/demo_34_uv_rotate_mirror.frag"),
                // Demo 35 归档当前 main.frag 的 UV 方向标记版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_35_title, R.string.demo_35_description, "shaders/image_input_and_color_management/demo_35_uv_orientation_marker.frag"),
                // Demo 36 归档当前 main.frag 的 YUV 色度采样版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_36_title, R.string.demo_36_description, "shaders/image_input_and_color_management/demo_36_yuv_chroma_subsampling.frag"),
                // Demo 37 归档当前 main.frag 的 RGB/YUV 往返转换版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_37_title, R.string.demo_37_description, "shaders/image_input_and_color_management/demo_37_rgb_yuv_roundtrip.frag"),
                // Demo 38 归档当前 main.frag 的 Gamma 亮度对比版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_38_title, R.string.demo_38_description, "shaders/image_input_and_color_management/demo_38_gamma_brightness_comparison.frag"),
                // Demo 39 归档当前 main.frag 的白平衡暖色校正版本；声明的调节 uniform 未参与最终输出，因此不展示滑条。
                ShaderDemo(R.string.demo_39_title, R.string.demo_39_description, "shaders/image_input_and_color_management/demo_39_white_balance_warm_correction.frag"),
                // Demo 40 归档白平衡与曝光补偿三栏对比；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_40_title, R.string.demo_40_description, "shaders/image_input_and_color_management/demo_40_white_balance_exposure_compensation.frag"),
            ),
            initiallyExpanded = false,
        ),
        // 第五章的人脸分析与区域遮罩 Demo 统一放在独立分组中。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_face_analysis_and_mask,
            demos = listOf(
                // Demo 30 归档当前 main.frag；最终输出仅依赖人脸区域和肤色遮罩。
                ShaderDemo(R.string.demo_30_title, R.string.demo_30_description, "shaders/face_analysis_and_mask/demo_30_face_region_mask.frag"),
                // Demo 31 归档椭圆人脸区域版本；最终输出仍只使用人脸与肤色遮罩。
                ShaderDemo(R.string.demo_31_title, R.string.demo_31_description, "shaders/face_analysis_and_mask/demo_31_ellipse_face_region_mask.frag"),
                // Demo 32 归档人脸中心 uniform 版本；最终输出仍只使用人脸与肤色遮罩。
                ShaderDemo(R.string.demo_32_title, R.string.demo_32_description, "shaders/face_analysis_and_mask/demo_32_face_center_mask.frag"),
                // Demo 33 归档人脸包围盒版本；调色 uniform 未参与最终输出，因此不展示滑条。
                ShaderDemo(R.string.demo_33_title, R.string.demo_33_description, "shaders/face_analysis_and_mask/demo_33_face_bounding_box_mask.frag"),
            ),
            initiallyExpanded = false,
        ),
    )
}
