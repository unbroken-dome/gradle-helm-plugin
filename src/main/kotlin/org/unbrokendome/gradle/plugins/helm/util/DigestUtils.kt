package org.unbrokendome.gradle.plugins.helm.util

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import javax.annotation.WillNotClose


/**
 * Calculates a digest over the contents of a [ReadableByteChannel].
 *
 * @param channel the [ReadableByteChannel] that contains the data. It will be read fully but not closed by this
 *        method.
 * @param algorithm the digest algorithm, e.g. `"MD5"`. Must be the name of an available [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as an array of bytes
 */
internal fun calculateDigest(
    @WillNotClose channel: ReadableByteChannel,
    algorithm: String,
    bufferSize: Int = 4096
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
 * @param path the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"MD5"`. Must be the name of an available [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as an array of bytes
 */
internal fun calculateDigest(
    path: Path,
    algorithm: String,
    bufferSize: Int = 4096
): ByteArray =
    FileChannel.open(path, StandardOpenOption.READ).use { channel ->
        calculateDigest(channel, algorithm, bufferSize)
    }


/**
 * Calculates a digest over the contents of a file, and encodes it as a lower-case hexadecimal string.
 *
 * @param path the full path of the file for which to calculate the digest
 * @param algorithm the digest algorithm, e.g. `"MD5"`. Must be the name of an available [MessageDigest] implementation.
 * @param bufferSize the buffer size
 * @return the digest as a hex-encoded string
 */
internal fun calculateDigestHex(
    path: Path,
    algorithm: String,
    bufferSize: Int = 4096
): String {
    val digestBytes = calculateDigest(path, algorithm, bufferSize)
    return buildString(digestBytes.size * 2) {
        digestBytes.forEach { byte ->
            append(HEX_ALPHABET[(byte.toInt() shr 4) and 0x0F])
            append(HEX_ALPHABET[byte.toInt() and 0x0F])
        }
    }
}


private val HEX_ALPHABET: CharArray = "01234567890abcdef".toCharArray()
