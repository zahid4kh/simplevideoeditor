@file:JvmName("SimpleVideoEditor")
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.appModule
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import simplevideoeditor.resources.Res
import simplevideoeditor.resources.appicon
import viewmodel.MainViewModel
import viewmodel.VideoEditorViewModel


fun main() = application {
    startKoin {
        modules(appModule)
    }

    val mainViewModel = getKoin().get<MainViewModel>()
    val editorViewModel = getKoin().get<VideoEditorViewModel>()
    val windowState = rememberWindowState(size = DpSize(1280.dp, 780.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Simple Video Editor",
        icon = painterResource(Res.drawable.appicon)
    ) {
        window.minimumSize = Dimension(900, 600)

        App(
            mainViewModel = mainViewModel,
            editorViewModel = editorViewModel
        )
    }
}