precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // RGB 提升 20%，并限制到合法颜色上限 1.0。
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);
    vec3 brighterRgb = min(originalColor.rgb * 1.2, vec3(1.0));
    gl_FragColor = vec4(brighterRgb, originalColor.a);
}
