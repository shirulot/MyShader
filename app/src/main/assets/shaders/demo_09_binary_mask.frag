precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // 左侧保留原图，右侧用黑白显示暖色条件选中的 mask。
    bool isRightSide = textureCoordinate.x >= 0.5;
    vec4 originalColor = texture2D(inputImageTexture, textureCoordinate);
    bool looksLikeSkin =
        originalColor.r > originalColor.g
        && originalColor.g > originalColor.b
        && originalColor.r - originalColor.b > 0.15;

    if (isRightSide) {
        // 白色表示被当前暖色条件选中的像素。
        if (looksLikeSkin) {
            gl_FragColor = vec4(vec3(1.0), originalColor.a);
        } else {
            gl_FragColor = vec4(vec3(0.0), originalColor.a);
        }
    } else {
        gl_FragColor = originalColor;
    }
}
