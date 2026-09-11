precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
uniform float brightenStrength;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);


// 肤色的红蓝色差 - 肤色识别
float SKIN_DIFF_RB_START = 0.04;
float SKIN_DIFF_RB_END = 0.16;
// 肤色的红绿色差 - 肤色识别
float SKIN_DIFF_RG_START = 0.00;
float SKIN_DIFF_RG_END = 0.08;
// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

float getSkinWeight(vec3 color);

// 实验前提：虚拟白卡在加光前为线性白色，目标线性亮度为 1.0。
const float TARGET_WHITE_LUMA = 1.0;

// 仅用于教学：返回受到模拟光照色偏后的颜色。
vec3 simulateColorCast(vec3 sourceRgb) {
    vec3 sourceLinear = pow(sourceRgb, vec3(2.2));
    vec3 simulatedLightGain = vec3(0.3, 0.5, 0.2);
    vec3 observedLinear = sourceLinear * simulatedLightGain;
    return pow(clamp(observedLinear, 0.0, 1.0), vec3(1.0 / 2.2));
}


// 模拟一张已知为纯白的白卡，不能从人物图片中随意取一个像素代替。
vec3 simulateWhiteCard() {
    return simulateColorCast(vec3(1.0));
}

void main() {
    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // 模拟加光后的rgb
    vec3 observedRgb = simulateColorCast(sourceColor.rgb);
    // 模拟加光后的argb
    vec4 observedColor = vec4(observedRgb, sourceColor.a);
    // 模拟白卡色值
    vec3 observedNeutralRgb = simulateWhiteCard();
    // --------------- 以上为模拟图像/白卡加灯光 ----------------

    // 正式校正区：从这里开始只使用模拟光照后的 observed 数据。
    // 偏色图的 gamma 线性空间。
    vec3 observedLinear = pow(observedColor.rgb, vec3(2.2));
    // 白卡的 gamma 线性空间。
    vec3 observedNeutralLinear = pow(observedNeutralRgb, vec3(2.2));

    // 根据偏色后的中性参考，以绿色为 1 推算反向增益。
    // 比如假设这里的原图被加光之后的线性空间是 (0.3, 0.5, 0.2)。
    // 这里得出 0.5/0.3, 0.5/0.5, 0.5/0.2。
    // 1.66, 1, 2.5 是根据光照结果推算出的反向校正倍率。
    // 这里的 (0.3, 0.5, 0.2) 指 observedNeutralLinear，不是整张图所有像素。
    vec3 whiteBalanceGain = vec3(observedNeutralLinear.g) / max(observedNeutralLinear, vec3(0.001));

    // 白平衡校正偏色后的整张图。意义为 绿通道的校正增益为 1.0时的颜色
    // 理解三色通道乘 (1.66, 1, 2.5) 倍
    vec3 correctedLinear = observedLinear * whiteBalanceGain;
    // 计算中性参考点经过白平衡后还剩多少亮度。
    // 此时 这里的答案一定是 vec3(green,green,green) 如果是上面假设的green=0.5则为 (0.5, 0.5, 0.5)。
    vec3 correctedNeutralLinear = observedNeutralLinear * whiteBalanceGain;

    // 曝光目标来自“参考点已知为白色”的外部前提，不读取加光前的参考点。
    // 计算白平衡之后的 Y luminance。这个值 一定是observedNeutralLinear.g
    float correctedLuma = dot(correctedNeutralLinear, lightRec709);
    // 曝光补偿：目标亮度除以当前亮度。实际是白平衡中亮度倍补到1了 需要取到原本1和绿的差倍
    float exposureGain = TARGET_WHITE_LUMA / max(correctedLuma, 0.001);
    // 原本1和绿的差倍 去*白平衡后的原图 做曝光补正
    vec3 restoredLinear = correctedLinear * exposureGain;
    // 防止溢出 然后做gamma反码
    vec3 restoredRgb = pow(clamp(restoredLinear, 0.0, 1.0), vec3(1.0 / 2.2));

    // 左：模拟色偏；中：只做白平衡；右：白平衡加曝光补偿。
    float showWhiteBalance = step(1.0 / 3.0, textureCoordinate.x);
    float showExposure = step(2.0 / 3.0, textureCoordinate.x);
    // 学习笔记：所以这里拿比例去还原 RGB 是为什么？
    // 答：比例已在 correctedLinear 中完成偏色校正，pow(1.0 / 2.2) 只负责转回屏幕编码。
    vec3 correctedRgb = pow(clamp(correctedLinear, 0.0, 1.0), vec3(1.0 / 2.2));
    //
    vec3 outputRgb = mix(observedRgb, correctedRgb, showWhiteBalance);
    outputRgb = mix(outputRgb, restoredRgb, showExposure);

    gl_FragColor = vec4(outputRgb, observedColor.a);
}



float getSkinWeight(vec3 color){
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, color.r - color.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, color.r - color.g);
    return min(rbWeight, rgWeight);
}
