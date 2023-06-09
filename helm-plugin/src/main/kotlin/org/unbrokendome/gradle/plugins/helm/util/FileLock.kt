package com.citi.gradle.plugins.helm.util

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


private val locksByLockFile = ConcurrentHashMap<File, ReentrantLock>()


internal fun <T> withLockFile(file: File, block: () -> T): T {
    // File locks won't work within the same JVM, we have to synchronize access to the lockfile
    // with a ReentrantLock
    return locksByLockFile.computeIfAbsent(file) { ReentrantLock() }
        .withLock {
            file.parentFile.mkdirs()
            RandomAccessFile(file, "rw").channel.use { channel ->
                channel.lock().use {
                    block()
                }
            }
        }
}
