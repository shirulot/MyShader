precision mediump float;
uniform sampler2D inputImageTexture;
uniform float whitenStrength;

// 黑眼圈滑条控制下眼区域的提亮幅度。
uniform float blackCircleStrength;
// 大眼滑条控制眼睛区域的放大强度，范围由 Android SeekBar 限制为 0.00--0.15，步长为 0.005。
uniform float bigEyeStrength;
// 瘦脸滑条控制轮廓向内部收缩的强度，范围为 0.00--0.05，步长为 0.005。
uniform float slimFaceStrength;
// 口红滑条控制目标色混入原图的强度，范围由 Android 端限制为 0.00--0.50。
uniform float lipstickStrength;
varying vec2 textureCoordinate;

// SDK 眼部轮廓计算出的原始半径。
uniform vec2 leftEyeRadius;
uniform vec2 rightEyeRadius;
// SDK 检测得到的左右眼中心。
uniform vec2 leftEyeCenter;
uniform vec2 rightEyeCenter;

// SDK 轮廓起点及其对应的脸部内部目标点。
uniform vec2 slimOrigins[9];
uniform vec2 slimTargets[9];

//取亮度 RC709
vec3 lightRec709 = vec3(0.2126, 0.7152, 0.0722);

// SDK 内嘴轮廓计算出的中心和半径。
uniform vec2 innerLipCenter;
uniform vec2 innerLipRadius;
// 外部分析得到的人脸框中心。
uniform vec2 faceCenter;
// 外部分析得到的人脸框宽高。
uniform vec2 faceSize;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;


// 有效高光带
float HIGHTLIGHT_START = 0.65;
float HIGHTLIGHT_END = 0.9;
// 有效阴影带
float SHADOW_START = 0.1;
float SHADOW_END = 0.3;

// hue 肤色色相判断
float SKIN_HUE_START = 0.08;
float SKIN_HUE_END = 0.14;

// Saturation 肤色饱和度判断
float SKIN_SATURATION_START = 0.08;
float SKIN_SATURATION_END = 0.20;

float eyeXRatio = 2.0;
float eyeYRatio = 4.5;
// SDK 外唇轮廓计算出的中心和半径。
uniform vec2 lipCenter;
uniform vec2 lipRadius;

// 沿外唇边界依次排列的 20 个关键点。
uniform vec2 smoothOuterLipPoints[40];
uniform vec2 smoothInnerLipPoints[40];

// 选择一个固定的口红目标色。
vec3 lipstickColor = vec3(0.78, 0.06, 0.16);

// 从边界向嘴唇内部逐渐增强，宽度暂用 UV 单位。
float featherWidth = 0.001;

float getHue(vec3 color);
float getSaturation(vec3 color);

float getFaceBoxWeight(){
    // 计算人脸框的左右、上下边界。
    vec2 faceMin = faceCenter - faceSize * 0.5;
    vec2 faceMax = faceCenter + faceSize * 0.5;

    // 当前片元同时位于四条边界内时，人脸框权重为 1。
    float faceBoxWeight =
    step(faceMin.x, textureCoordinate.x) *
    step(textureCoordinate.x, faceMax.x) *
    step(faceMin.y, textureCoordinate.y) *
    step(textureCoordinate.y, faceMax.y) *
    faceCenterReady;
    return faceBoxWeight;
}

float getZoneWeight(vec2 center, vec2 radius){
    // textureCoordinate - eyeCenter 求出坐标的xy差 也就是当前片元距离眼睛中心点的偏移量
    // / radius 归一化 /当前倍率之后得出 用length计算距离 最小的为0黑色 眼中心点 逐渐变大 最后到1.0 这部分都是黑色渐变 大于等于1部分最终现实为白色
    vec2 offset = (textureCoordinate - center) / radius;
    // 获取长度
    float zoneDistance = length(offset);
    // 将眼周距离转换为带羽化边缘的遮罩。
    return 1.0 - smoothstep(0.75, 1.0, zoneDistance);
}

// 获取皮肤颜色权重
float getSkinCandidateWeight(vec3 color) {
    float hue = getHue(color);
    float saturation = getSaturation(color);
    float value = max(max(color.r, color.g), color.b);

    // 限制肤色色相范围。
    float hueWeight = 1.0 - smoothstep(SKIN_HUE_START, SKIN_HUE_END, hue);
    // 排除饱和度过低的灰、白、黑区域。
    float saturationWeight = smoothstep(SKIN_SATURATION_START, SKIN_SATURATION_END, saturation);
    // 保护过暗阴影和过亮高光。
    float shadowWeight = smoothstep(SHADOW_START, SHADOW_END, value);
    float highlightWeight = 1.0 - smoothstep(HIGHTLIGHT_START, HIGHTLIGHT_END, value);

    return hueWeight * saturationWeight * shadowWeight * highlightWeight;
}

