precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;

// 黑眼圈滑条控制下眼区域的提亮幅度。
uniform float blackCircleStrength;
// 大眼滑条控制眼睛区域的放大强度，范围由 Android SeekBar 限制为 0.00--0.15，步长为 0.005。
uniform float bigEyeStrength;
// 瘦脸滑条控制轮廓向内部收缩的强度，范围为 0.00--0.05，步长为 0.005。
uniform float slimFaceStrength;
varying vec2 textureCoordinate;

// SDK 眼部轮廓计算出的原始半径。
uniform vec2 leftEyeRadius;
uniform vec2 rightEyeRadius;
// SDK 检测得到的左右眼中心。
uniform vec2 leftEyeCenter;
uniform vec2 rightEyeCenter;

// SDK 轮廓起点及其对应的脸部内部目标点。
uniform vec2 slimOrigins[9];
uniform vec2 slimTargets[9];

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// 外部分析得到的人脸框中心。
uniform vec2 faceCenter;
// 外部分析得到的人脸框宽高。
uniform vec2 faceSize;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;


// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

// hue 肤色色相判断
float SKIN_HUE_START = 0.08;
float SKIN_HUE_END = 0.14;

// Saturation 肤色饱和度判断
float SKIN_SATURATION_START = 0.08;
float SKIN_SATURATION_END = 0.20;

float eyeXRatio = 2.0;
float eyeYRatio = 4.5;
// SDK 外唇轮廓计算出的中心和半径。
uniform vec2 lipCenter;
uniform vec2 lipRadius;

float getHue(vec3 color);
float getSaturation(vec3 color);

float getFaceBoxWeight(){
    // 计算人脸框的左右、上下边界。
    vec2 faceMin = faceCenter - faceSize * 0.5;
    vec2 faceMax = faceCenter + faceSize * 0.5;

    // 当前片元同时位于四条边界内时，人脸框权重为 1。
    float faceBoxWeight =
    step(faceMin.x, textureCoordinate.x) *
    step(textureCoordinate.x, faceMax.x) *
    step(faceMin.y, textureCoordinate.y) *
    step(textureCoordinate.y, faceMax.y) *
    faceCenterReady;
    return faceBoxWeight;
}

float getZoneWeight(vec2 center, vec2 radius){
    // textureCoordinate - eyeCenter 求出坐标的xy差 也就是当前片元距离眼睛中心点的偏移量
    // / radius 归一化 /当前倍率之后得出 用length计算距离 最小的为0黑色 眼中心点 逐渐变大 最后到1.0 这部分都是黑色渐变 大于等于1部分最终现实为白色
    vec2 offset = (textureCoordinate - center) / radius;
    // 获取长度
    float zoneDistance = length(offset);
    // 将眼周距离转换为带羽化边缘的遮罩。
    return 1.0 - smoothstep(0.75, 1.0, zoneDistance);
}

// 获取皮肤颜色权重
float getSkinCandidateWeight(vec3 color) {
    float hue = getHue(color);
    float saturation = getSaturation(color);
    float value = max(max(color.r, color.g), color.b);

    // 限制肤色色相范围。
    float hueWeight = 1.0 - smoothstep(SKIN_HUE_START, SKIN_HUE_END, hue);
    // 排除饱和度过低的灰、白、黑区域。
    float saturationWeight = smoothstep(SKIN_SATURATION_START, SKIN_SATURATION_END, saturation);
    // 保护过暗阴影和过亮高光。
    float shadowWeight = smoothstep(SHADOW_START, SHADOW_END, value);
    float highlightWeight = 1.0 - smoothstep(HIGHTLIGHT_START, HIGHTLIGHT_END, value);

    return hueWeight * saturationWeight * shadowWeight * highlightWeight;
}

