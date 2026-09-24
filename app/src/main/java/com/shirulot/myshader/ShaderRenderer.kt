package com.shirulot.myshader

import android.content.Context
import android.graphics.*
import android.opengl.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 最小 OpenGL ES 2.0 容器：加载独立的 .vert/.frag，上传一张本地图片并画一个矩形。
 * 这里不接 JNI，目的是让 Shader 文件成为唯一需要频繁修改的地方。
 */
class ShaderSurfaceView(
    context: Context,
    fragmentShaderAsset: String,
    onStatusChanged: (String) -> Unit,
) : GLSurfaceView(context) {
    private val shaderRenderer = ShaderRenderer(
        context = context.applicationContext,
        fragmentShaderAsset = fragmentShaderAsset,
        onStatusChanged = onStatusChanged,
    )

    init {
        // 使用 GLES 2.0，才能直接学习 varying、texture2D 和 gl_FragColor。
        setEGLContextClientVersion(2)
        setPreserveEGLContextOnPause(true)
        setRenderer(shaderRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun release() {
        // GLSurfaceView 的 OpenGL 资源只能在 GL 线程释放。
        queueEvent { shaderRenderer.release() }
    }

    fun setBlurStrength(value: Float) {
        // SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setBlurStrength(value) }
    }

    fun setWhitenStrength(value: Float) {
        // SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setWhitenStrength(value) }
    }

    fun setBrightenStrength(value: Float) {
        // SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setBrightenStrength(value) }
    }

    fun setSlimFacePairs(pairs: List<FaceWarpPair>) {
        // 人脸轮廓数据统一切换到 GL 线程保存。
        queueEvent { shaderRenderer.setSlimFacePairs(pairs) }
    }

    fun setBlackCircleStrength(value: Float) {
        // 黑眼圈 SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setBlackCircleStrength(value) }
    }

    fun setBigEyeStrength(value: Float) {
        // 大眼 SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setBigEyeStrength(value) }
    }

    fun setSlimFaceStrength(value: Float) {
        // 瘦脸 SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setSlimFaceStrength(value) }
    }

    fun setLipstickStrength(value: Float) {
        // 口红 SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setLipstickStrength(value) }
    }

    fun setWarmthStrength(value: Float) {
        // SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setWarmthStrength(value) }
    }

    fun setSaturationStrength(value: Float) {
        // SeekBar 回调来自主线程，uniform 状态必须在 GL 线程更新。
        queueEvent { shaderRenderer.setSaturationStrength(value) }
    }

    fun setFaceCenterRegion(centerX: Float, centerY: Float, width: Float, height: Float) {
        // 人脸结果来自后台线程，uniform 状态仍只在 GL 线程更新。
        queueEvent { shaderRenderer.setFaceCenter(centerX, centerY, width, height) }
    }

    fun setSourceImage(@DrawableRes sourceImageRes: Int) {
        // 切换图片会创建和删除 OpenGL 纹理，因此同样只能在 GL 线程执行。
        queueEvent {
            // 新纹理与清除旧人脸状态在同一个 GL 事件中执行。
            shaderRenderer.clearFaceAnalysis()
            shaderRenderer.setSourceImage(sourceImageRes)
        }
    }

    /** 同一张图片的检测数据一次性更新，全部就绪后再启用人脸效果。 */
    fun setFaceAnalysis(
        face: FaceRegion,
        eyes: Pair<FaceFeatureRegion, FaceFeatureRegion>,
        lip: FaceFeatureRegion,
        innerLip: FaceFeatureRegion,
        slimPairs: List<FaceWarpPair>,
        lipContours: Pair<FloatArray, FloatArray>,
    ) {
        queueEvent {
            shaderRenderer.clearFaceAnalysis()
            shaderRenderer.setSlimFacePairs(slimPairs)
            shaderRenderer.setEyeRegions(eyes.first, eyes.second)
            shaderRenderer.setLipRegions(lip)
            shaderRenderer.setInnerLipRegions(innerLip)
            shaderRenderer.setLipContours(lipContours)
            shaderRenderer.setFaceCenter(face.centerX, face.centerY, face.width, face.height)
        }
    }

    fun setEyeRegions(leftEyeRegion: FaceFeatureRegion, rightEyeRegion: FaceFeatureRegion) {
        // 人脸分析结果统一在 GL 线程更新。
        queueEvent { shaderRenderer.setEyeRegions(leftEyeRegion, rightEyeRegion) }
    }

    fun setLipRegions(lipRegion: FaceFeatureRegion) {
        // 人脸分析结果统一在 GL 线程更新。
        queueEvent { shaderRenderer.setLipRegions(lipRegion) }
    }

    fun setInnerLipRegions(innerLipRegion: FaceFeatureRegion) {
        // 内嘴区域与外唇区域一样，只在 GL 线程更新，供后续 Shader 扣除口腔权重。
        queueEvent { shaderRenderer.setInnerLipRegions(innerLipRegion) }
    }

    fun clearFaceAnalysis() {
        // 检测失败后在 GL 线程关闭所有依赖人脸区域的效果。
        queueEvent { shaderRenderer.clearFaceAnalysis() }
    }

}

private class ShaderRenderer(
    private val context: Context,
    private val fragmentShaderAsset: String,
    private val onStatusChanged: (String) -> Unit,
) : GLSurfaceView.Renderer {
    // 依次保存 9 组瘦脸轮廓起点和内部目标点。
    private val slimOrigins = FloatArray(SLIM_FACE_PAIR_COUNT * 2)
    private val slimTargets = FloatArray(SLIM_FACE_PAIR_COUNT * 2)

    // 尚未拿到人脸检测结果时，不显示局部脸部区域。
    private var faceCenterReady = false

    //人脸中心点
    private var faceCenterX = 0.5f
    private var faceCenterY = 0.5f
    private var faceWidth = 0.3f
    private var faceHeight = 0.4f

    private var leftEyeRegion = FaceFeatureRegion(FacePoint(0.36f, 0.41f), FacePoint(0.062f, 0.016f))
    private var rightEyeRegion = FaceFeatureRegion(FacePoint(0.65f, 0.41f), FacePoint(0.062f, 0.016f))
    private var lipRegion = FaceFeatureRegion(FacePoint(0.50f, 0.62f), FacePoint(0.112f, 0.029f))
    private var innerLipRegion = FaceFeatureRegion(FacePoint(0.50f, 0.62f), FacePoint(0.04f, 0.012f))
    // 两条闭合轮廓各包含 20 个点，未检测时由 faceCenterReady 关闭效果。
    private val outerLipPoints = FloatArray(40)
    private val innerLipPoints = FloatArray(40)
    // 平滑轮廓使用独立 uniform，保持旧版 20 点 Demo 的数据布局。
    private var smoothOuterLipPoints = FloatArray(LipContourSmoother.OUTPUT_POINT_COUNT * 2)
    private var smoothInnerLipPoints = FloatArray(LipContourSmoother.OUTPUT_POINT_COUNT * 2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val positionBuffer = createFloatBuffer(
        floatArrayOf(
            -1f, -1f, 0f,
            1f, -1f, 0f,
            -1f, 1f, 0f,
            1f, 1f, 0f,
        ),
    )
    private val textureCoordinateBuffer = createFloatBuffer(
        floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
        ),
    )

    private var program = 0
    private var textureId = 0
    private var image: Bitmap? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    @DrawableRes
    private var sourceImageRes = R.drawable.lesson_face
    private var whitenStrength = DEFAULT_WHITEN_STRENGTH
    private var brightenStrength = DEFAULT_BRIGHTEN_STRENGTH
    private var blackCircleStrength = DEFAULT_BLACK_CIRCLE_STRENGTH
    private var bigEyeStrength = DEFAULT_BIG_EYE_STRENGTH
    private var slimFaceStrength = DEFAULT_SLIM_FACE_STRENGTH
    private var lipstickStrength = DEFAULT_LIPSTICK_STRENGTH

    private var warmthStrength = DEFAULT_WARMTH_STRENGTH
    private var saturationStrength = DEFAULT_SATURATION_STRENGTH
    private var blurStrength = DEFAULT_BLUR_STRENGTH

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        val clearColor = ContextCompat.getColor(context, R.color.shader_demo_background)
        GLES20.glClearColor(
            Color.red(clearColor) / 255f,
            Color.green(clearColor) / 255f,
            Color.blue(clearColor) / 255f,
            Color.alpha(clearColor) / 255f,
        )

        try {
            val vertexSource = readShaderAsset("main.vert")
            // 首页选中的资源只替换片元 Shader，顶点与纹理链保持一致。
            val fragmentSource = readShaderAsset(fragmentShaderAsset)
            program = createProgram(vertexSource, fragmentSource)
            image = loadSourceImage(sourceImageRes)
            textureId = createTexture(image ?: error("测试图片为空"))
            updateQuadForAspectRatio()
            postStatus(
                context.getString(
                    R.string.shader_demo_status_ready,
                    fragmentShaderAsset.substringAfterLast('/'),
                ),
            )
        } catch (error: Exception) {
            program = 0
            textureId = 0
            postStatus(
                context.getString(
                    R.string.shader_demo_status_error,
                    error.message ?: error.javaClass.simpleName,
                ),
            )
            Log.e(TAG, "Shader 初始化失败", error)
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES20.glViewport(0, 0, width, height)
        updateQuadForAspectRatio()
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0 || textureId == 0) {
            return
        }

        GLES20.glUseProgram(program)

        val positionLocation = GLES20.glGetAttribLocation(program, "position")
        val textureCoordinateLocation =
            GLES20.glGetAttribLocation(program, "inputTextureCoordinate")
        val textureLocation = GLES20.glGetUniformLocation(program, "inputImageTexture")
        val whitenStrengthLocation = GLES20.glGetUniformLocation(program, "whitenStrength")
        val brightenStrengthLocation = GLES20.glGetUniformLocation(program, "brightenStrength")
        val blackCircleStrengthLocation = GLES20.glGetUniformLocation(program, "blackCircleStrength")
        val bigEyeStrengthLocation = GLES20.glGetUniformLocation(program, "bigEyeStrength")
        val slimFaceStrengthLocation = GLES20.glGetUniformLocation(program, "slimFaceStrength")
        val lipstickStrengthLocation = GLES20.glGetUniformLocation(program, "lipstickStrength")
        val blurStrengthLocation = GLES20.glGetUniformLocation(program, "blurStrength")
        val warmthStrengthLocation = GLES20.glGetUniformLocation(program, "warmthStrength")
        val saturationStrengthLocation = GLES20.glGetUniformLocation(program, "saturationStrength")
        val faceCenterLocation = GLES20.glGetUniformLocation(program, "faceCenter")
        val faceCenterReadyLocation = GLES20.glGetUniformLocation(program, "faceCenterReady")
        val faceSizeLocation = GLES20.glGetUniformLocation(program, "faceSize")

        val leftEyeRadiusLocation = GLES20.glGetUniformLocation(program, "leftEyeRadius")
        val rightEyeRadiusLocation = GLES20.glGetUniformLocation(program, "rightEyeRadius")

        val leftEyeCenterLocation = GLES20.glGetUniformLocation(program, "leftEyeCenter")
        val rightEyeCenterLocation = GLES20.glGetUniformLocation(program, "rightEyeCenter")

        val lipCenterLocation = GLES20.glGetUniformLocation(program, "lipCenter")
        val lipRadiusLocation = GLES20.glGetUniformLocation(program, "lipRadius")
        val innerLipCenterLocation = GLES20.glGetUniformLocation(program, "innerLipCenter")
        val innerLipRadiusLocation = GLES20.glGetUniformLocation(program, "innerLipRadius")
        // 数组从首元素查询；旧 Shader 未声明时返回 -1，跳过上传。
        val outerLipPointsLocation = GLES20.glGetUniformLocation(program, "outerLipPoints[0]")
        val innerLipPointsLocation = GLES20.glGetUniformLocation(program, "innerLipPoints[0]")
        val smoothOuterLocation = GLES20.glGetUniformLocation(program, "smoothOuterLipPoints[0]")
        val smoothInnerLocation = GLES20.glGetUniformLocation(program, "smoothInnerLipPoints[0]")

        //瘦脸-脸轮廓点
        val slimOriginsLocation = GLES20.glGetUniformLocation(program, "slimOrigins[0]")
        val slimTargetsLocation = GLES20.glGetUniformLocation(program, "slimTargets[0]")

        if (positionLocation < 0 || textureCoordinateLocation < 0 || textureLocation < 0) {
            postStatus(context.getString(R.string.shader_demo_status_interface_error))
            return
        }

        positionBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            positionBuffer,
        )

        textureCoordinateBuffer.position(0)
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
        GLES20.glVertexAttribPointer(
            textureCoordinateLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            textureCoordinateBuffer,
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureLocation, 0)
        // 只有美白 Demo 声明 whitenStrength；其他 Shader 返回 -1，保持原有行为。
        if (whitenStrengthLocation >= 0) {
            GLES20.glUniform1f(whitenStrengthLocation, whitenStrength)
        }
        // 只有提亮 Shader 声明 brightenStrength；其他 Shader 返回 -1，保持原有行为。
        if (brightenStrengthLocation >= 0) {
            GLES20.glUniform1f(brightenStrengthLocation, brightenStrength)
        }
        // 只有声明 blackCircleStrength 的 Shader 才接收黑眼圈提亮系数。
        if (blackCircleStrengthLocation >= 0) {
            GLES20.glUniform1f(blackCircleStrengthLocation, blackCircleStrength)
        }
        // 只有声明 bigEyeStrength 的 Shader 才接收大眼强度。
        if (bigEyeStrengthLocation >= 0) {
            GLES20.glUniform1f(bigEyeStrengthLocation, bigEyeStrength)
        }

        // 只有声明 slimFaceStrength 的 Shader 才接收瘦脸强度。
        if (slimFaceStrengthLocation >= 0) {
            GLES20.glUniform1f(slimFaceStrengthLocation, slimFaceStrength)
        }
        // 只有声明 lipstickStrength 的 Shader 才接收口红强度。
        if (lipstickStrengthLocation >= 0) {
            GLES20.glUniform1f(lipstickStrengthLocation, lipstickStrength)
        }
        // 只有磨皮声明 blurStrength；其他 Shader 返回 -1，保持原有行为。
        if (blurStrengthLocation >= 0) {
            GLES20.glUniform1f(blurStrengthLocation, blurStrength)
        }

        // 只有声明 warmthStrength 的 Shader 才接收暖色强度。
        if (warmthStrengthLocation >= 0) {
            GLES20.glUniform1f(warmthStrengthLocation, warmthStrength)
        }
        // 只有声明 saturationStrength 的 Shader 才接收饱和度强度。
        if (saturationStrengthLocation >= 0) {
            GLES20.glUniform1f(saturationStrengthLocation, saturationStrength)
        }
        // 面部中心点
        if (faceCenterLocation >= 0) {
            GLES20.glUniform2f(faceCenterLocation, faceCenterX, faceCenterY)
        }

        if (leftEyeCenterLocation >= 0) GLES20.glUniform2f(leftEyeCenterLocation, leftEyeRegion.center.x, leftEyeRegion.center.y)
        if (rightEyeCenterLocation >= 0) GLES20.glUniform2f(rightEyeCenterLocation, rightEyeRegion.center.x, rightEyeRegion.center.y)
        if (leftEyeRadiusLocation >= 0) GLES20.glUniform2f(leftEyeRadiusLocation, leftEyeRegion.radius.x, leftEyeRegion.radius.y)
        if (rightEyeRadiusLocation >= 0) GLES20.glUniform2f(rightEyeRadiusLocation, rightEyeRegion.radius.x, rightEyeRegion.radius.y)
        if (lipRadiusLocation >= 0) GLES20.glUniform2f(lipRadiusLocation, lipRegion.radius.x, lipRegion.radius.y)
        if (lipCenterLocation >= 0) GLES20.glUniform2f(lipCenterLocation, lipRegion.center.x, lipRegion.center.y)
        if (innerLipRadiusLocation >= 0) GLES20.glUniform2f(innerLipRadiusLocation, innerLipRegion.radius.x, innerLipRegion.radius.y)
        if (innerLipCenterLocation >= 0) GLES20.glUniform2f(innerLipCenterLocation, innerLipRegion.center.x, innerLipRegion.center.y)
        // 每个点使用原检测 UV，不再压缩成椭圆中心和半径。
        if (outerLipPointsLocation >= 0) GLES20.glUniform2fv(outerLipPointsLocation, 20, outerLipPoints, 0)
        if (innerLipPointsLocation >= 0) GLES20.glUniform2fv(innerLipPointsLocation, 20, innerLipPoints, 0)
        // 仅在 Shader 使用平滑数组时上传，原始轮廓仍可用于对照。
        if (smoothOuterLocation >= 0) GLES20.glUniform2fv(smoothOuterLocation, LipContourSmoother.OUTPUT_POINT_COUNT, smoothOuterLipPoints, 0)
        if (smoothInnerLocation >= 0) GLES20.glUniform2fv(smoothInnerLocation, LipContourSmoother.OUTPUT_POINT_COUNT, smoothInnerLipPoints, 0)

        if (faceCenterReadyLocation >= 0) {
            GLES20.glUniform1f(faceCenterReadyLocation, if (faceCenterReady) 1f else 0f)
        }
        if (faceSizeLocation >= 0) {
            GLES20.glUniform2f(faceSizeLocation, faceWidth, faceHeight)
        }

        // 每两个 Float 组成一个 vec2，一次上传全部 9 个轮廓起点。
        if (slimOriginsLocation >= 0) {
            GLES20.glUniform2fv(slimOriginsLocation, SLIM_FACE_PAIR_COUNT, slimOrigins, 0)
        }

        // 每两个 Float 组成一个 vec2，一次上传全部 9 个内部目标点。
        if (slimTargetsLocation >= 0) {
            GLES20.glUniform2fv(slimTargetsLocation, SLIM_FACE_PAIR_COUNT, slimTargets, 0)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(textureCoordinateLocation)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }


    fun release() {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        image?.recycle()
        image = null
    }

    fun setFaceCenter(centerX: Float, centerY: Float, width: Float, height: Float) {
        faceCenterX = centerX.coerceIn(0f, 1f)
        faceCenterY = centerY.coerceIn(0f, 1f)
        faceWidth = width.coerceIn(0.01f, 1f)
        faceHeight = height.coerceIn(0.01f, 1f)
        faceCenterReady = true
    }

    fun setBlurStrength(value: Float) {
        blurStrength = value.coerceIn(MIN_BLUR_STRENGTH, MAX_BLUR_STRENGTH)
    }

    fun setWhitenStrength(value: Float) {
        whitenStrength = value.coerceIn(MIN_WHITEN_STRENGTH, MAX_WHITEN_STRENGTH)
    }

    fun setBrightenStrength(value: Float) {
        brightenStrength = value.coerceIn(MIN_BRIGHTEN_STRENGTH, MAX_BRIGHTEN_STRENGTH)
    }

    fun setBlackCircleStrength(value: Float) {
        blackCircleStrength = value.coerceIn(MIN_BLACK_CIRCLE_STRENGTH, MAX_BLACK_CIRCLE_STRENGTH)
    }

    fun setBigEyeStrength(value: Float) {
        bigEyeStrength = value.coerceIn(MIN_BIG_EYE_STRENGTH, MAX_BIG_EYE_STRENGTH)
    }

    fun setSlimFaceStrength(value: Float) {
        slimFaceStrength = value.coerceIn(MIN_SLIM_FACE_STRENGTH, MAX_SLIM_FACE_STRENGTH)
    }

    fun setLipstickStrength(value: Float) {
        lipstickStrength = value.coerceIn(MIN_LIPSTICK_STRENGTH, MAX_LIPSTICK_STRENGTH)
    }

    fun setWarmthStrength(value: Float) {
        warmthStrength = value.coerceIn(MIN_WARMTH_STRENGTH, MAX_WARMTH_STRENGTH)
    }

    fun setSaturationStrength(value: Float) {
        saturationStrength = value.coerceIn(MIN_SATURATION_STRENGTH, MAX_SATURATION_STRENGTH)
    }

    fun setSourceImage(@DrawableRes resourceId: Int) {
        if (sourceImageRes == resourceId) {
            return
        }
        sourceImageRes = resourceId
        // Surface 尚未创建时只记录选择，初始化纹理时会读取该资源。
        if (textureId == 0) {
            return
        }

        val replacementImage = loadSourceImage(resourceId)
        try {
            val replacementTexture = createTexture(replacementImage)
            val previousTextureId = textureId
            val previousImage = image
            textureId = replacementTexture
            image = replacementImage
            if (previousTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(previousTextureId), 0)
            }
            previousImage?.recycle()
            // 两张图的比例不同，切换后重新计算矩形，避免人脸被拉伸。
            updateQuadForAspectRatio()
        } catch (error: Exception) {
            replacementImage.recycle()
            throw error
        }
    }

    private fun loadSourceImage(@DrawableRes resourceId: Int): Bitmap {
        // 测试图放在 drawable-nodpi，避免 Android 按屏幕密度改变 Shader 的输入尺寸。
        val options = BitmapFactory.Options().apply { inScaled = false }
        return requireNotNull(BitmapFactory.decodeResource(context.resources, resourceId, options)) {
            "测试图片加载失败"
        }
    }

    private fun createTexture(bitmap: Bitmap): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        check(texture[0] != 0) { "无法创建 OpenGL 纹理" }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return texture[0]
    }

    private fun updateQuadForAspectRatio() {
        val currentImage = image ?: return
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            return
        }

        val imageAspect = currentImage.width.toFloat() / currentImage.height
        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight
        val xScale: Float
        val yScale: Float
        if (surfaceAspect > imageAspect) {
            xScale = imageAspect / surfaceAspect
            yScale = 1f
        } else {
            xScale = 1f
            yScale = surfaceAspect / imageAspect
        }

        positionBuffer.clear()
        positionBuffer.put(
            floatArrayOf(
                -xScale, -yScale, 0f,
                xScale, -yScale, 0f,
                -xScale, yScale, 0f,
                xScale, yScale, 0f,
            ),
        )
        positionBuffer.position(0)
    }

    private fun readShaderAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        var vertexShader = 0
        var fragmentShader = 0
        var linkedProgram = 0
        var linkSucceeded = false
        try {
            vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource, "vertex shader")
            fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource, "fragment shader")
            linkedProgram = GLES20.glCreateProgram()
            check(linkedProgram != 0) { "无法创建 OpenGL Program" }
            GLES20.glAttachShader(linkedProgram, vertexShader)
            GLES20.glAttachShader(linkedProgram, fragmentShader)
            GLES20.glLinkProgram(linkedProgram)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(linkedProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val message = GLES20.glGetProgramInfoLog(linkedProgram)
                throw IllegalStateException("Program 链接失败：$message")
            }
            linkSucceeded = true
            return linkedProgram
        } finally {
            if (vertexShader != 0) {
                GLES20.glDeleteShader(vertexShader)
            }
            if (fragmentShader != 0) {
                GLES20.glDeleteShader(fragmentShader)
            }
            if (!linkSucceeded && linkedProgram != 0) {
                GLES20.glDeleteProgram(linkedProgram)
            }
        }
    }

    private fun compileShader(type: Int, source: String, label: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "无法创建 $label" }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("$label 编译失败：$message")
        }
        return shader
    }

    private fun postStatus(message: String) {
        mainHandler.post { onStatusChanged(message) }
    }

    private fun createFloatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer
            .allocateDirect(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }

    fun setSlimFacePairs(pairs: List<FaceWarpPair>) {
        if (pairs.size != SLIM_FACE_PAIR_COUNT) {
            faceCenterReady = false
            return
        }

        pairs.forEachIndexed { index, pair ->
            val offset = index * 2
            slimOrigins[offset] = pair.origin.x
            slimOrigins[offset + 1] = pair.origin.y
            slimTargets[offset] = pair.target.x
            slimTargets[offset + 1] = pair.target.y
        }
    }

    fun setEyeRegions(leftEyeRegion: FaceFeatureRegion, rightEyeRegion: FaceFeatureRegion) {
        this.leftEyeRegion = leftEyeRegion
        this.rightEyeRegion = rightEyeRegion
    }


    fun setLipRegions(lipRegion: FaceFeatureRegion) {
        this.lipRegion = lipRegion
    }

    fun setInnerLipRegions(innerLipRegion: FaceFeatureRegion) {
        this.innerLipRegion = innerLipRegion
    }

    /** 在同一次 GL 更新中保存两条轮廓，避免与当前图片的人脸数据错配。 */
    fun setLipContours(contours: Pair<FloatArray, FloatArray>) {
        require(contours.first.size == outerLipPoints.size && contours.second.size == innerLipPoints.size)
        contours.first.copyInto(outerLipPoints)
        contours.second.copyInto(innerLipPoints)
        // 静态图片检测完成后计算一次，不在每个绘制帧中重复插值。
        smoothOuterLipPoints = LipContourSmoother.smooth(contours.first)
        smoothInnerLipPoints = LipContourSmoother.smooth(contours.second)
    }

    fun clearFaceAnalysis() {
        faceCenterReady = false
    }

    private companion object {
        const val TAG = "ShaderDemo"
        const val DEFAULT_WHITEN_STRENGTH = 0f
        const val MIN_WHITEN_STRENGTH = 0f
        const val MAX_WHITEN_STRENGTH = 0.15f
        const val DEFAULT_BRIGHTEN_STRENGTH = 0f
        const val MIN_BRIGHTEN_STRENGTH = 0f
        const val MAX_BRIGHTEN_STRENGTH = 1f
        const val DEFAULT_BLACK_CIRCLE_STRENGTH = 0.12f
        const val MIN_BLACK_CIRCLE_STRENGTH = 0f
        const val MAX_BLACK_CIRCLE_STRENGTH = 0.15f
        const val DEFAULT_BIG_EYE_STRENGTH = 0.15f
        const val DEFAULT_SLIM_FACE_STRENGTH = 0.05f
        const val MIN_BIG_EYE_STRENGTH = 0f
        const val MIN_SLIM_FACE_STRENGTH = 0f
        const val MAX_BIG_EYE_STRENGTH = 0.15f
        const val MAX_SLIM_FACE_STRENGTH = 0.05f
        const val DEFAULT_LIPSTICK_STRENGTH = 0.8f
        const val MIN_LIPSTICK_STRENGTH = 0f
        const val MAX_LIPSTICK_STRENGTH = 1f

        const val DEFAULT_WARMTH_STRENGTH = 0f
        const val MIN_WARMTH_STRENGTH = 0f
        const val MAX_WARMTH_STRENGTH = 1f
        const val DEFAULT_SATURATION_STRENGTH = 0f
        const val MIN_SATURATION_STRENGTH = 0f
        const val MAX_SATURATION_STRENGTH = 0.3f
        const val DEFAULT_BLUR_STRENGTH = 0f
        const val MIN_BLUR_STRENGTH = 0f
        const val MAX_BLUR_STRENGTH = 1f

        // 瘦脸 - 脸部轮廓点-下巴点
        private const val SLIM_FACE_PAIR_COUNT = 9
    }
}
