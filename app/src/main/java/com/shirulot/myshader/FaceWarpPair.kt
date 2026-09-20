package com.shirulot.myshader

/** 一组瘦脸局部形变使用的轮廓起点和内部目标点。 */
data class FaceWarpPair(
    val origin: FacePoint,
    val target: FacePoint,
)