precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

void main() {
    // 定义局部形变的中心和半径。
    vec2 center = vec2(0.5, 0.5);
    float radius = 0.25;
    // 计算当前片元到椭圆中心长度
    float distanceToCenter = length(textureCoordinate - center);
    // step里代表 大于0.25的部分保留 1-取反代表保留小于0.25的部分 这个权重是当前片元是否在中心椭圆内 0.6代表0.6*0.25-> 0.15-0.25递减过渡
    // 中心区域保持完整形变，靠近外圈时逐渐减弱到零。
    float innerRadius = radius * 0.6;
    float regionWeight = 1.0 - smoothstep(innerRadius, radius, distanceToCenter);
    // 只让半径以内的区域改为读取右侧纹理。
    // regionWeight 代表 当前片元是否在中心椭圆范围内 在的话则是1 进行左移取样
    vec2 sampledUv = textureCoordinate + vec2(0.08, 0.0) * regionWeight;
    gl_FragColor = texture2D(inputImageTexture, sampledUv);
}
