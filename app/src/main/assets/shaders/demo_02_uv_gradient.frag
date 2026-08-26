precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // x 映射红色、y 映射绿色，直观看到 UV 从 0.0 到 1.0 的变化。
    float originalAlpha = texture2D(inputImageTexture, textureCoordinate).a;
    gl_FragColor = vec4(textureCoordinate, 0.0, originalAlpha);
}
