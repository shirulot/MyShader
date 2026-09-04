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
// gamma 编码 亮度*0.5的算法和错误算法 实际如果直接rgb*0.5 亮度降低幅度远高于一半
void main() {
    // 当前输入是 RGB(A) 纹理。
    vec4 rgbColor = texture2D(inputImageTexture, textureCoordinate);

    // 假设输入偏暖：压红、补蓝，校正到更中性的颜色。
    vec3 linearRgb = pow(rgbColor.rgb, vec3(2.2));
    // 模拟暖光先让整张图偏暖。
    vec3 warmLight = vec3(1.15, 1.00, 0.85);
    vec3 warmLinearRgb = linearRgb * warmLight;

    // 已知左侧采样点应为中性白，用它反推出校正增益。
    vec3 neutralRgb = texture2D(inputImageTexture, vec2(0.05, 0.50)).rgb;
    vec3 neutralLinear = pow(neutralRgb, vec3(2.2)) * warmLight;
    vec3 whiteBalanceGain = neutralLinear.g / max(neutralLinear, vec3(0.001));

    vec3 correctedRgb = pow(clamp(warmLinearRgb * whiteBalanceGain, 0.0, 1.0), vec3(1.0 / 2.2));
    gl_FragColor = vec4(correctedRgb, rgbColor.a);
}

float getSkinWeight(vec3 color){
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, color.r - color.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, color.r - color.g);
    return min(rbWeight, rgWeight);
}
