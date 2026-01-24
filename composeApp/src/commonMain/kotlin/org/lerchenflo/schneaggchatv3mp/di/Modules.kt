package org.lerchenflo.schneaggchatv3mp.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetailsViewmodel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreatorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChatViewModel
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounterViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerstackViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.UndercoverViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    singleOf(::TokenManager)

    single <HttpClient>(named("api")) { createHttpClient(get(), get(), true) }

    single <HttpClient>(named("auth")) { createHttpClient(get(), get(), false) }


    singleOf(::Navigator)


    //Repository
    single {
        AppRepository(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    singleOf(::SettingsRepository)
    singleOf(::GroupRepository)
    singleOf(::UserRepository)
    singleOf(::MessageRepository)
    singleOf(::TodoRepository)
    singleOf(::LoggingRepository)


    single<NetworkUtils> {
        NetworkUtils(get(named("api")), get(named("auth")), get(), get())
    }

    // Socket Connection Manager
    single<SocketConnectionManager> {
        SocketConnectionManager(get(named("api")), get(), get(), get(), get())
    }


    //Preferences
    singleOf(::Preferencemanager)
    
    //Language
    singleOf(::LanguageService)

    //View model
    singleOf(::GlobalViewModel)


    //Alle viewmodels mit factory für desktop

    viewModelOf(::ChatSelectorViewModel)
    factory { ChatSelectorViewModel(get(), get(), get(), get(), get()) }

    viewModelOf(::ChatViewModel)
    factory { ChatViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }

    viewModelOf(::ChatDetailsViewmodel)
    factory { ChatDetailsViewmodel(get(), get(), get(), get(), get()) }

    viewModelOf(::NewChatViewModel)
    factory { NewChatViewModel(get(), get(), get(),get()) }

    viewModelOf(::GroupCreatorViewModel)
    factory { GroupCreatorViewModel(get(), get()) }

    viewModelOf(::LoginViewModel)
    factory { LoginViewModel(get(), get(), get()) }

    viewModelOf(::SignUpViewModel)
    factory { SignUpViewModel(get(), get(), get()) }

    viewModelOf(::TodolistViewModel)
    factory { TodolistViewModel(get(), get(), get()) }

    viewModelOf(::SchneaggmapViewmodel)
    factory { SchneaggmapViewmodel(get()) }



    //Settings
    viewModelOf(::SharedSettingsViewmodel)
    factory { SharedSettingsViewmodel(get(), get(), get()) }


    viewModelOf(::SettingsViewModel)
    factory { SettingsViewModel() } // factory -> new instance each injection


    viewModelOf(::DevSettingsViewModel)
    factory { DevSettingsViewModel(get(), get()) }
    viewModelOf(::DartCounterViewModel)
    factory { DartCounterViewModel() }
    viewModelOf(::TowerstackViewModel)
    factory { TowerstackViewModel() }
    viewModelOf(::UndercoverViewModel)
    factory { UndercoverViewModel(get()) }


    viewModelOf(::UserSettingsViewModel)
    factory { UserSettingsViewModel(get(), get(), get()) }

    viewModelOf(::AppearanceSettingsViewModel)
    factory { AppearanceSettingsViewModel(get(), get(), get()) }

    viewModelOf(::MiscSettingsViewModel)
    factory { MiscSettingsViewModel(get(), get(), get()) }
}