package org.lerchenflo.schneaggchatv3mp.di

import LoginViewModel
import SignUpViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.NotificationManager
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    single <HttpClient> { createHttpClient(get()) }


    //Repository
    singleOf(::AppRepository)
    singleOf(::SettingsRepository)
    singleOf(::GroupRepository)
    singleOf(::UserRepository)
    singleOf(::MessageRepository)
    singleOf(::TodoRepository)


    //Netzwerktask
    singleOf(::NetworkUtils)

    //Preferences
    singleOf(::Preferencemanager)

    //View model
    singleOf(::GlobalViewModel)


    //Alle viewmodels mit factory für desktop
    viewModelOf(::SettingsViewModel)
    factory { SettingsViewModel(get(), get()) } // factory -> new instance each injection

    viewModelOf(::ChatSelectorViewModel)
    factory { ChatSelectorViewModel(get()) }

    viewModelOf(::ChatViewModel)
    factory { ChatViewModel(get(), get(), get()) }

    viewModelOf(::LoginViewModel)
    factory { LoginViewModel(get()) }

    viewModelOf(::SignUpViewModel)
    factory { SignUpViewModel(get()) }

    viewModelOf(::TodolistViewModel)
    factory { TodolistViewModel(get(), get()) }
}