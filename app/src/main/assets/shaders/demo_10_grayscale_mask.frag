precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
varying vec2 textureCoordinate;

void main() {
    // Demo 10-1 与 10-2 共用同一个输出流程，只替换 skinWeight 的计算方式。
    bool isRightSide = textureCoordinate.x >= 0.5;
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);

    // 10-1：只根据红蓝差生成连续权重。
    // float redBlueDifference = originalColor.r - originalColor.b;
    // float skinWeight = smoothstep(0.04, 0.16, redBlueDifference);

    // 10-2：同时考虑红蓝差和红绿差，取两种权重中的较小值。
    float redBlueWeight = smoothstep(0.04, 0.16, originalColor.r - originalColor.b);
    float redGreenWeight = smoothstep(0.00, 0.08, originalColor.r - originalColor.g);
    float skinWeight = min(redBlueWeight, redGreenWeight);
    // 10-3：考虑亮度
    // 取亮度 vec3就当做默认的亮度贡献权重 固定写法
    float brightness = dot(originalColor.rgb, vec3(0.299, 0.587, 0.114));
    float highlightProtection = 1.0 - smoothstep(0.65, 0.90, brightness);
    float whiteningWeight = skinWeight * highlightProtection * whitenStrength;
    vec3 whiteMix = mix(originalColor.rgb, vec3(1.0), whiteningWeight);


    if (isRightSide) {
        // 需要直接观察灰度 mask 时，可暂时改为 vec4(vec3(skinWeight), originalColor.a)。
//        vec3 whiteMix = mix(originalColor.rgb, vec3(1.0), skinWeight * 0.2);
        gl_FragColor = vec4(whiteMix, originalColor.a);
//        gl_FragColor = vec4(vec3(skinWeight), originalColor.a);
    } else {
        gl_FragColor = originalColor;
    }
}
