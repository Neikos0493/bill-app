package com.neikos0493.jileme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neikos0493.jileme.update.UpdateManager
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateManagerTest {
    @Test fun githubLatestReleaseIsReachableAndMatches()=kotlinx.coroutines.runBlocking{
        val context=ApplicationProvider.getApplicationContext<Context>()
        val manager=UpdateManager(context)
        val info=withTimeout(30_000){manager.check()}!!
        assertEquals(2,info.versionCode)
        assertEquals("1.0.1",info.versionName)
        assertFalse(manager.isNewer(info))
    }
}

