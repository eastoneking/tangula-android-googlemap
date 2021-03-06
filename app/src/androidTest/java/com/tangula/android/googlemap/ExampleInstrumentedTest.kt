package com.tangula.android.googlemap

import android.app.Application
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.tangula.android.utils.ApplicationUtils

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        
        assertEquals("com.tangula.android.googlemap", appContext.packageName)
    }
}
