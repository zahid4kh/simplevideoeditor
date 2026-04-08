@file:JvmName("SimpleVideoEditor")
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.appModule
import java.awt.Dimension
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import viewmodel.MainViewModel


fun main() = application {
    startKoin {
        modules(appModule)
    }

    val viewModel = getKoin().get<MainViewModel>()
    val windowState = rememberWindowState(size = DpSize(800.dp, 600.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        alwaysOnTop = true,
        title = "SimpleVideoEditor - Made with Compose for Desktop Wizard",
        icon = null
    ) {
        window.minimumSize = Dimension(800, 600)

        App(
            viewModel = viewModel
        )
    }
}