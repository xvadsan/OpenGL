package ru.xvadsan.opengl

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import ru.xvadsan.opengl.ui.opengl.OpenGLRenderer
import ru.xvadsan.opengl.ui.theme.OpenGLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenGLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenGL()
                }
            }
        }
    }
}

@Composable
fun OpenGL() {
    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setZOrderOnTop(true)
                setEGLContextClientVersion(2)
                setRenderer(OpenGLRenderer(context))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OpenGLTheme {
        OpenGL()
    }
}