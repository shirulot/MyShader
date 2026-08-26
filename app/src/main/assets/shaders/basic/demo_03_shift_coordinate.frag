precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // 采样坐标向右偏移 10%，越界区域由 GL_CLAMP_TO_EDGE 补齐。
    vec2 shiftedCoordinate = textureCoordinate + vec2(0.1, 0.0);
    gl_FragColor = texture2D(inputImageTexture, shiftedCoordinate);
}
