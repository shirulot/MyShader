precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

float cbScale();
float crScale();
vec3 rgbToYcbcr(vec3 rgbColor);
vec3 ycbcrToRgb(vec3 yuv);

// Y：亮度信号，黑到白；
// U / Cb：偏蓝还是偏黄；
// V / Cr：偏红还是偏青。
void main() {
    // 当前输入是 RGB(A) 纹理。
    vec4 rgbColor = texture2D(inputImageTexture, textureCoordinate);
    vec3 ycbcr = rgbToYcbcr(rgbColor.rgb);
    vec3 rgb = ycbcrToRgb(ycbcr);
    gl_FragColor = vec4(rgb, rgbColor.a);
}

// 计算 rgb转YUV
vec3 rgbToYcbcr(vec3 rgbColor){
    float y = dot(rgbColor, lightRec709);
    float cb = cbScale() * (rgbColor.b - y);
    float cr = crScale() * (rgbColor.r - y);
    return vec3(y, cb + 0.5, cr + 0.5);
}

// 计算YUV转rgb
vec3 ycbcrToRgb(vec3 yuv){
    float y = yuv.r;
    float cb = yuv.g - 0.5;
    float cr = yuv.b - 0.5;

    float redFromCr = 1.0 / crScale();
    float blueFromCb = 1.0 / cbScale();
    float greenFromCr = lightRec709.r * redFromCr / lightRec709.g;
    float greenFromCb = lightRec709.b * blueFromCb / lightRec709.g;

    return vec3(
            y + redFromCr * cr,
            y - greenFromCb * cb - greenFromCr * cr,
            y + blueFromCb * cb
    );
}

// 拿 中心点颜色(1，1，0)举列
// 此时亮度就为 0.2126 + 0.7152 = 0.9278
// 此时cb 为 0 - 0.9278 = - 0.9278
// 需要将色偏纠正为 -0.5 - 0.5 需要+0.5
// 然后+0.5 如果颜色值蓝色 为1的话 那么就会实际上溢出 b = 1 - 0.0722 + 0.5 = 1.4278 所以单纯的加行不通
// 这里0.5 / 的原因是需要让这个系数最终 * Cb结果在-0.5-0.5之间 所以0.5/最大色值 算出他的正边界值
// 这里我们计算系数
float cbScale(){
    float chromaScale = 1.0 - lightRec709.b;
    return 0.5 / chromaScale;
}

float crScale(){
    float chromaScale = 1.0 - lightRec709.r;
    return 0.5 / chromaScale;
}