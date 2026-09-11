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

void main() {
    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // x 到达 0.25 后，左边界权重变为 1。
    float leftWeight = smoothstep(0.2,0.25, textureCoordinate.x);
    // x 到达 0.75 后，右边界权重变回 0
    float rightWeight = 1.0 - smoothstep(0.75,0.8, textureCoordinate.x);
    // y 达到0.25后下方变为1(白色)
    float topWeight = smoothstep(0.2,0.25,textureCoordinate.y);
    // y 达到0.75后下方变为1(白色) 然后取反 上方变为白色下方变为黑色
    float bottomWeight = 1.0 - smoothstep(0.75,0.8,textureCoordinate.y);
    // 四个边界同时满足时，区域权重才为 1。
    float regionWeight = leftWeight * rightWeight * topWeight * bottomWeight;
    // 用灰度显示当前区域权重。
    gl_FragColor = vec4(vec3(regionWeight), sourceColor.a);
}

float getSkinWeight(vec3 color){
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, color.r - color.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, color.r - color.g);
    return min(rbWeight, rgWeight);
}
