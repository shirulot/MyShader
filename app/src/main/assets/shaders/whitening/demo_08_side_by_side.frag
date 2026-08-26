precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
varying vec2 textureCoordinate;

void main() {
    // 左右半屏分别映射到完整横向 UV，便于并排观察同一张图。
    vec2 compareCoordinate = vec2(fract(textureCoordinate.x * 2.0), textureCoordinate.y);
    vec4 originalColor = texture2D(inputImageTexture, compareCoordinate);
    bool isRightSide = textureCoordinate.x >= 0.5;
    bool looksLikeSkin = originalColor.r > originalColor.g && originalColor.g > originalColor.b;

    vec3 resultRgb = originalColor.rgb;
    if (isRightSide && looksLikeSkin) {
        resultRgb = mix(originalColor.rgb, vec3(1.0), whitenStrength);
    }
    gl_FragColor = vec4(resultRgb, originalColor.a);
}
