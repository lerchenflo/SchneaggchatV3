package org.lerchenflo.schneaggchatv3mp.di

import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.MessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetailsViewmodel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreatorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChatViewModel
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.todolist.data.TodoRepository
import org.lerchenflo.schneaggchatv3mp.todolist.presentation.TodolistViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    single <HttpClient>(named("api")) { createHttpClient(get(), get(), true) }

    single <HttpClient>(named("auth")) { createHttpClient(get(), get(), false) }


    singleOf(::Navigator)



    //Repository
    singleOf(::AppRepository)
    singleOf(::SettingsRepository)
    singleOf(::GroupRepository)
    singleOf(::UserRepository)
    singleOf(::MessageRepository)
    singleOf(::TodoRepository)


    single<NetworkUtils> {
        NetworkUtils(get(named("api")), get(named("auth")), get())
    }

    //Preferences
    singleOf(::Preferencemanager)

    //View model
    singleOf(::GlobalViewModel)


    //Alle viewmodels mit factory für desktop

    viewModelOf(::ChatSelectorViewModel)
    factory { ChatSelectorViewModel(get(), get(), get()) }

    viewModelOf(::ChatViewModel)
    factory { ChatViewModel(get(), get(), get(), get()) }

    viewModelOf(::ChatDetailsViewmodel)
    factory { ChatDetailsViewmodel(get(), get()) }

    viewModelOf(::NewChatViewModel)
    factory { NewChatViewModel(get(), get()) }

    viewModelOf(::GroupCreatorViewModel)
    factory { GroupCreatorViewModel(get(), get()) }

    viewModelOf(::LoginViewModel)
    factory { LoginViewModel(get(), get(), get()) }

    viewModelOf(::SignUpViewModel)
    factory { SignUpViewModel(get(), get()) }

    viewModelOf(::TodolistViewModel)
    factory { TodolistViewModel(get(), get(), get()) }



    //Settings
    viewModelOf(::SharedSettingsViewmodel)
    factory { SharedSettingsViewmodel(get(), get()) }


    viewModelOf(::SettingsViewModel)
    factory { SettingsViewModel(get(), get()) } // factory -> new instance each injection

    viewModelOf(::DevSettingsViewModel)
    factory { DevSettingsViewModel() }

    viewModelOf(::UserSettingsViewModel)
    factory { UserSettingsViewModel(get(), get(), get()) }

    viewModelOf(::AppearanceSettingsViewModel)
    factory { AppearanceSettingsViewModel(get()) }
}