package org.lerchenflo.schneaggchatv3mp.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
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
import org.lerchenflo.schneaggchatv3mp.datasource.network.TokenManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.createHttpClient
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerRepository
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.coinflip.CoinFlipViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.fingerpicker.FingerPickerViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounterViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush.GridRushViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout.OddOneOutViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.SchneaggaHusViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerstackViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.UndercoverViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck.EmailVerifiedCheckViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpViewModel
import org.lerchenflo.schneaggchatv3mp.roadmap.presentation.RoadmapViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapViewModel
import org.lerchenflo.schneaggchatv3mp.settings.data.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings.SchneaggmapSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

enum class HTTPCLIENTTYPE {
    AUTHENTICATED,
    NOT_AUTHENTICATED,
    SOCKET
}




val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    singleOf(::TokenManager)

    //Network utils must be created before HttpClients to avoid circular dependency
    single<NetworkUtils> {
        NetworkUtils(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)), get(), get())
    }

    single <HttpClient>(named(HTTPCLIENTTYPE.AUTHENTICATED)) { createHttpClient(get(), get(), true) }

    single <HttpClient>(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)) { createHttpClient(get(), get(), false) }


    singleOf(::Navigator)


    //Repository
    single {
        AppRepository(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    singleOf(::SettingsRepository)
    singleOf(::GroupRepository)
    singleOf(::UserRepository)
    singleOf(::MessageRepository)
    singleOf(::LoggingRepository)
    singleOf(::MapRepository)
    singleOf(::GameHighscoreRepository)
    singleOf(::PlayerRepository)


    // Socket Connection Manager
    single<SocketConnectionManager> {
        SocketConnectionManager(
            httpClient = get(named(HTTPCLIENTTYPE.SOCKET)),
            tokenManager = get()
        )
    }


    //Preferences
    single {
        Preferencemanager(get(), get(), get())
    }
    
    //Language
    singleOf(::LanguageService)

    //App-wide coroutine scope for work that must survive screen/ViewModel lifecycles
    singleOf(::ApplicationScope)

    //View model
    singleOf(::GlobalViewModel)


    //Alle viewmodels mit factory für desktop

    viewModelOf(::ChatSelectorViewModel)

    viewModelOf(::ChatViewModel)

    viewModelOf(::ChatDetailsViewmodel)

    viewModelOf(::NewChatViewModel)

    viewModelOf(::GroupCreatorViewModel)

    viewModelOf(::EmailVerifiedCheckViewModel)

    viewModelOf(::LoginViewModel)

    viewModelOf(::SignUpViewModel)

    // Explicit lambda because the nullable initialEntryId can't be resolved by viewModelOf
    viewModel { (initialEntryId: String?) ->
        SchneaggmapViewModel(
            navigator = get(),
            mapRepository = get(),
            appRepository = get(),
            preferenceManager = get(),
            locationService = get(),
            userRepository = get(),
            initialEntryId = initialEntryId
        )
    }



    //Settings
    viewModelOf(::SharedSettingsViewmodel)


    viewModelOf(::SettingsViewModel)


    viewModelOf(::DevSettingsViewModel)
    viewModelOf(::DartCounterViewModel)
    viewModelOf(::TowerstackViewModel)
    viewModelOf(::UndercoverViewModel)
    viewModelOf(::YatziViewModel)
    viewModelOf(::TetrisViewModel)
    viewModelOf(::MorseViewModel)
    viewModelOf(::CoinFlipViewModel)
    viewModelOf(::FingerPickerViewModel)

    viewModelOf(::SchneaggaHusViewmodel)

    viewModelOf(::GridRushViewmodel)

    viewModelOf(::OddOneOutViewmodel)

    viewModelOf(::PlayerSelectorViewModel)
    viewModelOf(::RecapViewModel)
    viewModelOf(::GameSelectorViewModel)


    viewModelOf(::UserSettingsViewModel)

    viewModelOf(::AppearanceSettingsViewModel)

    viewModelOf(::MiscSettingsViewModel)

    viewModelOf(::SchneaggmapSettingsViewModel)

    viewModelOf(::RoadmapViewModel)
}