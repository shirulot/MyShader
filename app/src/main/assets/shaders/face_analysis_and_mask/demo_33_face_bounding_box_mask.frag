precision mediump float;
uniform sampler2D inputImageTexture;
uniform vec2 faceCenter;
// 关键点包围盒的宽高，已归一化到 UV 空间。
uniform vec2 faceSize;
uniform float faceCenterReady;

varying vec2 textureCoordinate;

// 肤色的红蓝色差 - 肤色识别
float SKIN_DIFF_RB_START = 0.04;
float SKIN_DIFF_RB_END = 0.16;

// 肤色的红绿色差 - 肤色识别
float SKIN_DIFF_RG_START = 0.00;
float SKIN_DIFF_RG_END = 0.08;

//float FACE_X_SCALE = 1.65;
//float FACE_INNER_RADIUS = 0.18;
//float FACE_OUTER_RADIUS = 0.28;

void main() {
    // 原图直通：采样结果不做任何颜色处理。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);

    // 拿出肤色权重
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, centerColor.r - centerColor.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, centerColor.r - centerColor.g);
    float skinWeight = min(rbWeight, rgWeight);

    // UV 的 y 轴向下，因此减小 y 值会把候选区域向额头方向上移。
    vec2 faceMaskCenter = faceCenter - vec2(0.0, faceSize.y * 0.10);

    // 横向仅略扩，纵向更多覆盖额头与下巴。
    vec2 faceMaskSize = faceSize * vec2(1.08, 1.35);

    vec2 faceUv = (textureCoordinate - faceMaskCenter) / (faceMaskSize * 0.5);
    // 除以半宽高后，length 为 1.0 就是实际人脸椭圆边缘。
//    vec2 faceUv = (textureCoordinate - faceCenter) / (faceSize * 0.5);
    float faceDistance = length(faceUv);
    float faceRegionWeight = (1.0 - smoothstep(0.75, 1.0, faceDistance)) * faceCenterReady;

    float faceSkinWeight = skinWeight * faceRegionWeight;
    vec3 resultRgb = mix(centerColor.rgb, vec3(1.0, 0.0, 0.0), faceSkinWeight * 0.65);
    gl_FragColor = vec4(resultRgb, centerColor.a);

}