// 获取大眼
vec2 getBigEyeUv(vec2 uv, vec2 radius, vec2 center){
    // 使用 SDK 左眼中心和轮廓半径建立椭圆形影响区域。
    vec2 eyeOffset = uv - center;
    vec2 effectRadius = max(radius * 2.0, vec2(0.001));
    float eyeDistance = length(eyeOffset / effectRadius);
    float eyeWeight = (1.0 - smoothstep(0.0, 1.0, eyeDistance)) * faceCenterReady;
    // 让采样位置向眼睛中心收缩，使眼睛内容向外放大。
    float eyeScale = 1.0 - bigEyeStrength * eyeWeight;
    return center + eyeOffset * eyeScale;
}

// 根据真实轮廓起点和目标点计算局部瘦脸采样坐标。
vec2 getSlimFaceUv(vec2 uv, vec2 origin, vec2 target) {
    vec2 direction = target - origin;
    // 暂用两点间的 UV 距离作为影响半径，并避免零半径。
    float radius = max(length(direction), 0.001);
    float weight = 1.0 - smoothstep(0.0, radius, length(uv - origin));
    // 沿目标方向的反方向采样，使轮廓内容向目标方向收缩。
    return uv - direction * slimFaceStrength * weight * faceCenterReady;
}

// 检查是否溢出UV 溢出 0 不溢出 1
float insideUv(vec2 value){
    return step(0.0, value.x) * step(value.x, 1.0) * step(0.0, value.y) * step(value.y, 1.0);
}

void main() {
    vec2 leftEyeUv = getBigEyeUv(textureCoordinate, leftEyeRadius, leftEyeCenter);
    vec2 rightEyeUv = getBigEyeUv(leftEyeUv, rightEyeRadius, rightEyeCenter);
    // 从双眼处理后的采样坐标开始。
    vec2 sampledUv = rightEyeUv;
    // 依次应用全部九组轮廓形变，每组接收上一组的结果。
    for (int i = 0; i < 8; i++) {
        sampledUv = getSlimFaceUv(sampledUv, slimOrigins[i], slimTargets[i]);
    }
    // 判断形变后的采样坐标是否仍在纹理范围内，包含边界。
    float inside = insideUv(sampledUv);
    // 越界时读取原图对应位置，有效时使用形变后的采样坐标。
    vec2 safeUv = mix(textureCoordinate, sampledUv, inside);
    gl_FragColor = texture2D(inputImageTexture, safeUv);
}

// 详细查看 [res/drawable/hue_color_ring.png]
// 将 RGB 转换为 0--1 范围的 HSV 色相 H。
float getHue(vec3 color) {
    // 取到通道最大值
    float maxValue = max(max(color.r, color.g), color.b);
    // 取到通道最小值
    float minValue = min(min(color.r, color.g), color.b);
    // 三通道最大差 同时也是value
    float delta = maxValue - minValue;
    //最大值与最小值几乎相等时属于无彩色,H 没有定义，这里安全返回 0。是灰/白/黑 三色
    if (delta < 0.0001) return 0.0;
    // 如果最大为红 则去计算他是偏向正负 正则为色环右方的黄色 负则为左边的品红 这里的mod单纯用来校正负值 后续不需要因为后续不是从0开始
    if (maxValue == color.r) return mod((color.g - color.b) / delta, 6.0) / 6.0;
    // 如果最大为绿 则去计算他是偏向正负 正则为色环右方的青色 负则为左边的黄色 +2是因为移动到绿区块
    if (maxValue == color.g) return ((color.b - color.r) / delta + 2.0) / 6.0;
    // 如果最大为蓝 则去计算他是偏向正负 正则为色环右方的品红 负则为左边的青色 +4是因为移动到蓝区块
    return ((color.r - color.g) / delta + 4.0) / 6.0;
}
// 饱和度
float getSaturation(vec3 color){
    float maxChannel = max(max(color.r, color.g), color.b);
    float minChannel = min(min(color.r, color.g), color.b);
    return (maxChannel - minChannel) / max(maxChannel, 0.0001);
}
