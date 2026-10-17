package org.lerchenflo.schneaggchatv3mp.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.data.DefaultActionChannel
import org.lerchenflo.schneaggchatv3mp.app.data.DefaultDataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.data.DefaultErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.data.LocalSessionTerminator
import org.lerchenflo.schneaggchatv3mp.app.data.RemoteAppInfoRepository
import org.lerchenflo.schneaggchatv3mp.app.logging.RoomLoggingRepository
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.chat.data.local.GroupLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.MessageLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstChatListRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstGroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstMessageRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.OfflineFirstUserRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.local.RoomGroupDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.RoomMessageDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.RoomUserDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.local.UserLocalDataSource
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.ChatViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.ChatDetailsViewmodel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.birthdays.BirthdaysViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentTab
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent.SharedContentViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.GroupCreatorViewModel
import org.lerchenflo.schneaggchatv3mp.chat.presentation.newchat.NewChatViewModel
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import org.lerchenflo.schneaggchatv3mp.datasource.database.CreateAppDatabase
import org.lerchenflo.schneaggchatv3mp.app.domain.ActionChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.AppInfoRepository
import org.lerchenflo.schneaggchatv3mp.login.domain.AuthRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.Logger
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageRepository
import org.lerchenflo.schneaggchatv3mp.app.domain.SessionTerminator
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AppAuthEventSink
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthClock
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthEventSink
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthLog
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthRefreshApi
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionManager
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.AuthSessionStore
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.KtorAuthRefreshApi
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.LoggingAuthLog
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.PreferenceAuthSessionStore
import org.lerchenflo.schneaggchatv3mp.datasource.network.auth.SystemAuthClock
import org.lerchenflo.schneaggchatv3mp.datasource.network.socket.SocketConnectionManager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.games.data.CrosswordRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.data.PlayerRepository
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoresViewModel
import org.lerchenflo.schneaggchatv3mp.games.data.WordleRepository
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSelector.PlayerSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSelectorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.coinflip.CoinFlipViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.crossword.CrosswordViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.wordle.WordleViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.fingerpicker.FingerPickerViewModel
import org.lerchenflo.schneaggchatv3mp.tools.presentation.fuelcalculator.FuelCalculatorViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.game2048.Game2048ViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter.DartCounterViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.morse.MorseViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush.GridRushViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge.CChallengeViewmodel
import org.lerchenflo.schneaggchatv3mp.games.data.CChallengeStreakRepository
import org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout.OddOneOutViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus.SchneaggaHusViewmodel
import org.lerchenflo.schneaggchatv3mp.games.presentation.recap.RecapViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.tetris.TetrisViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack.TowerstackViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.undercover.UndercoverViewModel
import org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi.YatziViewModel
import org.lerchenflo.schneaggchatv3mp.login.data.RemoteAuthRepository
import org.lerchenflo.schneaggchatv3mp.login.presentation.autologincredchecker.AutoLoginCredCheckerViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.emailverifiedcheck.EmailVerifiedCheckViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.login.LoginViewModel
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignUpViewModel
import org.lerchenflo.schneaggchatv3mp.events.data.local.EventLocalDataSource
import org.lerchenflo.schneaggchatv3mp.events.data.OfflineFirstEventRepository
import org.lerchenflo.schneaggchatv3mp.events.data.local.RoomEventDataSource
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.presentation.EventsViewModel
import org.lerchenflo.schneaggchatv3mp.feedback.presentation.FeedbackViewModel
import org.lerchenflo.schneaggchatv3mp.feedback.data.RemoteFeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.MapLocalDataSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.OfflineFirstMapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.local.RoomMapDataSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.friendcompass.FriendCompassViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.settings.data.DataStoreSettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.SharedSettingsViewmodel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.appearancesettings.AppearanceSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.DevSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.devsettings.announcements.AnnouncementAdminViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.miscSettings.MiscSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.notificationsettings.NotificationSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.privacyandsecurity.PrivacyAndSecurityViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.loggedindevices.LoggedInDevicesViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.schneaggmapsettings.SchneaggmapSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.settings.presentation.usersettings.UserSettingsViewModel
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService
import org.lerchenflo.schneaggchatv3mp.app.data.remote.AppInfoRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.app.data.remote.KtorAppInfoRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.GroupRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.KtorGroupRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.KtorMessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.KtorUserRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.MessageRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.chat.data.remote.UserRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.events.data.remote.EventRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.events.data.remote.KtorEventRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.FeedbackRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.feedback.data.remote.KtorFeedbackRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.announcement.data.RemoteAnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.AnnouncementRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.announcement.data.remote.KtorAnnouncementRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementRepository
import org.lerchenflo.schneaggchatv3mp.games.data.remote.GameRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.games.data.remote.KtorGameRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.login.data.remote.AuthRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.login.data.remote.KtorAuthRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.KtorMapRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.remote.MapRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.settings.data.remote.KtorSettingsRemoteDataSource
import org.lerchenflo.schneaggchatv3mp.settings.data.remote.SettingsRemoteDataSource

