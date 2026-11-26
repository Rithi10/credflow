package com.example.credflow.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates instant client-side logout without blocking navigation on network I/O.
 */
class LogoutCoordinator {
    private val logoutInProgress = AtomicBoolean(false)

    @Volatile
    private var pendingLocalClearJob: Job? = null

    fun isLoggingOut(): Boolean = logoutInProgress.get()

    fun resetForNextSession() {
        logoutInProgress.set(false)
    }

    suspend fun awaitPendingLocalCleanup() {
        pendingLocalClearJob?.join()
    }

    fun performLogout(
        scope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        readToken: () -> String?,
        cancelActiveSync: () -> Unit,
        clearSessionImmediate: () -> Unit,
        clearUiStateImmediate: () -> Unit,
        onNavigateToLogin: () -> Unit,
        clearLocalCacheSuspend: suspend () -> Unit,
        callRemoteLogoutSuspend: suspend (String?) -> Unit
    ): Boolean {
        if (!logoutInProgress.compareAndSet(false, true)) {
            return false
        }

        val token = try {
            readToken()
        } catch (_: Exception) {
            null
        }

        cancelActiveSync()
        clearSessionImmediate()
        clearUiStateImmediate()
        onNavigateToLogin()

        pendingLocalClearJob = scope.launch(ioDispatcher + NonCancellable) {
            try {
                clearLocalCacheSuspend()
            } catch (_: Exception) {
                // Keep user logged out even if local DB cleanup fails.
            }
        }

        scope.launch(ioDispatcher + NonCancellable) {
            try {
                if (!token.isNullOrBlank()) {
                    callRemoteLogoutSuspend(token)
                }
            } catch (_: Exception) {
                // Remote logout failure or timeout must never keep user logged in.
            }
        }

        return true
    }
}

