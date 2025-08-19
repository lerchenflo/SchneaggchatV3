package org.lerchenflo.schneaggchatv3mp.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.CreateUserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient
import kotlin.math.sin

val sharedmodule = module{

    //Database
    single < UserDatabase> { CreateUserDatabase(get()).getDatabase() }

    single < HttpClient> { createHttpClient(get()) }


    //Repository
    singleOf(::UserDatabaseRepository)


    //Netzwerktask
    singleOf(::NetworkUtils)


    //Use cases userdatenbank
    singleOf(::GetAllUserUseCase)
    singleOf(::UpsertUserUseCase)

    //View model
    viewModelOf(::SharedViewModel)
}