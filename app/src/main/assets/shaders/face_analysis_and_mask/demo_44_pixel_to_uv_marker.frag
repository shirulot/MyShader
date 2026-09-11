precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
uniform float brightenStrength;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// 外部分析得到的人脸框中心。
uniform vec2 faceCenter;
// 外部分析得到的人脸框宽高。
uniform vec2 faceSize;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

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
    // 模拟分析工具返回的图片像素坐标。
    vec2 analysisPointPx = vec2(410.0, 645.0);
    // 当前输入图片的真实像素尺寸。
    vec2 analysisImageSize = vec2(1024.0, 1536.0);
    // 将像素坐标转换为 0--1 纹理坐标。
    vec2 texturePoint = analysisPointPx / analysisImageSize;
    // 将转换后的关键点显示为红色圆点。
    float pointWeight = 1.0 - smoothstep(0.015, 0.020, distance(textureCoordinate, texturePoint));
    vec3 debugColor = mix(sourceColor.rgb, vec3(1.0, 0.0, 0.0), pointWeight);
    gl_FragColor = vec4(debugColor, sourceColor.a);

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
