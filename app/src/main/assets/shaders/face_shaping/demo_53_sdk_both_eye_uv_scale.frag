precision mediump float;
uniform sampler2D inputImageTexture;
// 大眼滑条控制眼睛区域的放大强度，范围由 Android SeekBar 限制为 0.00--0.15，步长为 0.005。
uniform float bigEyeStrength;
varying vec2 textureCoordinate;

// SDK 眼部轮廓计算出的原始半径。
uniform vec2 leftEyeRadius;
uniform vec2 rightEyeRadius;
// SDK 检测得到的左右眼中心。
uniform vec2 leftEyeCenter;
uniform vec2 rightEyeCenter;

// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

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

void main() {
    vec2 leftEyeUv = getBigEyeUv(textureCoordinate, leftEyeRadius, leftEyeCenter);
    vec2 rightEyeUv = getBigEyeUv(leftEyeUv, rightEyeRadius, rightEyeCenter);
    gl_FragColor = texture2D(inputImageTexture, rightEyeUv);
}
