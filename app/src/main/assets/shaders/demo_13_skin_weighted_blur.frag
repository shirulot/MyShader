precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // demo 13
    vec2 horizontalOffset = vec2(0.002, 0.0);
    // 读取当前像素左侧的邻居颜色。
    vec4 leftColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset);
    // 读取当前像素自己的颜色。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);
    // 读取当前像素右侧的邻居颜色。
    vec4 rightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset);
    // 左、中、右三份 RGB 等权平均，避免只向右产生拖影。 平均后存在模糊
    //    vec3 averageRgb = (leftColor.rgb + centerColor.rgb + rightColor.rgb) / 3.0;
    // 当前像素保留 60%，左右邻居各混入 20%，使柔化更轻。
    // 因为上面提到了模糊 让我们降低幅度 这时候因为center就是原图的颜色 所以实际上只要提高center占比即可
    vec3 averageRgb = centerColor.rgb * 0.6 + leftColor.rgb * 0.2 + rightColor.rgb * 0.2;

    // 根据红蓝差计算候选皮肤的连续权重。
    float redBlueWeight = smoothstep(0.04, 0.16, centerColor.r - centerColor.b);
    // 根据红绿差计算候选皮肤的连续权重。
    float redGreenWeight = smoothstep(0.00, 0.08, centerColor.r - centerColor.g);
    // 两项条件取较小值，得到最终皮肤权重。
    float skinWeight = min(redBlueWeight, redGreenWeight);
    // 仅按皮肤权重混入平滑后的颜色，最多使用 50% 的平滑结果。
    vec3 resultRgb = mix(centerColor.rgb, averageRgb, skinWeight * 2.0);

    // 输出局部磨皮结果，并保留当前像素透明度。
    //    gl_FragColor = vec4(resultRgb, centerColor.a);
    //    gl_FragColor = vec4(averageRgb, centerColor.a);
    if (textureCoordinate.x >= 0.5) {
        // 输出平均颜色，并保留当前像素原本的透明度。
        gl_FragColor = vec4(resultRgb, centerColor.a);
    } else {
        gl_FragColor = centerColor;
    }
}
