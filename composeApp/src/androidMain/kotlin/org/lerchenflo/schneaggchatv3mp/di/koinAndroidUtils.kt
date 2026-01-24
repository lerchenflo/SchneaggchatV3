package org.lerchenflo.schneaggchatv3mp.di

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.app.logging.LogType
import org.lerchenflo.schneaggchatv3mp.app.logging.LoggingRepository
import java.io.PrintWriter
import java.io.StringWriter

fun startKoinAndroid(androidContext: Context) {
    startKoin {
        //androidLogger(Level.DEBUG) // Enable Koin logging
        androidContext(androidContext)

        //Modules für room Userdatabase
        modules(androidUserDatabaseModule, sharedmodule)

        //Modules für Httpclient
        modules(
            androidHttpModule,
            androidHttpAuthModule,
            androidDataStoreModule,
            androidVersionModule,
            androidPictureManagerModule,
            androidShareUtilsModule,
            androidLanguageManagerModule
        )
    }
}

fun logCrash(throwable: Throwable) {
    val loggingRepository = KoinPlatform.getKoin().get<LoggingRepository>()
    runBlocking {
        loggingRepository.log(
            message = getFullStackTrace(throwable),
            logType = LogType.ERROR
        )
    }
}

private fun getFullStackTrace(throwable: Throwable): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)

    // Print this throwable
    throwable.printStackTrace(pw)

    // Walk through causes
    var cause = throwable.cause
    while (cause != null) {
        pw.println("Caused by:")
        cause.printStackTrace(pw)
        cause = cause.cause
    }

    // Include suppressed
    val suppressed = throwable.suppressed
    if (suppressed.isNotEmpty()) {
        pw.println("Suppressed exceptions:")
        for (sup in suppressed) {
            sup.printStackTrace(pw)
        }
    }



    pw.flush()
    return sw.toString()
}