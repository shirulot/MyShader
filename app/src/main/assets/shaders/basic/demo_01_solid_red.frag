precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // 保留原图 alpha，同时把 RGB 固定为红色。
    float originalAlpha = texture2D(inputImageTexture, textureCoordinate).a;
    gl_FragColor = vec4(1.0, 0.0, 0.0, originalAlpha);
}
