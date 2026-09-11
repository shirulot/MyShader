precision mediump float;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;

//float FACE_X_SCALE = 1.65;
//float FACE_INNER_RADIUS = 0.18;
//float FACE_OUTER_RADIUS = 0.28;

void main() {
    //（0,0）   （0.5,0）   （1,0）
    //（0,0.5） （0.5,0.5） （1,0.5）
    //（0,1）   （0.5,1）   （1,1）
    //
    //        ↓
    //
    //（1,1）    （1,0.5）    （1,0）
    //（0.5,1）  （0.5,0.5）  （0.5,0）
    //（0,1）    （0,0.5）    （0,0）

    // 先将输入纹理顺时针旋转 90 度。
    vec2 rotatedUv = vec2(textureCoordinate.y, 1.0 - textureCoordinate.x);

    // 再按前摄预览常见需求做左右镜像。
    vec2 frontCameraUv = vec2(1.0 - rotatedUv.x, rotatedUv.y);

//    vec2   frontCameraUv = vec2(textureCoordinate.y, textureCoordinate.x);
    // 使用组合变换后的坐标采样输入纹理。
    vec4 centerColor = texture2D(inputImageTexture, frontCameraUv);
    // 原图直通：采样结果不做任何颜色处理。
    //    vec4 centerColor = texture2D(inputImageTexture, rotatedUv);

//    // 将静态输入纹理的采样颜色直接输出到屏幕。
//    gl_FragColor = centerColor;
    // 标出输入纹理左上角附近，便于判断组合方向。
    vec3 outputColor = centerColor.rgb;
    if (frontCameraUv.x < 0.15 && frontCameraUv.y < 0.15) {
        outputColor = vec3(1.0, 0.0, 1.0);
    }
    gl_FragColor = vec4(outputColor, centerColor.a);
}
