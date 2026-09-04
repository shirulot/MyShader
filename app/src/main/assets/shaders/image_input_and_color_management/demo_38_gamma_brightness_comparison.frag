precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// gamma 编码 亮度*0.5的算法和错误算法 实际如果直接rgb*0.5 亮度降低幅度远高于一半
void main() {
    // 当前输入是 RGB(A) 纹理。
    vec4 rgbColor = texture2D(inputImageTexture, textureCoordinate);
    // 左侧直接将编码 RGB 数值减半。
    vec3 encodedHalf = rgbColor.rgb * 0.5;
    // 右侧先近似转线性 RGB，减半后再编码回显示空间。
    vec3 linearHalf = pow(pow(rgbColor.rgb, vec3(2.2)) * 0.5, vec3(1.0 / 2.2));
    float useLinear = step(0.5, textureCoordinate.x);
    gl_FragColor = vec4(mix(encodedHalf, linearHalf, useLinear), rgbColor.a);
}
