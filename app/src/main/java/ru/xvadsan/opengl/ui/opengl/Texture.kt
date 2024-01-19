package ru.xvadsan.opengl.ui.opengl

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import ru.xvadsan.opengl.R
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Texture(private val context: Context) {

    private var programId: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0

    private val textureModelMatrix = FloatArray(16)
    val textureViewMatrix = FloatArray(16)
    private val textureProjectionMatrix = FloatArray(16)

    private var modelMatrixHandle: Int = 0
    private var viewMatrixHandle: Int = 0
    private var projectionMatrixHandle: Int = 0
    private var textureHandle: Int = 0
    private var textureH: Int = 0
    private var textureCoordHandle: Int = 0

    private var rotationZ = 0f

    private val vertices = floatArrayOf(
        0f, 0f, 0f,
        1f * 0.7f, 0f * 0.7f, 0f,
        0.7f * 0.7f, 0.7f * 0.7f, 0f,
        0f * 0.7f, 1f * 0.7f, 0f,
        -0.7f * 0.7f, 0.7f * 0.7f, 0f,
        -1f * 0.7f, 0f * 0.7f, 0f,
        -0.7f * 0.7f, -0.7f * 0.7f, 0f,
        0f * 0.7f, -1f * 0.7f, 0f,
        0.7f * 0.7f, -0.7f * 0.7f, 0f
    )

    private val indices = byteArrayOf(
        // Центр, затем вершины вокруг
        0, 1, 2,
        0, 2, 3,
        0, 3, 4,
        0, 4, 5,
        0, 5, 6,
        0, 6, 7,
        0, 7, 8,
        0, 8, 1
    )

    // Координаты текстуры
    private val textureCoords = floatArrayOf(
        // Центр восьмиугольника
        0.5f, 0.5f,
        // Вершины восьмиугольника
        1f, 0.5f,     // Вершина справа
        0.85f, 0.85f, // Вершина вправо-вверх
        0.5f, 1f,     // Вершина сверху
        0.15f, 0.85f, // Вершина влево-вверх
        0f, 0.5f,     // Вершина слева
        0.15f, 0.15f, // Вершина влево-вниз
        0.5f, 0f,     // Вершина снизу
        0.85f, 0.15f  // Вершина вправо-вниз
    )

    private val mVertexBuffer: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4).apply {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }

    private val mIndexBuffer: ByteBuffer = ByteBuffer.allocateDirect(indices.size).apply {
        put(indices)
        position(0)
    }

    private val mTextureBuffer: ByteBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4).apply {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(textureCoords)
            position(0)
        }
    }

    fun updateRotation(dz: Float) {
        rotationZ += dz
    }

    private val vertexShaderCode = """
        uniform mat4 uModelMatrix;
        uniform mat4 uViewMatrix;
        uniform mat4 uProjectionMatrix;
        
        attribute vec4 vPosition;
        attribute vec4 vColor;
        varying vec4 fColor;
        
        attribute vec2 aTexCoordinate;
        varying vec2 vTexCoordinate;
        
        void main() {
            fColor = vColor;
            gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * vPosition;
            vTexCoordinate = aTexCoordinate;
        }
    """

    private val fragmentShaderCode = """ 
        precision mediump float;
        varying vec4 fColor;
        varying vec2 vTexCoordinate;
        uniform sampler2D uTexture;
    
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoordinate);
        }
    """

    init {
        val vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        programId = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShaderId)
            GLES20.glAttachShader(it, fragmentShaderId)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(programId, "vColor")

        modelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        viewMatrixHandle = GLES20.glGetUniformLocation(programId, "uViewMatrix")
        projectionMatrixHandle = GLES20.glGetUniformLocation(programId, "uProjectionMatrix")
        textureH = GLES20.glGetUniformLocation(programId, "uTexture")

        Matrix.setIdentityM(textureModelMatrix, 0)
        Matrix.setIdentityM(textureViewMatrix, 0)
        Matrix.setIdentityM(textureProjectionMatrix, 0)
        textureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoordinate")
        textureHandle = loadTexture(context = context, R.drawable.asd)
    }

    private fun loadTexture(context: Context, resourceId: Int): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        val options = BitmapFactory.Options()
        options.inScaled = false   // Без масштабирования
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        // Привязка текстуры
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        // Настройка параметров текстуры
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return textureIds[0]
    }

    fun draw(screenWidth: Int, screenHeight: Int) {
        positionChanger(screenWidth, screenHeight)

        GLES20.glUseProgram(programId)

        // Передача MVP матрицы в шейдер
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, textureModelMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, textureViewMatrix, 0)
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, textureProjectionMatrix, 0)
        errorCheck.invoke("GLES20.glUniformMatrix4fv")

        // Подготовка вершинных данных
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer)

        // Подготовка координат текстуры
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer)

        // Отрисовка восьмиугольника
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_FAN, indices.size, GLES20.GL_UNSIGNED_BYTE, mIndexBuffer)

        // Отключение атрибутов после использования
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e("OpenGL", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader")
        }
        return shader
    }

    private var errorCheck = { location: String ->
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("OpenGL-ERROR", "Error at $location: $error")
        }
    }

    private fun positionChanger(screenWidth: Int, screenHeight: Int) {
        // Сброс матрицы модели
        Matrix.setIdentityM(textureModelMatrix, 0)
        Matrix.translateM(textureModelMatrix, 0, 0f, 0f, 1f) // Сдвигаем назад
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        Matrix.scaleM(textureModelMatrix, 0, 1f, aspectRatio, 1f)
        Matrix.rotateM(textureModelMatrix, 0, rotationZ, 0f, 0f, 1f)
    }
}
