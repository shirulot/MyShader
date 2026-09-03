precision mediump float;
uniform sampler2D inputImageTexture;
uniform float blurStrength;
varying vec2 textureCoordinate;

void main() {
    // demo 15
    // 左侧显示效果，右侧显示原图。
    bool isProcessedSide = textureCoordinate.x <= 0.5;
    // 横向近邻采样距离。
    vec2 horizontalOffset = vec2(0.004, 0.0);
    // 纵向近邻采样距离。
    vec2 verticalOffset = vec2(0.0, 0.004);

    // 当前像素原色。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);
    // 左侧近邻颜色。
    vec4 leftColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset);
    // 右侧近邻颜色。
    vec4 rightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset);
    // 上方近邻颜色。
    vec4 topColor = texture2D(inputImageTexture, textureCoordinate - verticalOffset);
    // 下方近邻颜色。
    vec4 bottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset);
    // 左上近邻颜色。
    vec4 leftTopColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset - verticalOffset);
    // 右上近邻颜色。
    vec4 rightTopColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset - verticalOffset);
    // 左下近邻颜色。
    vec4 leftBottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset - horizontalOffset);
    // 右下近邻颜色。
    vec4 rightBottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset + horizontalOffset);



//    // 更远一圈的左侧颜色。
//    vec4 farLeftColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset * 2.0);
//    // 更远一圈的右侧颜色。
//    vec4 farRightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset * 2.0);
//    // 更远一圈的上方颜色。
//    vec4 farTopColor = texture2D(inputImageTexture, textureCoordinate - verticalOffset * 2.0);
//    // 更远一圈的下方颜色。
//    vec4 farBottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset * 2.0);
//    // 四个远邻扩大柔化范围。
//    vec3 farZone= (farLeftColor.rgb + farRightColor.rgb + farTopColor.rgb + farBottomColor.rgb) * 0.06;
    // 当前像素保留最多细节。
//    vec3 averageRgb = centerColor.rgb * 0.28 + originZone + farZone;
    vec3 averageRgb = centerColor.rgb * 0.25;
    // 四个近邻提供主要柔化。
    averageRgb += (leftColor.rgb + rightColor.rgb + topColor.rgb + bottomColor.rgb) * 0.125;
    averageRgb += (leftTopColor.rgb + rightTopColor.rgb + leftBottomColor.rgb + rightBottomColor.rgb) * 0.0625;



    // 红蓝差对应的皮肤权重。
    float redBlueWeight = smoothstep(0.04, 0.16, centerColor.r - centerColor.b);
    // 红绿差对应的皮肤权重。
    float redGreenWeight = smoothstep(0.00, 0.08, centerColor.r - centerColor.g);
    // 两项条件共同限制皮肤权重。
    float skinWeight = min(redBlueWeight, redGreenWeight);
    // 原色与模糊色差距代表边缘强度。
    float edgeStrength = length(centerColor.rgb - averageRgb);
    // 明显边缘得到更高保护权重。
    float edgeProtection = smoothstep(0.03, 0.12, edgeStrength);
    // 将 SeekBar 强度映射到安全的 0 到 1。
    float strengthWeight = clamp(blurStrength , 0.0, 1.0);
    // 得到最终局部磨皮比例。
    float blurWeight = skinWeight * (1.0 - edgeProtection) * strengthWeight;
    // 混合原色与平滑后的颜色。
    vec3 resultRgb = mix(centerColor.rgb, averageRgb, blurWeight);

    // 左侧显示磨皮结果。
    if (isProcessedSide) {
        // 输出处理结果。
        gl_FragColor = vec4(resultRgb, centerColor.a);
    } else {
        // 右侧显示原图。
        gl_FragColor = centerColor;
    }
}
