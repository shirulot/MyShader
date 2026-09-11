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

    //    gl_FragColor = observedColor;
    // --------------- 以上为模拟图像/白卡加灯光 ----------------
    // 获取线性白卡以及原色
    vec3 linearWhiteCard = pow(observedNeutralRgb, vec3 (2.2));
    vec3 linearOrigin = pow(observedRgb, vec3 (2.2));
    // 倍率
    vec3 whiteBalanceGain = linearWhiteCard.g / linearWhiteCard;
    // 原图白平衡
    vec3 whiteBalanceOrigin = linearOrigin * whiteBalanceGain;
    // 实际只是把色值变成三个统一为绿的vec3
    vec3 whiteBalanceWihteCard = linearWhiteCard * whiteBalanceGain;
    // 实际结果为绿值
    float luminaceY = dot(whiteBalanceWihteCard, lightRec709);
    // 白平衡补正倍率
    float exposureGain = TARGET_WHITE_LUMA / max(luminaceY, 0.0001);
    // 去打光原图
    vec3 noLightOrigin = pow(whiteBalanceOrigin * exposureGain, vec3(1.0 / 2.2));

//    gl_FragColor = vec4(mix(observedRgb, noLightOrigin, textureCoordinate.x > 0.5), observedColor.a);
    //    gl_FragColor = vec4(noLightOrigin,observedColor.a);
    float inputDebugBand = floor(textureCoordinate.x * 4.0);
    vec3 inputDebug = observedRgb;
    if (inputDebugBand == 1.0) inputDebug = vec3(observedRgb.r);
    if (inputDebugBand == 2.0) inputDebug = vec3(observedRgb.g);
    if (inputDebugBand == 3.0) inputDebug = vec3(observedRgb.b);

    gl_FragColor = vec4(inputDebug, observedColor.a);
}



float getSkinWeight(vec3 color){
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, color.r - color.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, color.r - color.g);
    return min(rbWeight, rgWeight);
}
