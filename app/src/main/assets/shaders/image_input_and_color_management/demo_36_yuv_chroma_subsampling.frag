precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

float getBlueColorDiff();
float getRedChromaScale();

// Y：亮度信号，黑到白；
// U / Cb：偏蓝还是偏黄；
// V / Cr：偏红还是偏青。
void main() {
    // 当前输入是 RGB(A) 纹理。
    vec4 rgbColor = texture2D(inputImageTexture, textureCoordinate);
    // 模拟低分辨率色度：亮度逐像素保留，色度按大格子共享。
    vec2 chromaGrid = vec2(24.0);
    // 横竖分成 24*24 的格子 chromaUv 是这个格子内中心点
    // 因为是以 floor(textureCoordinate * chromaGrid) 结果一个整数为一个格子 所以+0.5就是中心点了
    vec2 chromaUv = (floor(textureCoordinate * chromaGrid) + 0.5) / chromaGrid;
    vec3 chromaColor = texture2D(inputImageTexture, chromaUv).rgb;
    // 亮度
    float y = dot(rgbColor.rgb, lightRec709);
    // 中心点亮度
    float chromaY = dot(chromaColor, lightRec709);


    float cb = getBlueChromaScale() * (chromaColor.b - chromaY);
    float cr = getRedChromaScale() * (chromaColor.r - chromaY);

    vec3 outputColor = vec3(y + 1.5748 * cr, y - 0.1873 * cb - 0.4681 * cr, y + 1.8556 * cb);
    gl_FragColor = vec4(clamp(outputColor, 0.0, 1.0), rgbColor.a);
}

// 拿 中心点颜色(1，1，0)举列
// 此时亮度就为 0.2126 + 0.7152 = 0.9278
// 此时cb 为 0 - 0.9278 = - 0.9278
// 需要将色偏纠正为 -0.5 - 0.5 需要+0.5
// 然后+0.5 如果颜色值蓝色 为1的话 那么就会实际上溢出 b = 1 - 0.0722 + 0.5 = 1.4278 所以单纯的加行不通
// 这里0.5 / 的原因是需要让这个系数最终 * Cb结果在-0.5-0.5之间 所以0.5/最大色值 算出他的正边界值
// 这里我们计算系数
float getBlueChromaScale(){
    float chromaScale = 1.0 - lightRec709.b;
    return 0.5 / chromaScale;
}

float getRedChromaScale(){
    float chromaScale = 1.0 - lightRec709.r;
    return 0.5 / chromaScale;
}