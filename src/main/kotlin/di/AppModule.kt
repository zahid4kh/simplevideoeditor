package di

import data.Database
import org.koin.dsl.module
import viewmodel.MainViewModel
import viewmodel.VideoEditorViewModel

val appModule = module {
    single { Database() }
    single { MainViewModel(get()) }
    single { VideoEditorViewModel() }
}