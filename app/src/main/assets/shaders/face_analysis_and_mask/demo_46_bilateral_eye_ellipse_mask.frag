precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {

    // 模拟输入准备区：允许读取加光前的原图。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);
    // 手工指定左眼中心。
    vec2 leftEyeCenter = vec2(0.36, 0.41);
    vec2 rightEyeCenter = vec2(0.65, 0.41);
    // 分别控制椭圆的横向半径和纵向半径。
    vec2 eyeRadius = vec2(0.12, 0.055);
    // textureCoordinate - eyeCenter 求出坐标的xy差 也就是当前片元距离眼睛中心点的偏移量
    // / eyeRadius 归一化 /当前倍率之后得出 用length计算距离 最小的为0黑色 眼中心点 逐渐变大 最后到1.0 这部分都是黑色渐变 大于等于1部分最终现实为白色
    vec2 leftEyeOffset = (textureCoordinate - leftEyeCenter) / eyeRadius;
    vec2 rightEyeOffset = (textureCoordinate - rightEyeCenter) / eyeRadius;
    float leftEyeDistance = length(leftEyeOffset);
    float rightEyeDistance = length(rightEyeOffset);
    // 将眼周距离转换为带羽化边缘的遮罩。
    float leftEyeWeight = 1.0 - smoothstep(0.75, 1.0, leftEyeDistance);
    float rightEyeWeight = 1.0 - smoothstep(0.75, 1.0, rightEyeDistance);
    float showEyeWeight = max(leftEyeWeight, rightEyeWeight);
    // 显示眼周遮罩。
    gl_FragColor = vec4(vec3(showEyeWeight), sourceColor.a);
}
