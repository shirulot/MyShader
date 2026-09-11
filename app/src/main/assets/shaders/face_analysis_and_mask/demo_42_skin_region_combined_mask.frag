precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
uniform float brightenStrength;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// hue 肤色判断
float SKIN_HUE_START = 0.08;
float SKIN_HUE_END = 0.14;

// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

float SKIN_SATURATION_START = 0.08;
float SKIN_SATURATION_END = 0.20;

float getHue(vec3 color);
float getSaturation(vec3 color);

void main() {
    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // x 到达 0.25 后，左边界权重变为 1。
    float leftWeight = smoothstep(0.2, 0.25, textureCoordinate.x);
    // x 到达 0.75 后，右边界权重变回 0
    float rightWeight = 1.0 - smoothstep(0.75, 0.8, textureCoordinate.x);
    // y 达到0.25后下方变为1(白色)
    float topWeight = smoothstep(0.2, 0.25, textureCoordinate.y);
    // y 达到0.75后下方变为1(白色) 然后取反 上方变为白色下方变为黑色
    float bottomWeight = 1.0 - smoothstep(0.75, 0.8, textureCoordinate.y);
    // 四个边界同时满足时，区域权重才为 1。
    float regionWeight = leftWeight * rightWeight * topWeight * bottomWeight;

    // 制造一个容易观察的局部提亮颜色。
    vec3 brightenedColor = clamp(sourceColor.rgb + vec3(0.25), 0.0, 1.0);
    // 使用区域遮罩控制原图与提亮颜色的混合比例。
    vec3 resultColor = mix(sourceColor.rgb, brightenedColor, regionWeight);
    // 读取当前像素的色相。
    float hue = getHue(sourceColor.rgb);
    // 选择红色到橙黄色附近，并在边界处平滑衰减。
    float hueWeight = 1.0 - smoothstep(SKIN_HUE_START, SKIN_HUE_END, hue);

    // 根据 RGB 最大值和最小值计算 HSV 饱和度 S。
    float saturation = getSaturation(sourceColor.rgb);
    // 低饱和度像素权重为 0，有明显颜色后逐渐变为 1。
    float saturationWeight = smoothstep(SKIN_SATURATION_START, SKIN_SATURATION_END, saturation);

    // HSV 的 V 等于 RGB 三个通道中的最大值。
    float value = max(max(sourceColor.r, sourceColor.g), sourceColor.b);
    // 阴影保护:太暗时权重接近 0，进入正常亮度后逐渐变为 1。
    float shadowWeight = smoothstep(SHADOW_START, SHADOW_END, value);
    // 高光保护:太亮时权重从 1 逐渐降到 0。
    float highlightWeight = 1.0 - smoothstep(HIGHTLIGHT_START, HIGHTLIGHT_END, value);
    // 像素必须同时满足 H、S 和 V 条件。
    float skinCandidate = hueWeight * saturationWeight * shadowWeight * highlightWeight;
    // 同时满足肤色条件和空间区域条件，才是最终候选。
    float finalSkinWeight = skinCandidate * regionWeight;
    // 用灰度显示颜色与位置联合后的权重。
    gl_FragColor = vec4(vec3(finalSkinWeight), sourceColor.a);
}
// 详细查看 [res/drawable/hue_color_ring.png]
// 将 RGB 转换为 0--1 范围的 HSV 色相 H。
float getHue(vec3 color) {
    // 取到通道最大值
    float maxValue = max(max(color.r, color.g), color.b);
    // 取到通道最小值
    float minValue = min(min(color.r, color.g), color.b);
    // 三通道最大差
    float delta = maxValue - minValue;
    //最大值与最小值几乎相等时属于无彩色，H 没有定义，这里安全返回 0。是灰/白/黑 三色
    if (delta < 0.0001) return 0.0;
    // 如果最大为红 则去计算他是偏向正负 正则为色环右方的黄色 负则为左边的品红 这里的mod单纯用来校正负值 后续不需要因为后续不是从0开始
    if (maxValue == color.r) return mod((color.g - color.b) / delta, 6.0) / 6.0;
    // 如果最大为绿 则去计算他是偏向正负 正则为色环右方的青色 负则为左边的黄色 +2是因为移动到绿区块
    if (maxValue == color.g) return ((color.b - color.r) / delta + 2.0) / 6.0;
    // 如果最大为蓝 则去计算他是偏向正负 正则为色环右方的品红 负则为左边的青色 +4是因为移动到蓝区块
    return ((color.r - color.g) / delta + 4.0) / 6.0;
}

float getSaturation(vec3 color){
    float maxChannel = max(max(color.r, color.g), color.b);
    float minChannel = min(min(color.r, color.g), color.b);
    return (maxChannel - minChannel) / max(maxChannel, 0.0001);
}
