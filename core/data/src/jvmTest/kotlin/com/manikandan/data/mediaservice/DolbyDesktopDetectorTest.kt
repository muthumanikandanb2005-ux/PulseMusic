package com.manikandan.data.mediaservice

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DolbyDesktopDetectorTest {
    @Test
    fun unsupportedWindowsMachineWithoutDolbySignalsReturnsFalse() {
        val result = DolbyDesktopDetector.evaluate(
            DolbyRegistrySignals(
                osName = "Windows 11",
                osVersion = "10.0",
                hasActiveDolbySoftwareEntry = false,
                hasActiveDolbyService = false,
                hasDolbyApoEntry = false,
                hasDolbySystemDriverFile = false,
            ),
        )
        assertFalse(result)
    }

    @Test
    fun supportedWindowsMachineWithDaxServiceOrApoReturnsTrue() {
        assertTrue(
            DolbyDesktopDetector.evaluate(
                DolbyRegistrySignals(
                    osName = "Windows 11",
                    osVersion = "10.0",
                    hasActiveDolbySoftwareEntry = false,
                    hasActiveDolbyService = true,
                    hasDolbyApoEntry = false,
                    hasDolbySystemDriverFile = false,
                ),
            ),
        )
        assertTrue(
            DolbyDesktopDetector.evaluate(
                DolbyRegistrySignals(
                    osName = "Windows 10",
                    osVersion = "10.0",
                    hasActiveDolbySoftwareEntry = false,
                    hasActiveDolbyService = false,
                    hasDolbyApoEntry = true,
                    hasDolbySystemDriverFile = false,
                ),
            ),
        )
        assertTrue(
            DolbyDesktopDetector.evaluate(
                DolbyRegistrySignals(
                    osName = "Windows 11",
                    osVersion = "10.0",
                    hasActiveDolbySoftwareEntry = false,
                    hasActiveDolbyService = false,
                    hasDolbyApoEntry = false,
                    hasDolbySystemDriverFile = true,
                ),
            ),
        )
    }

    @Test
    fun linuxNeverReportsDolbyHardwareSupport() {
        assertFalse(
            DolbyDesktopDetector.evaluate(
                DolbyRegistrySignals(
                    osName = "Linux",
                    osVersion = "6.8",
                    hasActiveDolbySoftwareEntry = true,
                    hasActiveDolbyService = true,
                    hasDolbyApoEntry = true,
                    hasDolbySystemDriverFile = true,
                ),
            ),
        )
    }

    @Test
    fun macOsChecksModernSpatialAudioVersion() {
        assertTrue(DolbyDesktopDetector.isMacOsSpatialSupported("14.6.1"))
        assertFalse(DolbyDesktopDetector.isMacOsSpatialSupported("10.15.7"))
    }
}
