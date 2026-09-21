precision mediump float;
uniform sampler2D inputImageTexture;
// 口红滑条控制目标色混入原图的强度，范围由 Android 端限制为 0.00--1.00，步长为 0.01。
uniform float lipstickStrength;
varying vec2 textureCoordinate;

// SDK 外唇轮廓计算出的中心和半径。
uniform vec2 lipCenter;
uniform vec2 lipRadius;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

void main() {
    // 读取原图 alpha，保持 inputImageTexture 处于实际使用状态。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // 用嘴唇横纵半径归一化偏移量，并避免除以零。
    vec2 lipOffset = (textureCoordinate - lipCenter) / max(lipRadius, vec2(0.001));
    // 中心距离为零，椭圆边界距离为一。
    float lipDistance = length(lipOffset);
    // 缩窄边缘羽化范围，让目标色覆盖更多嘴唇区域。
    float lipWeight = (1.0 - smoothstep(0.90, 1.0, lipDistance)) * faceCenterReady;
    // 选择一个固定的口红目标色。
    vec3 lipstickColor = vec3(0.78, 0.06, 0.16);
    // 再次限制外部参数，避免异常值让颜色混合超出预期。
    float safeLipstickStrength = clamp(lipstickStrength, 0.0, 1.0);
    // 按嘴唇权重把原图与目标色混合。
    vec3 resultColor = mix(sourceColor.rgb, lipstickColor, lipWeight * safeLipstickStrength);
    // 输出混合结果，保留原图透明度。
    gl_FragColor = vec4(resultColor, sourceColor.a);
}
