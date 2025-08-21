package org.lerchenflo.schneaggchatv3mp.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.DeleteUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllMessagesForUserIdUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllMessagesWithReadersUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetChangeIdUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertMessageUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.AppDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager
import kotlin.math.sin

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    single < HttpClient> { createHttpClient(get()) }


    //Repository
    singleOf(::AppDatabaseRepository)


    //Netzwerktask
    singleOf(::NetworkUtils)

    //Preferences
    singleOf(::Preferencemanager)


    //Use cases userdatenbank
    singleOf(::GetAllUserUseCase)
    singleOf(::UpsertUserUseCase)
    singleOf(::DeleteUserUseCase)
    singleOf(::GetChangeIdUserUseCase)

    singleOf(::GetChangeIdMessageUseCase)
    singleOf(::UpsertMessageUseCase)
    singleOf(::GetAllMessagesWithReadersUseCase)
    singleOf(::GetAllMessagesForUserIdUseCase)

    //View model
    singleOf(::SharedViewModel)
}