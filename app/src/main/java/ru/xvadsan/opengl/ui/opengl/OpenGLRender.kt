package ru.xvadsan.opengl.ui.opengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var mCube: Cube? = null
    private var mOctagon: Octagon? = null
    private var mTexture: Texture? = null
    private var mScreenWidth = 0
    private var mScreenHeight = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        mCube = Cube()
        mTexture = Texture(context = context)
        mOctagon = Octagon()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        mOctagon?.updateRotation(0.05f)
        mOctagon?.draw(mScreenWidth, mScreenHeight)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        mTexture?.updateRotation(-0.05f)
        mTexture?.draw(mScreenWidth, mScreenHeight)

        mCube?.updateRotation(-0.4f, -0.4f, -0.4f)
        mCube?.draw()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mScreenWidth = width
        mScreenHeight = height
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()

        Matrix.setLookAtM(mCube?.viewCubeMatrix, 0, 0f, 0f, -4f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.frustumM(mCube?.projectionCubeMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        Matrix.setIdentityM(mTexture?.textureViewMatrix, 0)
        Matrix.setIdentityM(mOctagon?.octagonViewMatrix, 0)
        Matrix.setIdentityM(mCube?.modelCubeMatrix, 0)
    }
}
