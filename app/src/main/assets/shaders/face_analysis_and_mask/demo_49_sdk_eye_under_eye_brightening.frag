precision mediump float;
uniform sampler2D inputImageTexture;
// 黑眼圈滑条控制下眼区域的提亮幅度。
uniform float blackCircleStrength;
varying vec2 textureCoordinate;

// SDK 眼部轮廓计算出的原始半径。
uniform vec2 leftEyeRadius;
uniform vec2 rightEyeRadius;
// SDK 检测得到的左右眼中心。
uniform vec2 leftEyeCenter;
uniform vec2 rightEyeCenter;

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

void main() {
    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // 原始轮廓只包含眼睛本体，扩张后作为眼周处理范围。
    vec2 leftEyeMaskRadius = leftEyeRadius * vec2(eyeXRatio, eyeYRatio);
    vec2 rightEyeMaskRadius = rightEyeRadius * vec2(eyeXRatio,eyeYRatio);

    float lipWeight = getZoneWeight(lipCenter, lipRadius);
    float leftEyeWeight = getZoneWeight(leftEyeCenter, leftEyeMaskRadius);
    float rightEyeWeight = getZoneWeight(rightEyeCenter, rightEyeMaskRadius);
    // 如果在其中一个区域内 则会返回有值
    float featureWeight = max(max(leftEyeWeight, rightEyeWeight), lipWeight);
    // 反转五官权重，眼睛和嘴唇变为受保护区域。
    float featureProtectWeight = 1.0 - featureWeight;
    // 获取面部区域
    float faceBoxWeight = getFaceBoxWeight();
    // 只保留人脸框内部，同时排除眼睛和嘴唇。
    float protectedFaceWeight = faceBoxWeight * featureProtectWeight;

    // 获取当前片元的肤色候选权重。
    float skinCandidateWeight = getSkinCandidateWeight(sourceColor.rgb);

    // 同时满足：属于肤色、在人脸框内、不在眼睛和嘴唇内。
    float finalSkinWeight = skinCandidateWeight * protectedFaceWeight;

    // UV 的 y 向下增大：眼睛中心以下逐渐允许处理。
    float leftLowerWeight = smoothstep(leftEyeCenter.y, leftEyeCenter.y + leftEyeMaskRadius.y * 0.7, textureCoordinate.y);
    float rightLowerWeight = smoothstep(rightEyeCenter.y, rightEyeCenter.y + rightEyeMaskRadius.y * 0.7, textureCoordinate.y);

    // 完整眼周椭圆乘以下方限制，只保留下眼区域。
    float leftUnderEyeWeight = leftEyeWeight * leftLowerWeight;
    float rightUnderEyeWeight = rightEyeWeight * rightLowerWeight;
    float underEyeWeight = max(leftUnderEyeWeight, rightUnderEyeWeight);

    // 只在脸框内、符合肤色的下眼区域进行处理。
    float underEyeCorrectionWeight = underEyeWeight * skinCandidateWeight * faceBoxWeight;
    // 黑眼圈强度控制下眼区域的提亮幅度。
    vec3 correctedColor = sourceColor.rgb + vec3(blackCircleStrength) * underEyeCorrectionWeight ;

    gl_FragColor = vec4(correctedColor, sourceColor.a);
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

float getSaturation(vec3 color){
    float maxChannel = max(max(color.r, color.g), color.b);
    float minChannel = min(min(color.r, color.g), color.b);
    return (maxChannel - minChannel) / max(maxChannel, 0.0001);
}
