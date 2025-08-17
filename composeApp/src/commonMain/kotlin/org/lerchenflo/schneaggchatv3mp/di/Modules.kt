package org.lerchenflo.schneaggchatv3mp.di

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.chat.Presentation.SharedViewModel
import org.lerchenflo.schneaggchatv3mp.chat.domain.GetAllUserUseCase
import org.lerchenflo.schneaggchatv3mp.chat.domain.UpsertUserUseCase
import org.lerchenflo.schneaggchatv3mp.database.CreateUserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabase
import org.lerchenflo.schneaggchatv3mp.database.UserDatabaseRepository

val sharedmodule = module{

    //Database
    single < UserDatabase> { CreateUserDatabase(get()).getDatabase() }

    //Repository
    singleOf(::UserDatabaseRepository)

    //Use cases
    singleOf(::GetAllUserUseCase)
    singleOf(::UpsertUserUseCase)

    //View model
    viewModelOf(::SharedViewModel)
}