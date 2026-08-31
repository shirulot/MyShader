precision mediump float;
uniform sampler2D inputImageTexture;
uniform float blurStrength;
uniform float warmthStrength;
varying vec2 textureCoordinate;
// 当前处理的中心色
vec4 centerColor;
float scale = 3.0;

//取亮度
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);
// 补暖色调公式(单位)
vec3 redFix = vec3(1.0, 0.0, -0.2126 / 0.0722);

// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

vec3 calculateBilateralAverageRgb(float scale);
vec3 createWarmToneOffset(float warmth);
float getSkinWeight();

void main() {
    // 原图直通：采样结果不做任何颜色处理。
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    // 左半屏显示分离调试，右半屏保留当前效果与原图对比。
    // 当前像素原色。
    centerColor = texture2D(inputImageTexture, textureCoordinate);
    //获取双边滤波取色
    vec3 averageRgb = calculateBilateralAverageRgb(scale);
    // 当前像素周围的局部颜色基准。
    vec3 localBaseRgb = averageRgb;
    // 亮度把 RGB 按视觉权重压缩成一个明暗值。
    // dot vec3每项的值相乘并且加起来 这里是取亮度的范式写法
    float luminance = dot(centerColor.rgb, lightRec709);
    // 色度是 RGB 相对亮度的偏差；中性灰表示没有偏色。
    vec3 chromaDebug = clamp((centerColor.rgb - vec3(luminance)) * 4.0 + vec3(0.5), 0.0, 1.0);
    // 两项条件共同限制皮肤权重。
    float skinWeight = getSkinWeight();
    // 原色与模糊色差距代表边缘强度。
    float edgeStrength = length(centerColor.rgb - averageRgb);
    // 明显边缘得到更高保护权重。
    float edgeProtection = smoothstep(0.03, 0.12, edgeStrength);
    // 将 SeekBar 强度映射到安全的 0 到 1。
    float strengthWeight = clamp(blurStrength, 0.0, 1.0);
    // 得到最终局部磨皮比例。
    float blurWeight = skinWeight * (1.0 - edgeProtection) * strengthWeight;

    // 只由磨皮强度控制原色与局部颜色基准的混合。
    vec3 smoothedRgb = mix(centerColor.rgb, localBaseRgb, blurWeight);
    // 限制大小
    float warmth = clamp(warmthStrength, 0.0, 1.0);
    // 红色增量大于绿色增量，形成轻微暖色方向。
    //    vec3 warmOffset = createWarmToneOffset(0.03 * warmth);
    // 红色增加后，用蓝色补偿相同的亮度，保证偏移自身的亮度为零。
    vec3 warmChromaOffset = redFix * (0.008 * warmth);
    // 只让皮肤候选且非明显细节区域获得暖色。
//    float warmthWeight = skinWeight * (1.0 - edgeProtection);
    // 中间亮度的肤色更适合调色，阴影和高光逐渐降低强度。
    // 0.1-0.3 是阴影过滤带 0.65-0.9是高光过滤带
    // 0.10-0.30：从阴影区逐渐放行暖色。
    // 0.65-0.90：进入高光区后逐渐关闭暖色。
    // 最终权重 = 阴影放行权重 * 非高光放行权重。
    // 实际含义：暖色主要作用于排除阴影和高光后的中间亮度区域。
    float midToneWeight = smoothstep(SHADOW_START, SHADOW_END, luminance) * (1.0 - smoothstep(HIGHTLIGHT_START, HIGHTLIGHT_END, luminance));
    // 皮肤、非边缘且处于中间亮度时才调整暖色色度。
    float warmthWeight = skinWeight * (1.0 - edgeProtection) * midToneWeight;
    // 在磨皮结果上叠加受遮罩限制的暖色。
    //    vec3 resultRgb = min(smoothedRgb + warmOffset * warmthWeight, vec3(1.0));
    // 只叠加色度偏移，并把 RGB 限制在合法范围。
    vec3 resultRgb = clamp(smoothedRgb + warmChromaOffset * warmthWeight, vec3(0.0), vec3(1.0));

    //    // 当前色加入暖色
    //    vec3 targetSkin = min(localBaseRgb + createWarmToneOffset(0.08), vec3(1.0));
    //    // 混合原色与平滑后的颜色。
    //    vec3 resultRgb = mix(centerColor.rgb, targetSkin, blurWeight);


    // 每半屏按 y 再分为两条横带，便于同时观察四种输出。
    // step 阈值方法 param 2 < param 1 则返回0 否则 1
    float band = step(0.5, textureCoordinate.y);
    vec4 debugColor = mix(vec4(chromaDebug, centerColor.a), vec4(vec3(luminance), centerColor.a), band);
    vec4 compareColor = mix(vec4(resultRgb, centerColor.a), centerColor, band);
    gl_FragColor = mix(debugColor, compareColor, step(0.5, textureCoordinate.x));
}

