precision mediump float;
uniform sampler2D inputImageTexture;
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

// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

// 获取大眼
vec2 getBigEyeUv(vec2 uv, vec2 radius, vec2 center){
    // 使用 SDK 左眼中心和轮廓半径建立椭圆形影响区域。
    vec2 eyeOffset = uv - center;
    vec2 effectRadius = max(radius * 2.0, vec2(0.001));
    float eyeDistance = length(eyeOffset / effectRadius);
    float eyeWeight = (1.0 - smoothstep(0.0, 1.0, eyeDistance)) * faceCenterReady;
    float safeBigEyeStrength = clamp(bigEyeStrength, 0.0, 0.15);
    // 让采样位置向眼睛中心收缩，使眼睛内容向外放大。
    float eyeScale = 1.0 - safeBigEyeStrength * eyeWeight;
    return center + eyeOffset * eyeScale;
}

// 根据真实轮廓起点和目标点计算局部瘦脸采样坐标。
vec2 getSlimFaceUv(vec2 uv, vec2 origin, vec2 target) {
    vec2 direction = target - origin;
    // 暂用两点间的 UV 距离作为影响半径，并避免零半径。
    float radius = max(length(direction), 0.001);
    float weight = 1.0 - smoothstep(0.0, radius, length(uv - origin));
    float safeSlimFaceStrength = clamp(slimFaceStrength, 0.0, 0.05);
    // 沿目标方向的反方向采样，使轮廓内容向目标方向收缩。
    return uv - direction * safeSlimFaceStrength * weight * faceCenterReady;
}

// 检查是否溢出UV 溢出 0 不溢出 1
float insideUv(vec2 value){
    return step(0.0, value.x) * step(value.x, 1.0) * step(0.0, value.y) * step(value.y, 1.0);
}

void main() {
    // 左右眼的大眼效果
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
