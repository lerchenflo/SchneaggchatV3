package org.lerchenflo.schneaggchatv3mp.testutil

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.lerchenflo.schneaggchatv3mp.datasource.database.AppDatabase
import kotlin.coroutines.CoroutineContext

/**
 * Fresh in-memory [AppDatabase] on the bundled SQLite driver, set up like the app's database
 * (`CreateAppDatabase`) and `ChatLoadingBenchmark`. Every call returns an empty database; close it
 * in `@AfterTest`.
 *
 * Queries run on [queryContext] (default `Dispatchers.IO`, as in the app).
 */
fun createInMemoryAppDatabase(queryContext: CoroutineContext = Dispatchers.IO): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .build()
