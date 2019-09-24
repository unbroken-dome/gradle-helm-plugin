package org.unbrokendome.gradle.plugins.helm.util

import java.io.FilterReader
import java.io.Reader
import java.nio.CharBuffer


/**
 * Implementation of [FilterReader] that delegates all [Reader] methods to another reader.
 */
internal abstract class DelegateReader(input: Reader) : FilterReader(input) {

    /**
     * The delegate [Reader].
     */
    protected abstract val delegate: Reader


    override fun read(): Int =
        delegate.read()


    override fun read(cbuf: CharArray, off: Int, len: Int): Int =
        delegate.read(cbuf, off, len)


    override fun read(target: CharBuffer): Int =
        delegate.read(target)


    override fun read(cbuf: CharArray): Int =
        delegate.read(cbuf)


    override fun skip(n: Long): Long =
        delegate.skip(n)


    override fun ready(): Boolean =
        delegate.ready()


    override fun markSupported(): Boolean =
        delegate.markSupported()


    override fun mark(readAheadLimit: Int) =
        delegate.mark(readAheadLimit)


    override fun reset() =
        delegate.reset()


    override fun close() =
        delegate.close()
}
