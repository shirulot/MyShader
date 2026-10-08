precision mediump float;
uniform sampler2D inputImageTexture;
// 独立腮红滑条控制混色强度，范围为 0.00--0.30。
uniform float blushStrength;
// 独立范围滑条表示渐变带宽度，最小值 0.40 对应渐变起点 0.60。
uniform float blushRange;
varying vec2 textureCoordinate;

// Android 根据 SDK 关键点计算的画面左侧腮红区域。
uniform vec2 leftBlushCenter;
uniform vec2 leftBlushRadius;
uniform vec2 rightBlushCenter;
uniform vec2 rightBlushRadius;
// 标记当前人脸框数据是否可用。
uniform float faceCenterReady;

// 选择一个固定的口红目标色。
vec3 lipstickColor = vec3(0.78, 0.06, 0.16);

float getBlushWeight(vec2 uv, vec2 center, vec2 radius){
    radius = max(radius, vec2(0.001));
    // 计算归一化的椭圆距离。当前uv坐标距离圆心有几个半径的距离 如果大于半径则会大于1 所以这里最后要取反
    float blushDistance = length((uv - center) / radius);
    // 渐变宽度限制在 0.40 到 1.00，不改变关键点计算出的椭圆半径。
    float safeBlushRange = clamp(blushRange, 0.4, 1.0);
    // 滑条显示 0.40 时，从距离 0.60 开始淡出，到 1.0 时归零。
    float fadeStart = 1.0 - safeBlushRange;
    float blushWeight = (1.0 - smoothstep(fadeStart, 1.0, blushDistance)) * faceCenterReady;
    return blushWeight;
}

void main() {
    vec2 uv = textureCoordinate;
    // 读取原图 alpha，保持 inputImageTexture 处于实际使用状态。
    vec4 sourceColor = texture2D(inputImageTexture, uv);
    float leftBlushWeight = getBlushWeight(uv, leftBlushCenter, leftBlushRadius);
    float rightBlushWeight = getBlushWeight(uv, rightBlushCenter, rightBlushRadius);
    float blushWeight = max(leftBlushWeight, rightBlushWeight);
    // 限制独立腮红强度，0.00 时恢复原图。
    float safeBlushStrength = clamp(blushStrength, 0.0, 0.3);
    // 保留当前预览色，用腮红滑条控制区域内的混色量。
    vec3 previewColor = mix(sourceColor.rgb, lipstickColor, blushWeight * safeBlushStrength);
    gl_FragColor = vec4(previewColor, sourceColor.a);
}
