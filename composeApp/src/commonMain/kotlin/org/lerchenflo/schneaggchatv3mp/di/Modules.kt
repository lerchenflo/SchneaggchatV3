package org.lerchenflo.schneaggchatv3mp.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.database.AppRepository
import org.lerchenflo.schneaggchatv3mp.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.utilities.Preferencemanager

val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    single < HttpClient> { createHttpClient(get()) }


    //Repository
    singleOf(::AppRepository)


    //Netzwerktask
    singleOf(::NetworkUtils)

    //Preferences
    singleOf(::Preferencemanager)


    //View model
    singleOf(::SharedViewModel)
}