enum class HTTPCLIENTTYPE {
    AUTHENTICATED,
    NOT_AUTHENTICATED,
    SOCKET
}




val sharedmodule = module{

    //Database
    single <AppDatabase> { CreateAppDatabase(get()).getDatabase() }

    // App-wide error / action events (shown and handled by App.kt)
    singleOf(::DefaultErrorChannel) bind ErrorChannel::class
    singleOf(::DefaultActionChannel) bind ActionChannel::class

    // Session layer (see plans/AUTH_SESSION_REBUILD_PLAN.md). Construction order matters and is
    // cycle-free: the refresh API only needs the NOT_AUTHENTICATED client, the AUTHENTICATED
    // client needs the manager, the remote data sources below need the clients.
    single<AuthSessionStore> { PreferenceAuthSessionStore(get()) }
    single<AuthRefreshApi> { KtorAuthRefreshApi(get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)), get(), get(), get()) }
    single<AuthEventSink> { AppAuthEventSink(get(), get()) }
    single<AuthClock> { SystemAuthClock }
    single<AuthLog> { LoggingAuthLog(get(), get<ApplicationScope>()) }
    single {
        AuthSessionManager(
            store = get(),
            api = get(),
            sink = get(),
            clock = get(),
            log = get(),
            scope = get<ApplicationScope>(),
            onlineFlow = SessionCache.onlineFlow,
            appResumedEvents = AppLifecycleManager.appResumedEvent,
        )
    }

    // Remote data sources. Explicit lambdas: the HttpClients are only registered by name.
    single<UserRemoteDataSource> { KtorUserRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<GroupRemoteDataSource> { KtorGroupRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<MessageRemoteDataSource> { KtorMessageRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<AuthRemoteDataSource> {
        KtorAuthRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)), get(), get(), get())
    }
    single<AppInfoRemoteDataSource> { KtorAppInfoRemoteDataSource(get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)), get()) }
    single<SettingsRemoteDataSource> { KtorSettingsRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<EventRemoteDataSource> { KtorEventRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<MapRemoteDataSource> { KtorMapRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<FeedbackRemoteDataSource> { KtorFeedbackRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<AnnouncementRemoteDataSource> { KtorAnnouncementRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }
    single<GameRemoteDataSource> { KtorGameRemoteDataSource(get(named(HTTPCLIENTTYPE.AUTHENTICATED)), get(), get()) }

    // AUTHENTICATED / NOT_AUTHENTICATED / SOCKET HttpClients are platform-specific (they need a
    // concrete HttpClientEngine) - see androidHttpModule, IosHttpModule, desktopHttpModule. Not
    // declared here.

    singleOf(::Navigator)


    //Repository
    singleOf(::DataStoreSettingsRepository) bind SettingsRepository::class
    singleOf(::RoomGroupDataSource) bind GroupLocalDataSource::class
    singleOf(::OfflineFirstGroupRepository) bind GroupRepository::class
    singleOf(::RoomUserDataSource) bind UserLocalDataSource::class
    singleOf(::OfflineFirstUserRepository) bind UserRepository::class
    singleOf(::RoomMessageDataSource) bind MessageLocalDataSource::class
    // Explicit lambda so the dispatcher keeps its default (Dispatchers.IO); tests pass their own (plan R4).
    // Sends and mark-read run in the app-wide scope, so they outlive the screen that started them (plan R2).
    single<MessageRepository> {
        OfflineFirstMessageRepository(
            database = get(),
            messageLocalDataSource = get(),
            messageRemoteDataSource = get(),
            pictureManager = get(),
            audioManager = get(),
            loggingRepository = get(),
            errorChannel = get(),
            externalScope = get<ApplicationScope>(),
        )
    }
    // Explicit lambda so the dispatcher keeps its default (Dispatchers.Default); tests pass their own (plan R4)
    single<ChatListRepository> {
        OfflineFirstChatListRepository(
            userLocalDataSource = get(),
            groupLocalDataSource = get(),
            messageLocalDataSource = get(),
            settingsRepository = get(),
            settingsRemoteDataSource = get(),
        )
    }
    singleOf(::RoomLoggingRepository) bind Logger::class
    singleOf(::RoomMapDataSource) bind MapLocalDataSource::class
    singleOf(::RoomEventDataSource) bind EventLocalDataSource::class
    singleOf(::OfflineFirstMapRepository) bind MapRepository::class
    singleOf(::OfflineFirstEventRepository) bind EventRepository::class
    singleOf(::RemoteAppInfoRepository) bind AppInfoRepository::class
    singleOf(::RemoteFeedbackRepository) bind FeedbackRepository::class
    singleOf(::RemoteAnnouncementRepository) bind AnnouncementRepository::class
    singleOf(::RemoteAuthRepository) bind AuthRepository::class
    singleOf(::LocalSessionTerminator) bind SessionTerminator::class
    // Explicit lambda for the scope: the sync runs in the app-wide scope, so it outlives the
    // screen that started it (plan R2). Depends on repositories only, never the other way round.
    single<DataSyncCoordinator> {
        DefaultDataSyncCoordinator(
            userRepository = get(),
            groupRepository = get(),
            messageRepository = get(),
            mapRepository = get(),
            eventRepository = get(),
            loggingRepository = get(),
            externalScope = get<ApplicationScope>(),
        )
    }
    singleOf(::GameHighscoreRepository)
    singleOf(::PlayerRepository)
    singleOf(::GameSaveRepository)
    singleOf(::CChallengeStreakRepository)
    // External puzzle archive fetch — no app auth, plain client
    single { CrosswordRepository(get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED))) }
    single { WordleRepository(get(named(HTTPCLIENTTYPE.NOT_AUTHENTICATED))) }


    // Socket Connection Manager
    single<SocketConnectionManager> {
        SocketConnectionManager(
            httpClient = get(named(HTTPCLIENTTYPE.SOCKET)),
            authSession = get(),
            // Desktop has no push channel: the socket must survive minimize/unfocus there.
            keepAliveInBackground = get<AppVersion>().isDesktop(),
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


    viewModelOf(::BirthdaysViewModel)

    viewModelOf(::NewChatViewModel)

    viewModelOf(::GroupCreatorViewModel)

    viewModelOf(::AutoLoginCredCheckerViewModel)

    viewModelOf(::EmailVerifiedCheckViewModel)

    viewModelOf(::LoginViewModel)

    viewModelOf(::SignUpViewModel)

    // Explicit lambda because the nullable initialEntryId can't be resolved by viewModelOf

    viewModel { (chatId: String, isGroup: Boolean) ->
        ChatViewModel(
            appVersion = get(),
            messageRepository = get(),
            userRepository = get(),
            groupRepository = get(),
            settingsRepository = get(),
            navigator = get(),
            loggingRepository = get(),
            pictureManager = get(),
            permissionsManager = get(),
            audioManager = get(),
            chatId = chatId,
            isGroup = isGroup
        )

    }

    // Explicit lambda because the runtime chatId/isGroup can't be resolved by viewModelOf
    viewModel { (chatId: String, isGroup: Boolean) ->
        ChatDetailsViewmodel(
            groupRepository = get(),
            navigator = get(),
            userRepository = get(),
            pictureManager = get(),
            eventRepository = get(),
            messageRepository = get(),
            chatId = chatId,
            isGroup = isGroup
        )
    }

    // Explicit lambda because the runtime chatId/isGroup/initialTab can't be resolved by viewModelOf
    viewModel { (chatId: String, isGroup: Boolean, initialTab: SharedContentTab) ->
        SharedContentViewModel(
            messageRepository = get(),
            userRepository = get(),
            navigator = get(),
            pictureManager = get(),
            chatId = chatId,
            isGroup = isGroup,
            initialTab = initialTab
        )
    }

    viewModel { (initialEntryId: String?, currentlyEditedEvent: Event?) ->
        SchneaggmapViewModel(
            navigator = get(),
            mapRepository = get(),
            dataSyncCoordinator = get(),
            settingsRepository = get(),
            locationService = get(),
            userRepository = get(),
            eventRepository = get(),
            initialEntryId = initialEntryId,
            currentlyEditedEvent = currentlyEditedEvent
        )
    }

    // Explicit lambda because the nullable targetUserId can't be resolved by viewModelOf
    viewModel { (targetUserId: String?) ->
        FriendCompassViewModel(
            targetUserId = targetUserId,
            navigator = get(),
            userRepository = get(),
            locationService = get()
        )
    }

    viewModel { (initialEntryId: String?, initialEntry: Event?) ->
        EventsViewModel(
            navigator = get(),
            appVersion = get(),
            dataSyncCoordinator = get(),
            eventRepository = get(),
            userRepository = get(),
            groupRepository = get(),
            settingsRepository = get(),
            pictureManager = get(),
            initialEntryId = initialEntryId,
            initialEntry = initialEntry
        )
    }





    //Settings
    viewModelOf(::SharedSettingsViewmodel)


    viewModelOf(::SettingsViewModel)


    viewModelOf(::DevSettingsViewModel)
    viewModelOf(::AnnouncementAdminViewModel)
    viewModelOf(::DartCounterViewModel)
    viewModelOf(::TowerstackViewModel)
    viewModelOf(::UndercoverViewModel)
    viewModelOf(::YatziViewModel)
    viewModelOf(::TetrisViewModel)
    viewModelOf(::MorseViewModel)
    viewModelOf(::CoinFlipViewModel)
    viewModelOf(::FingerPickerViewModel)
    viewModelOf(::FuelCalculatorViewModel)
    viewModelOf(::Game2048ViewModel)

    viewModelOf(::SchneaggaHusViewmodel)

    viewModelOf(::GridRushViewmodel)

    viewModelOf(::CChallengeViewmodel)

    viewModelOf(::OddOneOutViewmodel)

    viewModelOf(::SchneaggRodeoViewModel)

    viewModelOf(::CrosswordViewmodel)

    viewModelOf(::WordleViewModel)

    viewModelOf(::PlayerSelectorViewModel)
    viewModelOf(::HighscoresViewModel)
    viewModelOf(::RecapViewModel)
    viewModelOf(::GameSelectorViewModel)


    viewModelOf(::UserSettingsViewModel)

    viewModelOf(::PrivacyAndSecurityViewModel)
    viewModelOf(::LoggedInDevicesViewModel)

    viewModelOf(::NotificationSettingsViewModel)

    viewModelOf(::AppearanceSettingsViewModel)

    viewModelOf(::MiscSettingsViewModel)

    viewModelOf(::SchneaggmapSettingsViewModel)

    viewModelOf(::FeedbackViewModel)
}