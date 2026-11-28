package com.pulse.music.expect.ui

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowsTrustInspectorTest {
    @Test
    fun peCertificateTableDetectorRejectsPlainAndUnsignedFiles() {
        val temp = createTempFile("unsigned-pe", ".exe").toFile()
        try {
            temp.writeBytes(ByteArray(512))
            assertFalse(WindowsTrustInspector.hasPeCertificateTable(temp))

            // Valid DOS MZ + PE32+ header with zeroed Security directory (DataDirectory[4])
            val pe = ByteArray(512)
            pe[0] = 'M'.code.toByte()
            pe[1] = 'Z'.code.toByte()
            ByteBuffer.wrap(pe, 0x3C, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x80)
            pe[0x80] = 'P'.code.toByte()
            pe[0x81] = 'E'.code.toByte()
            ByteBuffer.wrap(pe, 0x80 + 24, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(0x020B.toShort())
            temp.writeBytes(pe)
            assertFalse(WindowsTrustInspector.hasPeCertificateTable(temp))

            // Populate DataDirectory[4] pointing within file bounds -> detected as signed PE container
            val certDirOffset = 0x80 + 24 + 144
            ByteBuffer.wrap(pe, certDirOffset, 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(384)
                .putInt(64)
            temp.writeBytes(pe)
            assertTrue(WindowsTrustInspector.hasPeCertificateTable(temp))
        } finally {
            temp.delete()
        }
    }

    @Test
    fun inspectFastReturnsSnapshotWithoutCrashing() {
        val snapshot = WindowsTrustInspector.inspectFast()
        val isWin = System.getProperty("os.name", "").contains("Windows", ignoreCase = true)
        assertTrue(snapshot.isWindows == isWin)
    }
}