// 获取大眼
vec2 getBigEyeUv(vec2 uv, vec2 radius, vec2 center){
    // 使用 SDK 左眼中心和轮廓半径建立椭圆形影响区域。
    vec2 eyeOffset = uv - center;
    vec2 effectRadius = max(radius * 2.0, vec2(0.001));
    float eyeDistance = length(eyeOffset / effectRadius);
    float eyeWeight = (1.0 - smoothstep(0.0, 1.0, eyeDistance)) * faceCenterReady;
    float safeBigEyeStrength = clamp(bigEyeStrength, 0.0, 0.15);
    // 让采样位置向眼睛中心收缩，使眼睛内容向外放大。
    float eyeScale = 1.0 - safeBigEyeStrength * eyeWeight;
    return center + eyeOffset * eyeScale;
}

// 根据真实轮廓起点和目标点计算局部瘦脸采样坐标。
vec2 getSlimFaceUv(vec2 uv, vec2 origin, vec2 target) {
    vec2 direction = target - origin;
    // 暂用两点间的 UV 距离作为影响半径，并避免零半径。
    float radius = max(length(direction), 0.001);
    float weight = 1.0 - smoothstep(0.0, radius, length(uv - origin));
    float safeSlimFaceStrength = clamp(slimFaceStrength, 0.0, 0.05);
    // 沿目标方向的反方向采样，使轮廓内容向目标方向收缩。
    return uv - direction * safeSlimFaceStrength * weight * faceCenterReady;
}

// 检查是否溢出UV 溢出 0 不溢出 1
float insideUv(vec2 value){
    return step(0.0, value.x) * step(value.x, 1.0) * step(0.0, value.y) * step(value.y, 1.0);
}
// 提前声明后面的距离函数，供轮廓函数调用。
float getSegmentDistance(vec2 p, vec2 a, vec2 b);

// 当前UV坐标向单一方向延长后看会和边界的两点相邻的支线相交多少次 如果奇数次则代表在内 否则在外
vec2 getPolygonInfo(vec2 uv, vec2 lipPoints[40]) {
    // 从当前像素向右发射射线，每穿过一次边界就切换内外状态。
    float inside = 0.0;
    // UV 范围内的距离小于 2，先用较大值初始化。
    float minDistance = 2.0;
    // 从最后一个点开始，让第一条边连接最后一点与第一点。
    vec2 a = lipPoints[39];
    for (int i = 0; i < 40; i++) {
        vec2 b = lipPoints[i];
        // 每条边都计算距离，保留最小值。
        float edgeDistance = getSegmentDistance(uv, a, b);
        minDistance = min(minDistance, edgeDistance);
        // 只有边的两端跨过当前高度，才可能与射线相交。
        if ((a.y > uv.y) != (b.y > uv.y)) {
            // 求这条边在当前高度上的横坐标；此分支内两端高度不同。
            // 当前高度位于 A 到 B 的百分之几。
            float t = (uv.y - a.y) / (b.y - a.y);
            // 横坐标也前进同样的百分比。
            float crossX = a.x + t * (b.x - a.x);
            // 交点位于像素右侧时，切换一次内外状态。
            if (uv.x < crossX) {
                inside = 1.0 - inside;
            }
        }
        // 下一条边从当前点出发。
        a = b;
    }
    return vec2(inside, minDistance);
}

