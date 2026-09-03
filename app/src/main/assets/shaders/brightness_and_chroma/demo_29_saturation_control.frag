precision mediump float;
uniform sampler2D inputImageTexture;
uniform float warmthStrength;
// 独立接收饱和度强度，避免与补暖强度共用同一个参数。
uniform float saturationStrength;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);
// 补暖色调公式(单位)
vec3 redFix = vec3(1.0, 0.0, -0.2126 / 0.0722);

// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;

// 有效阴影带,阴影保护 (亮度保护)
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

// 饱和度保护
// HSV 饱和度保护：0.55 开始保护，0.85 完全停止额外增饱和。
float SATURATION_START = 0.55;
float SATURATION_END = 0.85;

// 肤色的红蓝色差 - 肤色识别
float SKIN_DIFF_RB_START = 0.04;
float SKIN_DIFF_RB_END = 0.16;

// 肤色的红绿色差 - 肤色识别
float SKIN_DIFF_RG_START = 0.00;
float SKIN_DIFF_RG_END = 0.08;

float getSaturation(vec3 color);

void main() {
    // 原图直通：采样结果不做任何颜色处理。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);

    // 拿出肤色权重
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, centerColor.r - centerColor.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, centerColor.r - centerColor.g);
    float skinWeight = min(rbWeight, rgWeight);
    // 计算亮度
    float luminance = dot(centerColor.rgb, lightRec709);
    // 常光权重（排除高光）
    float norLightWeight = 1.0 - smoothstep(HIGHTLIGHT_START, HIGHTLIGHT_END, luminance);
    // 阴影权重（非纯黑）
    float shadowWeight = smoothstep(SHADOW_START, SHADOW_END, luminance);
    //暖色补正值
    float warmth = warmthStrength * 0.01;
    // 饱和度
    float saturation = getSaturation(centerColor.rgb);
    // 需补全的饱和权重
    float saturationDiffWeight = 1.0 - smoothstep(SATURATION_START, SATURATION_END, saturation);
    // 色差 用于处理饱和度
    vec3 colorDiff = centerColor.rgb - vec3(luminance);
    // 增强饱和度后的色差值
    vec3 saturationColor = colorDiff * saturationStrength * saturationDiffWeight;
    // 需要补暖的值
    vec3 warmthColor = redFix * warmth * skinWeight * norLightWeight * shadowWeight;
    // 最终输出色
    vec3 resultColor = clamp(centerColor.rgb + warmthColor + saturationColor, vec3(0.0), vec3(1.0));
    // 输出
    gl_FragColor = vec4(resultColor, centerColor.a);

}

float getSaturation(vec3 color){
    float maxChannel = max(color.r, max(color.b, color.g));
    float minChannel = min(color.r, min(color.b, color.g));
    return (maxChannel - minChannel) / max(maxChannel, 0.0001);
}
