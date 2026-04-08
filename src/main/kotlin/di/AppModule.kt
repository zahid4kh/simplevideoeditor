package di

import data.Database
import org.koin.dsl.module
import viewmodel.MainViewModel

val appModule = module {
    single { Database() }
    single { MainViewModel(get()) }
}