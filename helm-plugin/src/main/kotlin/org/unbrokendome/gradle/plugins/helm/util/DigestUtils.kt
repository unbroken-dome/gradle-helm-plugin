package com.citi.gradle.plugins.helm.util

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import javax.annotation.WillNotClose

private const val DefaultDigestAlgorithm = "SHA-256"
private const val DefaultBufferSize = DEFAULT_BUFFER_SIZE


/**
 * Calculates a digest over the contents of a [ReadableByteChannel].
 *
 * @param channel the [ReadableByteChannel] that contains the data. It will be read fully but not closed by this
 *        method.
 * @param algorithm the digest algorithm, e.g. `"SHA-256"`. Must be the name of an available
 *        [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as an array of bytes
 */
fun calculateDigest(
    @WillNotClose channel: ReadableByteChannel,
    algorithm: String = DefaultDigestAlgorithm,
    bufferSize: Int = DefaultBufferSize
): ByteArray {
    val digest = MessageDigest.getInstance(algorithm)

    val buffer = ByteBuffer.allocateDirect(bufferSize)

    while (true) {

        buffer.safeClear()

        val bytesRead = channel.read(buffer)
        if (bytesRead == -1) {
            break
        }

        buffer.flip()
        digest.update(buffer)
    }

    return digest.digest()
}


/**
 * Avoid using ByteBuffer.clear because of a JDK incompatibility that might lead to a NoSuchMethodError
 * when compiling with JDK > 8 and running with JDK 8.
 */
private fun ByteBuffer.safeClear() {
    position(0)
    limit(capacity())
}


/**
 * Calculates a digest over the contents of a file.
 *
 * @receiver the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"SHA-256"`. Must be the name of an available
 *        [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as an array of bytes
 */
fun Path.calculateDigest(
    algorithm: String = DefaultDigestAlgorithm,
    bufferSize: Int = DefaultBufferSize
): ByteArray {
    val fileSize = Files.size(this)
    val actualBufferSize = minOf(fileSize, bufferSize.toLong()).toInt()
    return FileChannel.open(this, StandardOpenOption.READ).use { channel ->
        calculateDigest(channel, algorithm, actualBufferSize)
    }
}


/**
 * Calculates a digest over the contents of a file.
 *
 * @receiver the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"SHA-256"`. Must be the name of an available
 *        [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as an array of bytes
 */
fun File.calculateDigest(
    algorithm: String = DefaultDigestAlgorithm,
    bufferSize: Int = DefaultBufferSize
): ByteArray =
    toPath().calculateDigest(algorithm, bufferSize)


/**
 * Calculates a digest over the contents of a file, and encodes it as a lower-case hexadecimal string.
 *
 * @receiver the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"SHA-256"`. Must be the name of an available
 *        [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as a hex-encoded string
 */
fun Path.calculateDigestHex(
    algorithm: String = DefaultDigestAlgorithm,
    bufferSize: Int = DefaultBufferSize
): String {
    val digestBytes = calculateDigest(algorithm, bufferSize)
    return buildString(digestBytes.size * 2) {
        digestBytes.forEach { byte ->
            append(HEX_ALPHABET[(byte.toInt() shr 4) and 0x0F])
            append(HEX_ALPHABET[byte.toInt() and 0x0F])
        }
    }
}


/**
 * Calculates a digest over the contents of a file, and encodes it as a lower-case hexadecimal string.
 *
 * @receiver the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"SHA-256"`. Must be the name of an available
 *        [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as a hex-encoded string
 */
fun File.calculateDigestHex(
    algorithm: String = DefaultDigestAlgorithm,
    bufferSize: Int = DefaultBufferSize
): String =
    toPath().calculateDigestHex(algorithm, bufferSize)


private val HEX_ALPHABET: CharArray = "0123456789abcdef".toCharArray()
