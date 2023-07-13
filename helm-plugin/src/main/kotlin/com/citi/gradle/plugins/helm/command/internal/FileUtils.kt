package com.citi.gradle.plugins.helm.command.internal

import java.io.File
import org.slf4j.Logger

internal inline fun <R> deleteFileOnException(file: File, logger: Logger, function: () -> R): R {
    // suppress false-positive warning: we re-throw the exception caught
    @Suppress("TooGenericExceptionCaught")
    try {
        return function()
    } catch (throwable: Throwable) {
        deleteFileIfExists(file, logger)

        throw throwable;
    }
}

/**
 * This is a separate function to offload inline method above (so, to avoid copying bytecode we just call this method)
 */
internal fun deleteFileIfExists(file: File, logger: Logger) {
    if (file.exists()) {
        file.delete()
    }

    logger.info("Deleting file at '{}' because of exception", file)
}
