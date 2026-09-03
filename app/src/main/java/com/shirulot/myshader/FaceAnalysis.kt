package com.shirulot.myshader

import android.content.res.Resources
import android.graphics.BitmapFactory
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixelSourceImage


object FaceAnalysis {


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
        return  FaceRegion(
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
}
