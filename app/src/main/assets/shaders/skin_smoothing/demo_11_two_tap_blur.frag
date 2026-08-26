precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // demo11
    // 读取当前片元对应的原图颜色，作为第一个样本。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);
    // 在 UV 坐标中向右移动一点点，准备读取邻近像素。
    vec2 horizontalOffset = vec2(0.002, 0.0);
    // 使用偏移后的坐标读取右侧邻近像素。
    vec4 rightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset);
    // 将两个像素的 RGB 各取一半，得到平均后的颜色。
    vec3 averageRgb = (centerColor.rgb + rightColor.rgb) * 0.5;

    if (textureCoordinate.x >= 0.5) {
        // 输出平均颜色，并保留当前像素原本的透明度。
        gl_FragColor = vec4(averageRgb, centerColor.a);
    } else {
        gl_FragColor = centerColor;
    }
}
