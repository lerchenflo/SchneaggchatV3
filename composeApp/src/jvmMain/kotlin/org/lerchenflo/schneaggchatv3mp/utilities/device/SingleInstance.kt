package org.lerchenflo.schneaggchatv3mp.utilities.device

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * One desktop app process per data folder, so two processes never write the same database and a
 * second start never adds a second taskbar entry or tray icon (user, 2026-11-20: seen on Windows and
 * Linux). The first process holds a file lock and listens on a local socket file; a later start
 * (dev builds too, they share the folder) finds the lock taken, asks the running process to show its
 * window and ends.
 */
class SingleInstance private constructor(
    // Kept open for the whole process: closing the channel would release the lock. Both null when
    // the folder allows no lock: the app then runs without the check.
    @Suppress("unused") private val lockChannel: FileChannel?,
    @Suppress("unused") private val lock: FileLock?,
) {
    private val showRequestChannel = Channel<Unit>(Channel.CONFLATED)

    /** Emits each time a later start asked this process to show its window. */
    val showRequests: Flow<Unit> = showRequestChannel.receiveAsFlow()

    /** Listens for later starts. A socket that can't be bound only costs the hand-over, never the start. */
    private fun listen(socketFile: Path) {
        val server = try {
            // Left behind by a crashed run; safe to remove, this process holds the lock
            Files.deleteIfExists(socketFile)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socketFile))
        } catch (e: IOException) {
            println("SingleInstance: no hand-over socket (${e.message})")
            return
        }
        thread(isDaemon = true, name = "single-instance-listener") {
            while (true) {
                try {
                    server.accept().use { client ->
                        val message = ByteBuffer.allocate(1)
                        if (client.read(message) == 1 && message.get(0) == SHOW_WINDOW) {
                            showRequestChannel.trySend(Unit)
                        }
                    }
                } catch (e: ClosedChannelException) {
                    return@thread
                } catch (e: IOException) {
                    println("SingleInstance: hand-over failed (${e.message})")
                }
            }
        }
    }

    companion object {
        private const val LOCK_FILE = "instance.lock"
        private const val SOCKET_FILE = "instance.sock"
        private const val SHOW_WINDOW: Byte = 1

        // The running process may still be starting and not listen yet
        private val HAND_OVER_TIMEOUT = 3.seconds
        private const val HAND_OVER_RETRY_MS = 200L

        /**
         * Takes this process's place as the only one. Null when another process already runs: it
         * was asked to show its window, and this start must end without starting the app.
         */
        fun acquire(dataDir: File = desktopAppDataDir()): SingleInstance? {
            val socketFile = File(dataDir, SOCKET_FILE).toPath()
            val channel = try {
                FileChannel.open(File(dataDir, LOCK_FILE).toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            } catch (e: IOException) {
                return withoutCheck(e)
            }
            val lock = try {
                channel.tryLock()
            } catch (e: OverlappingFileLockException) {
                // Only a second call in this same process; closing the channel could drop the held lock
                return null
            } catch (e: IOException) {
                channel.close()
                return withoutCheck(e)
            }

            if (lock == null) {
                channel.close()
                askRunningProcessToShowWindow(socketFile)
                return null
            }
            return SingleInstance(channel, lock).also { it.listen(socketFile) }
        }

        /** A folder without file locks must not stop the app; it then runs without the check. */
        private fun withoutCheck(error: IOException): SingleInstance {
            println("SingleInstance: no lock possible (${error.message}), starting without the check")
            return SingleInstance(lockChannel = null, lock = null)
        }

        private fun askRunningProcessToShowWindow(socketFile: Path) {
            val start = TimeSource.Monotonic.markNow()
            while (start.elapsedNow() < HAND_OVER_TIMEOUT) {
                try {
                    SocketChannel.open(UnixDomainSocketAddress.of(socketFile)).use { socket ->
                        socket.write(ByteBuffer.wrap(byteArrayOf(SHOW_WINDOW)))
                    }
                    return
                } catch (e: IOException) {
                    Thread.sleep(HAND_OVER_RETRY_MS)
                }
            }
            println("SingleInstance: the running app did not answer, ending this start anyway")
        }
    }
}
