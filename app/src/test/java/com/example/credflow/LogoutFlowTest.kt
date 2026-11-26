package com.example.credflow

import com.example.credflow.util.LogoutCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class LogoutFlowTest {

    @Test
    fun logout_clearsSessionAndNavigatesImmediately_withoutWaitingForSlowServerLogout(): Unit = runBlocking {
        val coordinator = LogoutCoordinator()
        var token: String? = "tok_active_123"
        var uiCleared = false
        var navigatedToLogin = false
        var syncCancelled = false
        val localCleared = CompletableDeferred<Unit>()
        val remoteStarted = CompletableDeferred<Unit>()
        val allowRemoteFinish = CompletableDeferred<Unit>()

        val started = coordinator.performLogout(
            scope = this,
            ioDispatcher = Dispatchers.Default,
            readToken = { token },
            cancelActiveSync = { syncCancelled = true },
            clearSessionImmediate = { token = null },
            clearUiStateImmediate = { uiCleared = true },
            onNavigateToLogin = { navigatedToLogin = true },
            clearLocalCacheSuspend = { localCleared.complete(Unit) },
            callRemoteLogoutSuspend = { passedToken ->
                assertEquals("tok_active_123", passedToken)
                remoteStarted.complete(Unit)
                allowRemoteFinish.await()
            }
        )

        // Navigation and local session clear happened synchronously before remote logout finishes.
        assertTrue(started)
        assertTrue(syncCancelled)
        assertNull(token)
        assertTrue(uiCleared)
        assertTrue(navigatedToLogin)
        assertFalse(allowRemoteFinish.isCompleted)

        // Let background local cleanup and hanging remote logout complete.
        localCleared.await()
        remoteStarted.await()
        allowRemoteFinish.complete(Unit)
    }

    @Test
    fun logout_preventsMultipleRequestsOnRepeatedTaps_andResetsCleanlyForNextLogin(): Unit = runBlocking {
        val coordinator = LogoutCoordinator()
        val remoteCalls = AtomicInteger(0)
        val navCalls = AtomicInteger(0)
        var token: String? = "tok_user_1"
        val firstLocalClearDone = CompletableDeferred<Unit>()

        val first = coordinator.performLogout(
            scope = this,
            ioDispatcher = Dispatchers.Default,
            readToken = { token },
            cancelActiveSync = {},
            clearSessionImmediate = { token = null },
            clearUiStateImmediate = {},
            onNavigateToLogin = { navCalls.incrementAndGet() },
            clearLocalCacheSuspend = {
                delay(40)
                firstLocalClearDone.complete(Unit)
            },
            callRemoteLogoutSuspend = { remoteCalls.incrementAndGet() }
        )

        val secondDuplicateTap = coordinator.performLogout(
            scope = this,
            ioDispatcher = Dispatchers.Default,
            readToken = { token },
            cancelActiveSync = {},
            clearSessionImmediate = { token = null },
            clearUiStateImmediate = {},
            onNavigateToLogin = { navCalls.incrementAndGet() },
            clearLocalCacheSuspend = {},
            callRemoteLogoutSuspend = { remoteCalls.incrementAndGet() }
        )

        assertTrue(first)
        assertFalse(secondDuplicateTap)
        firstLocalClearDone.await()
        delay(20)
        assertEquals(1, navCalls.get())
        assertEquals(1, remoteCalls.get())

        // Simulate next login: previous local cleanup completes cleanly before new session logout is allowed again.
        coordinator.awaitPendingLocalCleanup()
        coordinator.resetForNextSession()
        token = "tok_user_2"
        val secondLocalClearDone = CompletableDeferred<Unit>()

        val nextLogout = coordinator.performLogout(
            scope = this,
            ioDispatcher = Dispatchers.Default,
            readToken = { token },
            cancelActiveSync = {},
            clearSessionImmediate = { token = null },
            clearUiStateImmediate = {},
            onNavigateToLogin = { navCalls.incrementAndGet() },
            clearLocalCacheSuspend = { secondLocalClearDone.complete(Unit) },
            callRemoteLogoutSuspend = { remoteCalls.incrementAndGet() }
        )

        assertTrue(nextLogout)
        assertNull(token)
        secondLocalClearDone.await()
        delay(20)
        assertEquals(2, navCalls.get())
        assertEquals(2, remoteCalls.get())
    }

    @Test
    fun logout_handlesRemoteTimeoutOrFailureWithoutKeepingUserLoggedIn(): Unit = runBlocking {
        val coordinator = LogoutCoordinator()
        var token: String? = "tok_timeout"
        var navigated = false
        val remoteAttempted = CompletableDeferred<Unit>()

        coordinator.performLogout(
            scope = this,
            ioDispatcher = Dispatchers.Default,
            readToken = { token },
            cancelActiveSync = {},
            clearSessionImmediate = { token = null },
            clearUiStateImmediate = {},
            onNavigateToLogin = { navigated = true },
            clearLocalCacheSuspend = {},
            callRemoteLogoutSuspend = {
                remoteAttempted.complete(Unit)
                throw IOException("Simulated network timeout")
            }
        )

        assertTrue(navigated)
        assertNull(token)
        remoteAttempted.await()
        assertNull(token)
    }
}

