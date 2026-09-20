precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // 手工指定左脸轮廓附近的形变中心。
    vec2 leftCheekPoint = vec2(0.32, 0.55);
    float warpRadius = 0.16;
    float distanceToCheek = length(textureCoordinate - leftCheekPoint);
    float warpWeight = 1.0 - smoothstep(0.0, warpRadius, distanceToCheek);

    // 采样向左移动，使看到的左脸内容向右收缩。
    vec2 sampleOffset = vec2(-0.035, 0.0) * warpWeight;
    vec2 sampledUv = textureCoordinate + sampleOffset;
    gl_FragColor = texture2D(inputImageTexture, sampledUv);
}
