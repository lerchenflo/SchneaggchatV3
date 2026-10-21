package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application-wide coroutine scope for fire-and-forget work that must survive screen and
 * ViewModel lifecycles (e.g. finishing a message send while leaving the chat, or a data sync
 * kicked off during login). Lives for the whole process and is never cancelled; the
 * SupervisorJob keeps one failed job from cancelling the others.
 */
class ApplicationScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Like `viewModelScope.launch` (main thread, starts right away), but in the app-wide scope: for a
 * user action whose steps must all run even when its screen is left meanwhile - a logout and the
 * navigation after it, or a server write per friend.
 */
fun ApplicationScope.launchOnMain(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.Main.immediate, block = block)
