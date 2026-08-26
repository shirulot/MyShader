precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // RGB 降为一半，alpha 保持不变。
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);
    vec3 darkerRgb = originalColor.rgb * 0.5;
    gl_FragColor = vec4(darkerRgb, originalColor.a);
}
