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
    // 人脸类效果默认关闭；未声明对应 uniform 的 Shader 不受影响。
    val initialBlackCircleStrength: Float = 0f,
    val initialBigEyeStrength: Float = 0f,
    val initialSlimFaceStrength: Float = 0f,
    // 所有口红 Demo 默认关闭染色效果。
    val initialLipstickStrength: Float = 0f,
    // 滑条展示按 Demo 的实际最终输出决定；归档入口 Demo 时需同步对应控制配置。
    // 渲染器仍会安全忽略未声明的 uniform。
    val showWhitenStrengthControl: Boolean = false,
    val showBrightenStrengthControl: Boolean = false,
    val showBlackCircleStrengthControl: Boolean = false,
    val showBigEyeStrengthControl: Boolean = false,
    val showSlimFaceStrengthControl: Boolean = false,
    val showLipstickStrengthControl: Boolean = false,
    val showBlurStrengthControl: Boolean = false,
    // 暖色补正滑条控制 Shader 的 warmthStrength uniform。
    val showWarmthStrengthControl: Boolean = false,
    // 饱和度滑条控制 Shader 的 saturationStrength uniform。
    val showSaturationStrengthControl: Boolean = false,
    // 腮红默认关闭；与口红强度分别保存和控制。
    val initialBlushStrength: Float = 0f,
    // 仅实际使用 blushStrength 的 Demo 展示腮红滑条。
    val showBlushStrengthControl: Boolean = false,
    // 腮红范围控制满权重的着色区域，数值越大区域越大；关键点半径保持不变。
    val initialBlushRange: Float = 0.4f,
    // 腮红范围与颜色强度使用独立的控制开关。
    val showBlushRangeControl: Boolean = false,
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
        // 当前 main.frag 练习双侧腮红渐变，分别控制腮红强度和着色范围。
        ShaderDemo(
            R.string.demo_passthrough_title,
            R.string.demo_passthrough_description,
            "main.frag",
            initialBlushStrength = 0f,
            showBlushStrengthControl = true,
            initialBlushRange = 0.4f,
            showBlushRangeControl = true,
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
            titleRes = R.string.demo_group_skin_tone_equalization,
            demos = listOf(
                // 此 Demo 最终输出局部色差灰度图，resultRgb 未参与输出，因此不展示滑条。
                ShaderDemo(R.string.demo_18_title, R.string.demo_18_description, "shaders/local_color_difference_debug/demo_18_edge_difference_debug.frag"),
                ShaderDemo(R.string.demo_19_title, R.string.demo_19_description, "shaders/local_color_difference_debug/demo_19_warm_skin_smoothing.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_20_title, R.string.demo_20_description, "shaders/local_color_difference_debug/demo_20_local_warm_tone_debug.frag", showBlurStrengthControl = true),
                ShaderDemo(R.string.demo_21_title, R.string.demo_21_description, "shaders/local_color_difference_debug/demo_21_warmth_strength_uniform.frag", showBlurStrengthControl = true, showWarmthStrengthControl = true),
                // 第四章继续覆盖亮度与色度调试及局部调色。
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
        // 第五章图像输入练习放在人脸分析与区域遮罩之前，保持输入坐标到区域处理的学习顺序。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_image_input_and_color_management,
            demos = listOf(
                // Demo 30 归档 UV 旋转与前摄镜像版本；当前没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_30_title, R.string.demo_30_description, "shaders/image_input_and_color_management/demo_30_uv_rotate_mirror.frag"),
                // Demo 31 归档当前 main.frag 的 UV 方向标记版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_31_title, R.string.demo_31_description, "shaders/image_input_and_color_management/demo_31_uv_orientation_marker.frag"),
                // Demo 32 归档当前 main.frag 的 YUV 色度采样版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_32_title, R.string.demo_32_description, "shaders/image_input_and_color_management/demo_32_yuv_chroma_subsampling.frag"),
                // Demo 33 归档当前 main.frag 的 RGB/YUV 往返转换版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_33_title, R.string.demo_33_description, "shaders/image_input_and_color_management/demo_33_rgb_yuv_roundtrip.frag"),
                // Demo 34 归档当前 main.frag 的 Gamma 亮度对比版本；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_34_title, R.string.demo_34_description, "shaders/image_input_and_color_management/demo_34_gamma_brightness_comparison.frag"),
                // Demo 35 归档当前 main.frag 的白平衡暖色校正版本；声明的调节 uniform 未参与最终输出，因此不展示滑条。
                ShaderDemo(R.string.demo_35_title, R.string.demo_35_description, "shaders/image_input_and_color_management/demo_35_white_balance_warm_correction.frag"),
                // Demo 36 归档白平衡与曝光补偿三栏对比；没有实际使用的调节 uniform，因此不展示滑条。
                ShaderDemo(R.string.demo_36_title, R.string.demo_36_description, "shaders/image_input_and_color_management/demo_36_white_balance_exposure_compensation.frag"),
                // Demo 37 输出模拟偏色图及 RGB 通道灰度；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_37_title, R.string.demo_37_description, "shaders/image_input_and_color_management/demo_37_rgb_channel_debug.frag"),
            ),
            initiallyExpanded = false,
        ),
        // 第六章的人脸分析与区域遮罩 Demo 统一放在独立分组中。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_face_analysis_and_mask,
            demos = listOf(
                // Demo 38 用四条 UV 边界输出矩形遮罩；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_38_title, R.string.demo_38_description, "shaders/face_analysis_and_mask/demo_38_rectangular_region_mask.frag"),
                // Demo 39 用 smoothstep 羽化矩形边缘；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_39_title, R.string.demo_39_description, "shaders/face_analysis_and_mask/demo_39_feathered_rectangle_mask.frag"),
                // Demo 40 用羽化矩形遮罩混合固定提亮结果；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_40_title, R.string.demo_40_description, "shaders/face_analysis_and_mask/demo_40_feathered_rectangle_brightening.frag"),
                // Demo 41 输出 HSV 肤色候选灰度遮罩；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_41_title, R.string.demo_41_description, "shaders/face_analysis_and_mask/demo_41_hsv_skin_candidate_mask.frag"),
                // Demo 42 联合 HSV 肤色候选与羽化矩形区域；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_42_title, R.string.demo_42_description, "shaders/face_analysis_and_mask/demo_42_skin_region_combined_mask.frag"),
                // Demo 43 使用手工中心和宽高显示人脸框；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_43_title, R.string.demo_43_description, "shaders/face_analysis_and_mask/demo_43_manual_face_box_mask.frag"),
                // Demo 44 将模拟像素坐标转为 UV 并标记红点；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_44_title, R.string.demo_44_description, "shaders/face_analysis_and_mask/demo_44_pixel_to_uv_marker.frag"),
                // Demo 45 显示手工左眼中心的归一化椭圆距离；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_45_title, R.string.demo_45_description, "shaders/face_analysis_and_mask/demo_45_eye_ellipse_distance.frag"),
                // Demo 46 只保留 main 方法及其实际引用的纹理和坐标声明；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_46_title, R.string.demo_46_description, "shaders/face_analysis_and_mask/demo_46_bilateral_eye_ellipse_mask.frag"),
                // Demo 47 只保留 main 方法及其实际调用的脸框、五官和肤色候选依赖；调节 uniform 未参与输出，不展示滑条。
                ShaderDemo(R.string.demo_47_title, R.string.demo_47_description, "shaders/face_analysis_and_mask/demo_47_protected_face_skin_mask.frag"),
                // Demo 48 覆盖为当前 main.frag 的练习；最终输出使用黑眼圈强度，因此只显示对应滑条。
                ShaderDemo(
                    R.string.demo_48_title,
                    R.string.demo_48_description,
                    "shaders/face_analysis_and_mask/demo_48_under_eye_black_circle_brightening.frag",
                    initialBlackCircleStrength = 0f,
                    showBlackCircleStrengthControl = true,
                ),
                // Demo 49 使用 SDK 双眼中心与半径；最终输出受黑眼圈强度控制，因此保留对应滑条。
                ShaderDemo(
                    R.string.demo_49_title,
                    R.string.demo_49_description,
                    "shaders/face_analysis_and_mask/demo_49_sdk_eye_under_eye_brightening.frag",
                    initialBlackCircleStrength = 0f,
                    showBlackCircleStrengthControl = true,
                ),
            ),
            initiallyExpanded = false,
        ),
        // 第七章从固定中心、半径和偏移量开始练习局部 UV 形变。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_face_shaping,
            demos = listOf(
                // Demo 50 使用固定 UV 偏移，没有调节 uniform，因此不显示滑条。
                ShaderDemo(
                    R.string.demo_50_title,
                    R.string.demo_50_description,
                    "shaders/face_shaping/demo_50_local_uv_shift.frag",
                ),
                // Demo 51 在固定中心和半径基础上，给 UV 偏移增加平滑衰减。
                ShaderDemo(
                    R.string.demo_51_title,
                    R.string.demo_51_description,
                    "shaders/face_shaping/demo_51_local_uv_shift_falloff.frag",
                ),
                // Demo 52 使用固定左脸颊中心、半径和偏移量，不需要调节滑条。
                ShaderDemo(
                    R.string.demo_52_title,
                    R.string.demo_52_description,
                    "shaders/face_shaping/demo_52_local_cheek_uv_shift.frag",
                ),
                // Demo 53 归档当前双眼大眼版本，最终输出使用 bigEyeStrength，因此保留大眼滑条。
                ShaderDemo(
                    R.string.demo_53_title,
                    R.string.demo_53_description,
                    "shaders/face_shaping/demo_53_sdk_both_eye_uv_scale.frag",
                    initialBigEyeStrength = 0f,
                    showBigEyeStrengthControl = true,
                ),
                // Demo 54 归档当前 main.frag 的双眼放大与轮廓瘦脸版本，因此保留两个实际参与输出的滑条。
                ShaderDemo(
                    R.string.demo_54_title,
                    R.string.demo_54_description,
                    "shaders/face_shaping/demo_54_sdk_eye_and_slim_face_uv_warp.frag",
                    initialBigEyeStrength = 0f,
                    showBigEyeStrengthControl = true,
                    initialSlimFaceStrength = 0f,
                    showSlimFaceStrengthControl = true,
                ),
                // Demo 55 在大眼和瘦脸形变后增加 UV 边界保护，因此保留两个实际参与输出的滑条。
                ShaderDemo(
                    R.string.demo_55_title,
                    R.string.demo_55_description,
                    "shaders/face_shaping/demo_55_sdk_eye_slim_face_uv_bounds.frag",
                    initialBigEyeStrength = 0f,
                    showBigEyeStrengthControl = true,
                    initialSlimFaceStrength = 0f,
                    showSlimFaceStrengthControl = true,
                ),
            ),
            initiallyExpanded = false,
        ),
        // 第八章从 SDK 嘴唇区域和目标色混合开始练习妆容。
        ShaderDemoGroup(
            titleRes = R.string.demo_group_makeup,
            demos = listOf(
                // Demo 56 使用 SDK 外唇轮廓计算权重并混合固定口红色，保留实际使用的口红滑条。
                ShaderDemo(
                    R.string.demo_56_title,
                    R.string.demo_56_description,
                    "shaders/makeup/demo_56_sdk_lipstick_mix.frag",
                    initialLipstickStrength = 0f,
                    showLipstickStrengthControl = true,
                ),
                // Demo 57 使用外唇与内嘴多边形轮廓生成口红区域，保留实际使用的口红滑条。
                ShaderDemo(
                    R.string.demo_57_title,
                    R.string.demo_57_description,
                    "shaders/makeup/demo_57_outer_inner_lip_polygon_mask.frag",
                    initialLipstickStrength = 0f,
                    showLipstickStrengthControl = true,
                ),
                // Demo 58 使用平滑后的嘴唇轮廓和包围盒保护，口红强度仍参与最终输出。
                ShaderDemo(
                    R.string.demo_58_title,
                    R.string.demo_58_description,
                    "shaders/makeup/demo_58_smoothed_lip_contour_feather.frag",
                    initialLipstickStrength = 0f,
                    showLipstickStrengthControl = true,
                ),
                // Demo 59 将当前 main.frag 的双侧关键点腮红练习保存为独立 Shader。
                ShaderDemo(
                    R.string.demo_59_title,
                    R.string.demo_59_description,
                    "shaders/makeup/demo_59_landmark_dual_cheek_blush_gradient.frag",
                    initialBlushStrength = 0f,
                    showBlushStrengthControl = true,
                    initialBlushRange = 0.4f,
                    showBlushRangeControl = true,
                ),
            ),
            initiallyExpanded = false,
        ),
    )
}
