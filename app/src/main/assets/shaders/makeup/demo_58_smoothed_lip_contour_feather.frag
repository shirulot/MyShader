precision mediump float;
uniform sampler2D inputImageTexture;
// 口红滑条控制目标色混入原图的强度，范围由 Android 端限制为 0.00--0.50。
uniform float lipstickStrength;
varying vec2 textureCoordinate;

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

// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

// 前置声明：多边形计算会先调用后面定义的线段距离函数。
float getSegmentDistance(vec2 p, vec2 a, vec2 b);

// 当前UV坐标向单一方向延长后看会和边界的两点相邻的支线相交多少次 如果奇数次则代表在内 否则在外
vec2 getPolygonInfo(vec2 uv, vec2 lipPoints[40]) {
    // 从当前像素向右发射射线，每穿过一次边界就切换内外状态。
    float inside = 0.0;
    // UV 范围内的距离小于 2，先用较大值初始化。
    float minDistance = 2.0;
    // 从最后一个点开始，让第一条边连接最后一点与第一点。
    vec2 a = lipPoints[lipPoints.length() - 1];
    for (int i = 0; i < lipPoints.length(); i++) {
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
    float t = dot(ap, ab) / max(dot(ab, ab), 0.00000001);
    // 将位置限制在线段内部，避免落到延长线上。
    t = clamp(t, 0.0, 1.0);
    // 得到线段上最近的点Q = 垂足坐标
    vec2 q = a + t * ab;
    // 计算距离。
    return length(p - q);
}

void main() {
    // 读取原图 alpha，保持 inputImageTexture 处于实际使用状态。
    vec4 sourceColor = texture2D(inputImageTexture, textureCoordinate);

    // 用宽松包围盒筛掉远离嘴唇的像素，留出曲线插值的余量。
    vec2 lipBounds = lipRadius * 1.5;
    vec2 lipDelta = abs(textureCoordinate - lipCenter);
    // 当前显示黑白遮罩，区域外直接输出黑色。
    if (faceCenterReady < 0.5 || lipDelta.x > lipBounds.x || lipDelta.y > lipBounds.y) {
        gl_FragColor = sourceColor;
        return;
    }

    // 分别计算外唇和内嘴的内外状态、边界距离。
    vec2 outerInfo = getPolygonInfo(textureCoordinate, smoothOuterLipPoints);
    vec2 innerInfo = getPolygonInfo(textureCoordinate, smoothInnerLipPoints);

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
