precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
varying vec2 textureCoordinate;

void main() {
    // 右半屏统一混入 15% 白色，不区分皮肤区域。
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);
    vec3 whitenedRgb = mix(originalColor.rgb, vec3(1.0), whitenStrength);
    if (textureCoordinate.x > 0.5) {
        gl_FragColor = vec4(whitenedRgb, originalColor.a);
    } else {
        gl_FragColor = originalColor;
    }
}
