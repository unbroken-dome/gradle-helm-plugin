package com.citi.gradle.plugins.helm.command.internal

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.Logger

class FileUtilsTest {
    @TempDir
    private lateinit var temporaryFolder: File

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun shouldRemoveFileAfterException(createFile: Boolean) {
        // given
        val logger = mockk<Logger> {
            justRun { info(any(), any<File>()) }
        }
        val fileToDelete = File(temporaryFolder, "file")
        if (createFile) {
            fileToDelete.createNewFile()
        }
        val exceptionToThrow = Exception();

        // when
        val actualException = shouldThrowAny {
            deleteFileOnException(fileToDelete, logger) {
                throw exceptionToThrow
            }
        }

        // then
        actualException shouldBe exceptionToThrow
        actualException.suppressedExceptions should beEmpty()
        fileToDelete shouldNot exist()
    }
}
