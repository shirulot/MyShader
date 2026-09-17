package com.shirulot.myshader

import android.content.res.Resources
import android.graphics.BitmapFactory
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixelSourceImage
import kotlin.math.abs


object FaceAnalysis {

    // 左眼中心
    private const val LEFT_EYE_CENTER_INDEX = 74

    // 右眼中心
    private const val RIGHT_EYE_CENTER_INDEX = 77

    // 左眼关键点
    private val LEFT_EYE_BOUNDARY_INDICES = intArrayOf(52, 53, 54, 55, 56, 57, 72, 73)

    // 右眼关键点
    private val RIGHT_EYE_BOUNDARY_INDICES = intArrayOf(58, 59, 60, 61, 62, 63, 75, 76)

    // 外嘴唇轮廓关键点。
    private val LIP_BOUNDARY_INDICES = intArrayOf(84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95)

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
    private fun calculateFeatureRegion(landmarks: FloatArray, centerIndex: Int, boundaryIndices: IntArray): FaceFeatureRegion {
        val center = getPoint(landmarks, centerIndex)
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
        val left = calculateFeatureRegion(landmarks, LEFT_EYE_CENTER_INDEX, LEFT_EYE_BOUNDARY_INDICES)
        val right = calculateFeatureRegion(landmarks, RIGHT_EYE_CENTER_INDEX, RIGHT_EYE_BOUNDARY_INDICES)
        return left to right
    }

    fun getPoint(landmarks: FloatArray, pointIndex: Int): FacePoint {
        val offset = pointIndex * 2
        require(offset + 1 < landmarks.size) { "关键点索引越界：$pointIndex" }
        return FacePoint(landmarks[offset], landmarks[offset + 1])
    }

    fun calculateEyeCenters(landmarks: FloatArray): Pair<FacePoint, FacePoint> {
        return getPoint(landmarks, LEFT_EYE_CENTER_INDEX) to getPoint(landmarks, RIGHT_EYE_CENTER_INDEX)
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

    fun detectLessonFace(resources: Resources): FloatArray {
        val bitmap = requireNotNull(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.lesson_face,
                BitmapFactory.Options().apply { inScaled = false },
            ),
        )

        // 使用 GPUPixel 原生路径转换 Bitmap，保证输入为 RGBA。
        val sourceImage = GPUPixelSourceImage.CreateFromBitmap(bitmap)
        // 静态练习图必须以图片模式创建检测器。
        val detector = FaceDetector.Create(FaceDetector.GPUPIXEL_MODE_FMT_PICTURE)

        return try {
            detector.detect(
                requireNotNull(sourceImage.GetRgbaImageBuffer()),
                sourceImage.GetWidth(),
                sourceImage.GetHeight(),
                sourceImage.GetWidth() * 4,
                FaceDetector.GPUPIXEL_MODE_FMT_PICTURE,
                FaceDetector.GPUPIXEL_FRAME_TYPE_RGBA,
            )
        } finally {
            detector.destroy()
            sourceImage.Destroy()
            bitmap.recycle()
        }
    }

    private const val REQUIRED_LANDMARK_COUNT = 111

    fun hasValidLandmarks(landmarks: FloatArray): Boolean {
        return landmarks.size >= REQUIRED_LANDMARK_COUNT * 2 &&
                landmarks.size % 2 == 0 &&
                landmarks.all { it.isFinite() && it in 0f..1f }
    }
}
