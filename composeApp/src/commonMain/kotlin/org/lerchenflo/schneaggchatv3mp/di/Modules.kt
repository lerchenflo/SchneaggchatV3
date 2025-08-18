package org.lerchenflo.schneaggchatv3mp.di

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chatauswahl.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.chatauswahl.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chatauswahl.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.CreateUserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository
import org.lerchenflo.schneaggchatv3mp.network.Netzwerkutils
import kotlin.math.sin

val sharedmodule = module{

    //Database
    single < UserDatabase> { CreateUserDatabase(get()).getDatabase() }

    //Repository
    singleOf(::UserDatabaseRepository)


    //Netzwerktask
    singleOf(::Netzwerkutils)


    //Use cases userdatenbank
    singleOf(::GetAllUserUseCase)
    singleOf(::UpsertUserUseCase)

    //View model
    viewModelOf(::SharedViewModel)
}