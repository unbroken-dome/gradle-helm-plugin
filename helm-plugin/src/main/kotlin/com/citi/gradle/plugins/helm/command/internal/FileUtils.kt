package com.citi.gradle.plugins.helm.command.internal

import java.io.File
import org.slf4j.Logger

internal inline fun <R> deleteFileOnException(file: File, logger: Logger, function: () -> R): R {
    // suppress false-positive warning: we re-throw the exception caught
    @Suppress("TooGenericExceptionCaught")
    try {
        return function()
    } catch (throwable: Throwable) {
        deleteFileSafe(file, logger)

        throw throwable;
    }
}

internal fun deleteFileSafe(file: File, logger: Logger) {
    if (file.exists()) {
        file.delete()
    }

    logger.info("Deleting file at '{}' because of exception", file)
}
