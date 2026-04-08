import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import theme.AppTheme
import moe.tlaster.precompose.PreComposeApp
import viewmodel.MainViewModel


@Composable
fun App(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    AppTheme(darkTheme = uiState.darkMode) {

    }
}
