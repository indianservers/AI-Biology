package com.indianservers.AIbiology

import com.indianservers.AIbiology.data.ModelRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelRepositoryValidationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun acceptsGlbWithValidVersionAndDeclaredLength() {
        val file = temporaryFolder.newFile("valid.glb")
        file.writeBytes(glbBytes())

        assertTrue(ModelRepository.isValidGlb(file))
    }

    @Test
    fun rejectsGlbWithUnsupportedVersion() {
        val file = temporaryFolder.newFile("wrong-version.glb")
        file.writeBytes(glbBytes(version = 1))

        assertFalse(ModelRepository.isValidGlb(file))
    }

    @Test
    fun rejectsGlbWhoseHeaderLengthDoesNotMatchFile() {
        val file = temporaryFolder.newFile("truncated.glb")
        file.writeBytes(glbBytes(declaredLength = 24))

        assertFalse(ModelRepository.isValidGlb(file))
    }

    @Test
    fun rejectsFileThatOnlyHasTheGlbMagicBytes() {
        val file = temporaryFolder.newFile("magic-only.glb")
        file.writeBytes("glTF".toByteArray() + ByteArray(16))

        assertFalse(ModelRepository.isValidGlb(file))
    }

    private fun glbBytes(version: Int = 2, declaredLength: Int = 20): ByteArray =
        ByteBuffer.allocate(20)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put("glTF".toByteArray())
            .putInt(version)
            .putInt(declaredLength)
            .putInt(0)
            .putInt(0x4E4F534A)
            .array()
}
