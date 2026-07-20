package org.lerchenflo.schneaggchatv3mp.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

/**
 * Application-wide coroutine scope for fire-and-forget work that must survive screen and
 * ViewModel lifecycles (e.g. finishing a message send while leaving the chat, or a data sync
 * kicked off during login). Lives for the whole process and is never cancelled; the
 * SupervisorJob keeps one failed job from cancelling the others.
 */
class ApplicationScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
