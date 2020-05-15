package org.unbrokendome.gradle.plugins.helm.util


internal object SystemUtils {

    private val osName = System.getProperty("os.name")
    private val osArch = System.getProperty("os.arch")


    fun getOperatingSystemClassifier() =
        when {
            osName.startsWith("Windows") ->
                "windows-amd64"
            osName.startsWith("Mac OS X") ->
                "darwin-amd64"
            osName.startsWith("Linux") ->
                when (osArch) {
                    "amd64" -> "linux-amd64"
                    "arm" -> "linux-arm"
                    "arm64" -> "linux-arm64"
                    "i386" -> "linux-i386"
                    "ppc64le" -> "linux-ppc64le"
                    "s390x" -> "linux-s390x"
                    else -> null
                }
            else -> null
        } ?: throw IllegalStateException("Cannot determine operating system name and version")


    /**
     * Gets the default archive format for the current operating system.
     *
     * For Windows systems, this will be "zip", for all others it will be "tar.gz".
     *
     * @return the default archive format for the current OS
     */
    fun getOperatingSystemArchiveFormat() =
        if (osName.startsWith("Windows")) "zip" else "tar.gz"
}
