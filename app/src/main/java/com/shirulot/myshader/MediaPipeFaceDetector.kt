package com.shirulot.myshader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

/** 静态练习图检测入口：每张脸独立保存，不能把多脸拼成一个关键点数组。 */
object MediaPipeFaceDetector {
    const val MAX_FACES = 6
    private const val MODEL_PATH = "models/face_landmarker.task"

    /** 由页面的后台协程调用；串行执行，快速切图不会同时创建多套推理资源。 */
    @Synchronized
    fun detect(context: Context, @DrawableRes imageRes: Int): List<FloatArray> {
        val bitmap = requireNotNull(BitmapFactory.decodeResource(
            context.resources,
            imageRes,
            BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ))
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_PATH).build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(MAX_FACES)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .build()
            // 当前只在切图时检测；每次调用结束即释放，避免页面销毁后的原生资源残留。
            FaceLandmarker.createFromOptions(context.applicationContext, options).use { detector ->
                val image = BitmapImageBuilder(bitmap).build()
                try {
                    return detector.detect(image).faceLandmarks().map { face ->
                        // 当前纹理与模型都以左上为原点，直接使用归一化 x/y；暂不使用 z。
                        FloatArray(face.size * 2) { index ->
                            val point = face[index / 2]
                            if (index % 2 == 0) point.x() else point.y()
                        }
                    }
                } finally {
                    image.close()
                }
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
