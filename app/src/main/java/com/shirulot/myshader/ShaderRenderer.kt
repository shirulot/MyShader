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
        queueEvent { shaderRenderer.setSourceImage(sourceImageRes) }
    }

}

private class ShaderRenderer(
    private val context: Context,
    private val fragmentShaderAsset: String,
    private val onStatusChanged: (String) -> Unit,
) : GLSurfaceView.Renderer {

    // 尚未拿到人脸检测结果时，不显示局部脸部区域。
    private var faceCenterReady = false

    //人脸中心点
    private var faceCenterX = 0.5f
    private var faceCenterY = 0.5f
    private var faceWidth = 0.3f
    private var faceHeight = 0.4f
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
        val blurStrengthLocation = GLES20.glGetUniformLocation(program, "blurStrength")
        val warmthStrengthLocation = GLES20.glGetUniformLocation(program, "warmthStrength")
        val saturationStrengthLocation = GLES20.glGetUniformLocation(program, "saturationStrength")
        val faceCenterLocation = GLES20.glGetUniformLocation(program, "faceCenter")
        val faceCenterReadyLocation = GLES20.glGetUniformLocation(program, "faceCenterReady")
        val faceSizeLocation = GLES20.glGetUniformLocation(program, "faceSize")

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

        if (faceCenterReadyLocation >= 0) {
            GLES20.glUniform1f(faceCenterReadyLocation, if (faceCenterReady) 1f else 0f)
        }
        if (faceSizeLocation >= 0) {
            GLES20.glUniform2f(faceSizeLocation, faceWidth, faceHeight)
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

    private companion object {
        const val TAG = "ShaderDemo"
        const val DEFAULT_WHITEN_STRENGTH = 0f
        const val MIN_WHITEN_STRENGTH = 0f
        const val MAX_WHITEN_STRENGTH = 1f

        const val DEFAULT_WARMTH_STRENGTH = 0f
        const val MIN_WARMTH_STRENGTH = 0f
        const val MAX_WARMTH_STRENGTH = 1f
        const val DEFAULT_SATURATION_STRENGTH = 0f
        const val MIN_SATURATION_STRENGTH = 0f
        const val MAX_SATURATION_STRENGTH = 0.3f
        const val DEFAULT_BLUR_STRENGTH = 0f
        const val MIN_BLUR_STRENGTH = 0f
        const val MAX_BLUR_STRENGTH = 1f
    }
}
