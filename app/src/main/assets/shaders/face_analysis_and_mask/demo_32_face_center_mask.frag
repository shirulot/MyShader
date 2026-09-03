precision mediump float;
uniform sampler2D inputImageTexture;
uniform vec2 faceCenter;
uniform float faceCenterReady;
varying vec2 textureCoordinate;

// 肤色的红蓝色差 - 肤色识别
float SKIN_DIFF_RB_START = 0.04;
float SKIN_DIFF_RB_END = 0.16;

// 肤色的红绿色差 - 肤色识别
float SKIN_DIFF_RG_START = 0.00;
float SKIN_DIFF_RG_END = 0.08;

float FACE_X_SCALE = 1.65;
float FACE_INNER_RADIUS = 0.18;

void main() {
    // 原图直通：采样结果不做任何颜色处理。
    vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);

    // 拿出肤色权重
    float rbWeight = smoothstep(SKIN_DIFF_RB_START, SKIN_DIFF_RB_END, centerColor.r - centerColor.b);
    float rgWeight = smoothstep(SKIN_DIFF_RG_START, SKIN_DIFF_RG_END, centerColor.r - centerColor.g);
    float skinWeight = min(rbWeight, rgWeight);

    //    // 用 UV 四条边围出中间的人脸候选区域。
    //    float leftWeight = smoothstep(0.22, 0.30, textureCoordinate.x);
    //    float rightWeight = 1.0 - smoothstep(0.70, 0.78, textureCoordinate.x);
    //    float topWeight = smoothstep(0.12, 0.22, textureCoordinate.y);
    //    float bottomWeight = 1.0 - smoothstep(0.76, 0.86, textureCoordinate.y);
    //
    //    float faceRegionWeight = leftWeight * rightWeight * topWeight * bottomWeight;

    //  将 UV 移到脸部中心，并压窄横向范围形成椭圆。
    vec2 faceUv = textureCoordinate - faceCenter;
    faceUv.x *= FACE_X_SCALE;
    float faceDistance = length(faceUv);
    float faceRegionWeight = (1.0 - smoothstep(FACE_INNER_RADIUS, 0.28, faceDistance)) * faceCenterReady;


    float faceSkinWeight = skinWeight * faceRegionWeight;
    vec3 resultRgb = mix(centerColor.rgb, vec3(1.0, 0.0, 0.0), faceSkinWeight * 0.65);
    gl_FragColor = vec4(resultRgb, centerColor.a);

}