// 获取肤色权重
float getSkinWeight(){
    // 红蓝差对应的皮肤权重。
    float redBlueWeight = smoothstep(0.04, 0.16, centerColor.r - centerColor.b);
    // 红绿差对应的皮肤权重。
    float redGreenWeight = smoothstep(0.00, 0.08, centerColor.r - centerColor.g);
    // 两项条件共同限制皮肤权重。
    return min(redBlueWeight, redGreenWeight);
}

// 暖色调值 粗略算法 后续可以直接用 redFix
vec3 createWarmToneOffset(float warmth){
    return vec3(warmth, warmth * 0.35, 0.0);
}

// 计算和当前色的色差
float getDiffLength(vec3 target){
    return length(target - centerColor.rgb);
}

// 色差传入方法后
// 我们判断 通常 0.03我们认为他几乎为同一颜色。
// 如果0.18或以上 我们认为他完全不是一种颜色
// 因为我们需要的色差权重逻辑应该是色差越小权重越大 和这里的逻辑相反 需要用1-去取反
float getWeight (float diff){
    return 1.0 - smoothstep(0.03, 0.18, diff);
}

// 双边滤波算法 让色差大的占权重低 色差小的占权重高 使得整体更加平滑 如果色差过大则不进行磨皮
vec3 calculateBilateralAverageRgb(float scale){
    // 横向近邻采样距离。
    vec2 horizontalOffset = vec2(0.004 * scale, 0.0);
    // 纵向近邻采样距离。
    vec2 verticalOffset = vec2(0.0, 0.004 * scale);
    // 八侧近邻颜色。
    vec4 leftColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset);
    vec4 rightColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset);
    vec4 topColor = texture2D(inputImageTexture, textureCoordinate - verticalOffset);
    vec4 bottomColor = texture2D(inputImageTexture, textureCoordinate + verticalOffset);
    vec4 rightTopColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset - verticalOffset);
    vec4 rightBottomColor = texture2D(inputImageTexture, textureCoordinate + horizontalOffset + verticalOffset);
    vec4 leftTopColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset - verticalOffset);
    vec4 leftBottomColor = texture2D(inputImageTexture, textureCoordinate - horizontalOffset + verticalOffset);

    // 八方颜色颜色与当前颜色的差距。
    float leftDifference = getDiffLength(leftColor.rgb);
    float rightDifference = getDiffLength(rightColor.rgb);
    float topDifference = getDiffLength(topColor.rgb);
    float bottomDifference = getDiffLength(bottomColor.rgb);
    float leftTopDifference = getDiffLength(leftTopColor.rgb);
    float leftBottomDifference = getDiffLength(leftBottomColor.rgb);
    float rightTopDifference = getDiffLength(rightTopColor.rgb);
    float rightBottomDifference = getDiffLength(rightBottomColor.rgb);

    // 颜色越接近当前像素采样权重越大。
    float leftWeight = getWeight(leftDifference);
    float rightWeight = getWeight(rightDifference);
    float topWeight = getWeight(topDifference);
    float bottomWeight = getWeight(bottomDifference);
    float leftTopWeight = getWeight(leftTopDifference);
    float leftBottomWeight = getWeight(leftBottomDifference);
    float rightTopWeight = getWeight(rightTopDifference);
    float rightBottomWeight = getWeight(rightBottomDifference);

    // 中心像素始终保留一份基础权重。
    float totalWeight = 1.0;
    // 将左侧相近颜色加入平滑结果。
    vec3 averageRgb = centerColor.rgb;
    // 累加八侧颜色及其动态权重。
    averageRgb += leftColor.rgb * leftWeight;
    averageRgb += rightColor.rgb * rightWeight;
    averageRgb += topColor.rgb * topWeight;
    averageRgb += bottomColor.rgb * bottomWeight;
    averageRgb += leftTopColor.rgb * leftTopWeight;
    averageRgb += leftBottomColor.rgb * leftBottomWeight;
    averageRgb += rightTopColor.rgb * rightTopWeight;
    averageRgb += rightBottomColor.rgb * rightBottomWeight;

    // 累加所有实际参与计算的权重。
    totalWeight += leftWeight + rightWeight + topWeight + bottomWeight + leftTopWeight + leftBottomWeight + rightBottomWeight + rightTopWeight;
    // 除以总权重，得到不会整体变亮的平均色。
    averageRgb /= totalWeight;
    return averageRgb;
}
