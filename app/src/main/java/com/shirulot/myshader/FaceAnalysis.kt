package com.shirulot.myshader

import kotlin.math.abs


object FaceAnalysis {

    // MediaPipe 面部轮廓到鼻部中线的语义映射，并非 Mars 索引的一一对应。
    // 保留原来的九组参数布局：左右四层轮廓 + 下巴，Shader 当前仍只使用前八组。
    private val SLIM_FACE_INDEX_PAIRS = arrayOf(
        // 左外侧
        234 to 168,
        // 右外侧
        454 to 168,
        // 左中侧
        132 to 6,
        // 右中侧
        361 to 6,
        // 左内侧
        172 to 197,
        // 右内侧
        397 to 197,
        // 左下颌
        150 to 1,
        // 右下颌
        379 to 1,
        // 下巴
        152 to 1,
    )
    // 沿用画面左/右的命名；对应 MediaPipe 人物自身的右/左眼。
    private val LEFT_EYE_BOUNDARY_INDICES = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
    private val RIGHT_EYE_BOUNDARY_INDICES = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362, 398, 384, 385, 386, 387, 388, 466)

    // 外嘴唇轮廓关键点。
    private val LIP_BOUNDARY_INDICES = intArrayOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185)

    /**
     * 计算嘴唇
     */
    fun calculateLipRegion(landmarks: FloatArray): FaceFeatureRegion {
        var minX = 1f
        var maxX = 0f
        var minY = 1f
        var maxY = 0f

        for (index in LIP_BOUNDARY_INDICES) {
            val point = getPoint(landmarks, index)
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }

        val center = FacePoint((minX + maxX) * 0.5f, (minY + maxY) * 0.5f)
        val radius = FacePoint((maxX - minX) * 0.5f, (maxY - minY) * 0.5f)
        return FaceFeatureRegion(center, radius)
    }
    private fun calculateFeatureRegion(landmarks: FloatArray, center: FacePoint, boundaryIndices: IntArray): FaceFeatureRegion {
        var radiusX = 0f
        var radiusY = 0f

        for (index in boundaryIndices) {
            val point = getPoint(landmarks, index)
            radiusX = maxOf(radiusX, abs(point.x - center.x))
            radiusY = maxOf(radiusY, abs(point.y - center.y))
        }

        return FaceFeatureRegion(center, FacePoint(radiusX.coerceAtLeast(0.001f), radiusY.coerceAtLeast(0.001f)))
    }

    /**
     * 计算眼睛区域
     */
    fun calculateEyeRegions(landmarks: FloatArray): Pair<FaceFeatureRegion, FaceFeatureRegion> {
        val centers = calculateEyeCenters(landmarks)
        val left = calculateFeatureRegion(landmarks, centers.first, LEFT_EYE_BOUNDARY_INDICES)
        val right = calculateFeatureRegion(landmarks, centers.second, RIGHT_EYE_BOUNDARY_INDICES)
        return left to right
    }

    fun getPoint(landmarks: FloatArray, pointIndex: Int): FacePoint {
        val offset = pointIndex * 2
        require(pointIndex >= 0 && offset + 1 < landmarks.size) { "关键点索引越界：$pointIndex" }
        return FacePoint(landmarks[offset], landmarks[offset + 1])
    }

    fun calculateEyeCenters(landmarks: FloatArray): Pair<FacePoint, FacePoint> {
        // 用眼角中点而非虹膜中心，避免视线变化移动形变中心。
        fun midpoint(first: Int, second: Int): FacePoint {
            val a = getPoint(landmarks, first)
            val b = getPoint(landmarks, second)
            return FacePoint((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        }
        return midpoint(33, 133) to midpoint(362, 263)
    }

    fun calculateFaceRegion(landmarks: FloatArray): FaceRegion {
        require(landmarks.size >= 2 && landmarks.size % 2 == 0)

        var minX = 1f
        var maxX = 0f
        var minY = 1f
        var maxY = 0f

        for (index in landmarks.indices step 2) {
            minX = minOf(minX, landmarks[index])
            maxX = maxOf(maxX, landmarks[index])
            minY = minOf(minY, landmarks[index + 1])
            maxY = maxOf(maxY, landmarks[index + 1])
        }

        // 用关键点包围盒中心作为脸部区域中心。
        return FaceRegion(
            centerX = (minX + maxX) * 0.5f,
            centerY = (minY + maxY) * 0.5f,
            width = (maxX - minX).coerceAtLeast(0.01f),
            height = (maxY - minY).coerceAtLeast(0.01f),
        )
    }

    private const val REQUIRED_LANDMARK_COUNT = 478

    /** 保留检测到的真实人数，只有恰好一张有效脸才能驱动当前单脸 Shader。 */
    fun singleFaceOrNull(faces: List<FloatArray>): FloatArray? =
        faces.singleOrNull()?.takeIf(::hasValidLandmarks)

    fun hasValidLandmarks(landmarks: FloatArray): Boolean {
        // 当前渲染链只接收完整的一张脸，拒绝多脸拼接数据，避免脸框和五官串用。
        return landmarks.size == REQUIRED_LANDMARK_COUNT * 2 &&
                landmarks.size % 2 == 0 &&
                landmarks.all { it.isFinite() && it in 0f..1f }
    }

    /** 从 SDK 关键点中读取全部瘦脸形变点对。 */
    fun calculateSlimFacePairs(landmarks: FloatArray): List<FaceWarpPair> {
        require(hasValidLandmarks(landmarks)) { "瘦脸关键点数据无效" }

        return SLIM_FACE_INDEX_PAIRS.map { (originIndex, targetIndex) ->
            FaceWarpPair(
                origin = getPoint(landmarks, originIndex),
                target = getPoint(landmarks, targetIndex),
            )
        }
    }
}
