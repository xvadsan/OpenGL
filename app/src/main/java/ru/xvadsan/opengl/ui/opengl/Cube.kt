package ru.xvadsan.opengl.ui.opengl

import android.opengl.GLES20
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Cube {

    private var programId: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0

    val modelCubeMatrix = FloatArray(16)
    val viewCubeMatrix = FloatArray(16)
    val projectionCubeMatrix = FloatArray(16)
    private val mvpCubeMatrix = FloatArray(16)

    private var mvpCubeMatrixHandle: Int = 0

    private var rotationX = 0f
    private var rotationY = 0f
    private var rotationZ = 0f

    fun updateRotation(dx: Float, dy: Float, dz: Float) {
        rotationX += dx
        rotationY += dy
        rotationZ += dz
    }

    private val vertices = floatArrayOf(
        // Задняя грань
        -0.2f, -0.2f, -0.2f, // нижний левый
        0.2f, -0.2f, -0.2f,  // нижний правый
        0.2f, 0.2f, -0.2f,   // верхний правый
        -0.2f, 0.2f, -0.2f,  // верхний левый

        // Передняя грань
        -0.2f, -0.2f, 0.2f,  // нижний левый
        0.2f, -0.2f, 0.2f,   // нижний правый
        0.2f, 0.2f, 0.2f,    // верхний правый
        -0.2f, 0.2f, 0.2f    // верхний левый
    )

    private val colors = floatArrayOf(
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.5f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.5f, 0.0f, 0.5f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f
    )

    private val indices = byteArrayOf(
        // Задняя грань
        0, 4, 5,
        0, 5, 1,
        // Правая грань
        1, 5, 6,
        1, 6, 2,
        // Передняя грань
        2, 6, 7,
        2, 7, 3,
        // Левая грань
        3, 7, 4,
        3, 4, 0,
        // Верхняя грань
        4, 7, 6,
        4, 6, 5,
        // Нижняя грань
        3, 0, 1,
        3, 1, 2
    )

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
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec4 vColor;
    varying vec4 fColor;

    void main() {
        fColor = vColor;
        gl_Position = uMVPMatrix * vPosition;
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
        val vertexShaderId = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShaderId = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)
        programId = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShaderId)
            GLES20.glAttachShader(it, fragmentShaderId)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(programId, "vColor")

        mvpCubeMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")

        Matrix.setIdentityM(modelCubeMatrix, 0)
        Matrix.setIdentityM(viewCubeMatrix, 0)
        Matrix.setIdentityM(projectionCubeMatrix, 0)
    }

    fun draw() {
        // Сброс матрицы модели
        Matrix.setIdentityM(modelCubeMatrix, 0)

        // Масштабирование
        Matrix.scaleM(modelCubeMatrix, 0, 0.5f, 0.5f, 0.5f)

        // Активация шейдерной программы
        GLES20.glUseProgram(programId)

        // Вращение
        Matrix.rotateM(modelCubeMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelCubeMatrix, 0, rotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelCubeMatrix, 0, rotationZ, 0f, 0f, 1f)

        // Обновление матрицы преобразования
        Matrix.multiplyMM(mvpCubeMatrix, 0, viewCubeMatrix, 0, modelCubeMatrix, 0)
        Matrix.multiplyMM(mvpCubeMatrix, 0, projectionCubeMatrix, 0, mvpCubeMatrix, 0)

        // Передача MVP матрицы в шейдер
        GLES20.glUniformMatrix4fv(mvpCubeMatrixHandle, 1, false, mvpCubeMatrix, 0)

        // Подготовка вершинных данных
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, mVertexBuffer)

        // Подготовка цветовых данных
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GL_FLOAT, false, 0, mColorBuffer)

        // Отрисовка куба
        GLES20.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_BYTE, mIndexBuffer)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
