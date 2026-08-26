precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
varying vec2 textureCoordinate;

void main() {
    // 用最简单的 RGB 大小关系近似筛选肤色，只处理右半屏。
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);
    bool looksLikeSkin = originalColor.r > originalColor.g && originalColor.g > originalColor.b;
    if (textureCoordinate.x > 0.5 && looksLikeSkin) {
        vec3 whitenedRgb = mix(originalColor.rgb, vec3(1.0), whitenStrength);
        gl_FragColor = vec4(whitenedRgb, originalColor.a);
    } else {
        gl_FragColor = originalColor;
    }
}