// 计算点 p (UV) 到线段 AB 的最短距离。
// 找寻可以垂直的线段上 得到对应的权重
float getSegmentDistance(vec2 p, vec2 a, vec2 b) {
    // 以a为原点b的坐标(b对a偏移)
    vec2 ab = b - a;
    // 以a为原点p的坐标(p对a偏移)
    vec2 ap = p - a;
    // 重要: 推导过程
    //    // 如果ab存在倾斜算出斜边长 推导斜边
    //    float abLength = length(ab);
    //    // 归一化得出每个单位对应多少x y。AB 的单位方向：沿 AB 每走 1 个单位，x/y 各走多少
    //    vec2 abNormalize = normalize(ab);
    //    // AP 在 AB 单位方向上的长度，也就是 AQ
    //    float aq = ap.x * abNormalize.x + ap.y * abNormalize.y;
    //    // AQ 占整条 AB 的比例
    //    float t = aq / max(abLength, 0.00000001);
    // 投影得到最近位置的进度，并避免两个端点重合时除以零。
    // 计算线段长度的平方。
    float abLengthSquared = dot(ab, ab);
    // 长度平方为零时按一个点处理，避免除零。
    if (abLengthSquared <= 0.0) return length(ap);
    // 使用原始平方长度计算投影比例。
    float t = dot(ap, ab) / abLengthSquared;
    // 将位置限制在线段内部，避免落到延长线上。
    t = clamp(t, 0.0, 1.0);
    // 得到线段上最近的点Q = 垂足坐标
    vec2 q = a + t * ab;
    // 计算距离。
    return length(p - q);
}

void main() {
    vec2 uv = textureCoordinate;
    // 读取原图 alpha，保持 inputImageTexture 处于实际使用状态。
    vec4 sourceColor = texture2D(inputImageTexture, uv);

    // 用宽松包围盒筛掉远离嘴唇的像素，留出曲线插值的余量。
    vec2 lipBounds = lipRadius * 1.5;
    vec2 lipDelta = abs(uv - lipCenter);
    // 当前显示黑白遮罩，区域外直接输出黑色。
    if (faceCenterReady < 0.5 || lipDelta.x > lipBounds.x || lipDelta.y > lipBounds.y) {
        gl_FragColor = sourceColor;
        return;
    }

    // 分别计算外唇和内嘴的内外状态、边界距离。
    vec2 outerInfo = getPolygonInfo(uv, smoothOuterLipPoints);
    vec2 innerInfo = getPolygonInfo(uv, smoothInnerLipPoints);

    // 外唇以内、内嘴以外，才是需要染色的区域。
    float lipWeight = outerInfo.x * (1.0 - innerInfo.x) * faceCenterReady;

    // 取距离较近的边界，让内外两圈边缘都参与羽化。
    float boundaryDistance = min(outerInfo.y, innerInfo.y);

    // 从边界向嘴唇内部逐渐增强，宽度暂用 UV 单位。
    float fade = smoothstep(0.0, featherWidth, boundaryDistance);

    // 得到带柔和边缘的最终嘴唇权重。
    float softLipWeight = lipWeight * fade;
    // 再次限制外部参数，避免异常值让颜色混合超出预期。
    float safeLipstickStrength = clamp(lipstickStrength, 0.0, 0.5);
    // 使用羽化后的权重控制口红混色。
    vec3 resultColor = mix(sourceColor.rgb, lipstickColor, softLipWeight * safeLipstickStrength);
    // 显示羽化后的遮罩：白色染色，黑色不染色，灰色部分染色。
    gl_FragColor = vec4(resultColor, sourceColor.a);
}

// 详细查看 [res/drawable/hue_color_ring.png]
// 将 RGB 转换为 0--1 范围的 HSV 色相 H。
float getHue(vec3 color) {
    // 取到通道最大值
    float maxValue = max(max(color.r, color.g), color.b);
    // 取到通道最小值
    float minValue = min(min(color.r, color.g), color.b);
    // 三通道最大差 同时也是value
    float delta = maxValue - minValue;
    //最大值与最小值几乎相等时属于无彩色,H 没有定义，这里安全返回 0。是灰/白/黑 三色
    if (delta < 0.0001) return 0.0;
    // 如果最大为红 则去计算他是偏向正负 正则为色环右方的黄色 负则为左边的品红 这里的mod单纯用来校正负值 后续不需要因为后续不是从0开始
    if (maxValue == color.r) return mod((color.g - color.b) / delta, 6.0) / 6.0;
    // 如果最大为绿 则去计算他是偏向正负 正则为色环右方的青色 负则为左边的黄色 +2是因为移动到绿区块
    if (maxValue == color.g) return ((color.b - color.r) / delta + 2.0) / 6.0;
    // 如果最大为蓝 则去计算他是偏向正负 正则为色环右方的品红 负则为左边的青色 +4是因为移动到蓝区块
    return ((color.r - color.g) / delta + 4.0) / 6.0;
}
// 饱和度
float getSaturation(vec3 color){
    float maxChannel = max(max(color.r, color.g), color.b);
    float minChannel = min(min(color.r, color.g), color.b);
    return (maxChannel - minChannel) / max(maxChannel, 0.0001);
}
