package org.lerchenflo.schneaggchatv3mp.di

import LoginViewModel
import SignUpViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.presentation.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    single <HttpClient> { createHttpClient(get()) }


    //Repository
    singleOf(::AppRepository)


    //Netzwerktask
    singleOf(::NetworkUtils)

    //Preferences
    singleOf(::Preferencemanager)


    //View model
    singleOf(::SharedViewModel)

    //singleOf(::SettingsViewModel)

    //Alle viewmodels mit factory für desktop
    viewModelOf(::SettingsViewModel)
    factory { SettingsViewModel(get(), get()) } // factory -> new instance each injection

    viewModelOf(::ChatSelectorViewModel)
    factory { ChatSelectorViewModel(get()) }

    viewModelOf(::ChatViewModel)
    factory { ChatViewModel(get()) }

    viewModelOf(::LoginViewModel)
    factory { LoginViewModel(get()) }

    viewModelOf(::SignUpViewModel)
    factory { SignUpViewModel(get()) }
}