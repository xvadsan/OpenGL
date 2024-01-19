package ru.xvadsan.opengl.ui.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class Octagon {

    private var programId: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0

    private val octagonModelMatrix = FloatArray(16)
    val octagonViewMatrix = FloatArray(16)
    private val octagonProjectionMatrix = FloatArray(16)

    private var modelMatrixHandle: Int = 0
    private var viewMatrixHandle: Int = 0
    private var projectionMatrixHandle: Int = 0

    private var rotationZ = 0f
    private var time = 0f
    private val colorChangeSpeed = 0.01f
    private var timeUniformHandle: Int = 0
    private var timeColor = 0f

    private val vertices = floatArrayOf(
        // Вершины внешнего восьмиугольника (остаются без изменений)
        1f * 1.1f, 0f, 0f, // 0
        0.7f * 1.1f, 0.7f * 1.1f, 0f, // 1
        0f, 1f * 1.1f, 0f, // 2
        -0.7f * 1.1f, 0.7f * 1.1f, 0f, // 3
        -1f * 1.1f, 0f, 0f, // 4
        -0.7f * 1.1f, -0.7f * 1.1f, 0f, // 5
        0f, -1f * 1.1f, 0f, // 6
        0.7f * 1.1f, -0.7f * 1.1f, 0f, // 7
        // Вершины внутреннего восьмиугольника
        0.5f * 0.2f, 0f, 0f, // 8
        0.35f * 0.2f, 0.35f * 0.2f, 0f, // 9
        0f, 0.5f * 0.2f, 0f, // 10
        -0.35f * 0.2f, 0.35f * 0.2f, 0f, // 11
        -0.5f * 0.2f, 0f, 0f, // 12
        -0.35f * 0.2f, -0.35f * 0.2f, 0f, // 13
        0f, -0.5f * 0.2f, 0f, // 14
        0.35f * 0.2f, -0.35f * 0.2f, 0f // 15
    )

    private val colors = floatArrayOf(
        // Цвета для внешнего восьмиугольника (непрозрачный синий)
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.5f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.5f, 0.0f, 0.5f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,

        // Цвета для внутреннего восьмиугольника (прозрачный красный)
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.5f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.5f, 0.0f, 0.5f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
    )

    private val indices = byteArrayOf(
        // Индексы для внешнего восьмиугольника
        0, 1, 9,
        0, 9, 8,
        1, 2, 10,
        1, 10, 9,
        2, 3, 11,
        2, 11, 10,
        3, 4, 12,
        3, 12, 11,
        4, 5, 13,
        4, 13, 12,
        5, 6, 14,
        5, 14, 13,
        6, 7, 15,
        6, 15, 14,
        7, 0, 8,
        7, 8, 15,
        // Индексы для внутреннего восьмиугольника
        8, 9, 10,
        8, 10, 11,
        8, 11, 12,
        8, 12, 13,
        8, 13, 14,
        8, 14, 15,
        8, 15, 9
    )

    fun updateRotation(dz: Float) {
        rotationZ += dz
    }

    private val mVertexBuffer: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4).apply {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }

    private val mColorBuffer: ByteBuffer = ByteBuffer.allocateDirect(colors.size * 4).apply {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(colors)
            position(0)
        }
    }

    private val mIndexBuffer: ByteBuffer = ByteBuffer.allocateDirect(indices.size).apply {
        put(indices)
        position(0)
    }

    private val vertexShaderCode = """
        uniform mat4 uModelMatrix;
        uniform mat4 uViewMatrix;
        uniform mat4 uProjectionMatrix;
        uniform float uTime;
        
        attribute vec4 vPosition;
        attribute vec4 vColor;
        varying vec4 fColor;
        
        void main() {
            fColor = vColor;
            gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * vPosition;
        }
    """

    private val fragmentShaderCode = """ 
        precision mediump float;
        varying vec4 fColor;
    
        void main() {
            gl_FragColor = fColor;
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
        timeUniformHandle = GLES20.glGetUniformLocation(programId, "uTime")

        Matrix.setIdentityM(octagonModelMatrix, 0)
        Matrix.setIdentityM(octagonViewMatrix, 0)
        Matrix.setIdentityM(octagonProjectionMatrix, 0)
    }

    fun draw(screenWidth: Int, screenHeight: Int) {
        colorChanger()
        positionChanger(screenWidth, screenHeight)

        GLES20.glUseProgram(programId)

        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, octagonModelMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, octagonViewMatrix, 0)
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, octagonProjectionMatrix, 0)
        time += 0.015f
        GLES20.glUniform1f(timeUniformHandle, timeColor)

        // Подготовка вершинных данных
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer)

        // Подготовка цветовых данных
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, mColorBuffer)

        // Отрисовка восьмиугольника
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_BYTE, mIndexBuffer)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun colorChanger() {
        time += colorChangeSpeed
        val dynamicRed = (sin(time.toDouble()) * 0.5 + 0.5).toFloat()                    // Красный компонент
        val dynamicGreen = (sin(time.toDouble() + Math.PI / 2) * 0.5 + 0.5).toFloat() // Зеленый компонент
        val dynamicBlue = (sin(time.toDouble() + Math.PI) * 0.5 + 0.5).toFloat()      // Синий компонент
        for (i in 1 until colors.size / 2 step 4) {
            colors[i - 1] = dynamicRed
            colors[i] = dynamicGreen
            colors[i + 1] = dynamicBlue
        }
        mColorBuffer.clear()
        mColorBuffer.asFloatBuffer().put(colors).position(0)
    }

    private fun positionChanger(screenWidth: Int, screenHeight: Int) {
        Matrix.setIdentityM(octagonModelMatrix, 0)
        Matrix.translateM(octagonModelMatrix, 0, 0f, 0f, 1f) // Сдвигаем назад
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        Matrix.scaleM(octagonModelMatrix, 0, 1f, aspectRatio, 1f)
        Matrix.rotateM(octagonModelMatrix, 0, rotationZ, 0f, 0f, 1f)
    }
}

