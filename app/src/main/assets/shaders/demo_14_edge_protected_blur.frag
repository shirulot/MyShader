precision mediump float;
uniform sampler2D inputImageTexture;
uniform float blurStrength;
varying vec2 textureCoordinate;

void main() {
    // demo 14
    vec2 horizontalOffset = vec2(0.002, 0.0);
    vec2 verticalOffset = vec2(0.0, 0.002);
    // 读取当前像素左侧的邻居颜色。
    vec4 leftColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset);
    // 读取当前像素自己的颜色。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);
    // 读取当前像素右侧的邻居颜色。
    vec4 rightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset);
    // 读取当前像素上侧的邻居颜色。
    vec4 topColor = texture2D(inputImageTexture, textureCoordinate - verticalOffset);
    // 读取当前像素下侧的邻居颜色。
    vec4 bottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset);
    // 左、中、右三份 RGB 等权平均，避免只向右产生拖影。 平均后存在模糊
    //    vec3 averageRgb = (leftColor.rgb + centerColor.rgb + rightColor.rgb) / 3.0;
    // 当前像素保留 40%，上下左右邻居各混入 15%，使柔化更轻。
    vec3 averageRgb = centerColor.rgb * 0.4
        + leftColor.rgb * 0.15
        + rightColor.rgb * 0.15
        + topColor.rgb * 0.15
        + bottomColor.rgb * 0.15;

    // 根据红蓝差计算候选皮肤的连续权重。
    float redBlueWeight = smoothstep(0.04, 0.16, centerColor.r - centerColor.b);
    // 根据红绿差计算候选皮肤的连续权重。
    float redGreenWeight = smoothstep(0.00, 0.08, centerColor.r - centerColor.g);
    // 两项条件取较小值，得到最终皮肤权重。
    float skinWeight = min(redBlueWeight, redGreenWeight);
    // 仅按皮肤权重混入平滑后的颜色，最多使用 50% 的平滑结果。
    //    vec3 resultRgb = mix(centerColor.rgb, averageRgb, skinWeight * 1.0);

    // 计算原色与五点平均色的差异，差异大通常表示轮廓或细节。
    float edgeStrength = length(centerColor.rgb - averageRgb);
    // 将明显边缘转换为 0.0 到 1.0 的保护权重。
    float edgeProtection = smoothstep(0.03, 0.12, edgeStrength);
    // 皮肤区域且不是明显边缘时，才保留由 Android SeekBar 控制的模糊强度。
    float blurWeight = skinWeight * (1.0 - edgeProtection) * blurStrength;
    // 按最终模糊权重混合原色与五点平滑色。
    vec3 resultRgb = mix(centerColor.rgb, averageRgb, blurWeight);

    // 输出带轮廓保护的局部磨皮结果。
    //    gl_FragColor = vec4(resultRgb, centerColor.a);
    if (textureCoordinate.x <= 0.5) {
        // 输出带轮廓保护的局部磨皮结果。
        //        gl_FragColor = vec4(averageRgb, centerColor.a);
        gl_FragColor = vec4(resultRgb, centerColor.a);
    } else {
        gl_FragColor = centerColor;
    }
}
