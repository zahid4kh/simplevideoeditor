import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import theme.AppTheme
import ui.EditorScreen
import viewmodel.MainViewModel
import viewmodel.VideoEditorViewModel


@Composable
fun App(
    mainViewModel: MainViewModel,
    editorViewModel: VideoEditorViewModel
) {
    val uiState by mainViewModel.uiState.collectAsState()
    AppTheme(darkTheme = uiState.darkMode) {
        EditorScreen(
            mainViewModel = mainViewModel,
            editorViewModel = editorViewModel
        )
    }
}
