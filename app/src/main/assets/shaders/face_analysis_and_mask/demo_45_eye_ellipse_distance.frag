precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;
uniform float brightenStrength;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// 外部分析得到的人脸框中心。
uniform vec2 faceCenter;
// 外部分析得到的人脸框宽高。
uniform vec2 faceSize;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

// hue 肤色判断
float SKIN_HUE_START = 0.08;
float SKIN_HUE_END = 0.14;

// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

float SKIN_SATURATION_START = 0.08;
float SKIN_SATURATION_END = 0.20;

float getHue(vec3 color);
float getSaturation(vec3 color);

void main() {

    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // 手工指定左眼中心。
    vec2 eyeCenter = vec2(0.36, 0.41);
    // 分别控制椭圆的横向半径和纵向半径。
    vec2 eyeRadius = vec2(0.12, 0.055);
    // textureCoordinate - eyeCenter 求出坐标的xy差 也就是当前片元距离眼睛中心点的偏移量
    // / eyeRadius 归一化 /当前倍率之后得出 用length计算距离 最小的为0黑色 眼中心点 逐渐变大 最后到1.0 这部分都是黑色渐变 大于等于1部分最终现实为白色
    vec2 eyeOffset = (textureCoordinate - eyeCenter) / eyeRadius;
    float eyeDistance = length(eyeOffset);
    // 因为超出1.0的部分vec3通道最终也会显示未白色 所以就是眼睛椭圆为黑 其他为白色
    gl_FragColor = vec4(vec3(eyeDistance), sourceColor.a);
}