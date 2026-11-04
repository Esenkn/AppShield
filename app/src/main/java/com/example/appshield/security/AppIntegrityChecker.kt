package com.example.appshield.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class IntegrityResult(
    val packageName: String,
    val certificateSha256: String,
    val apkSha256: String?,
    val isDebuggable: Boolean,
    val packageIdentityValid: Boolean,
    val signingCertificateAvailable: Boolean,
    val releaseCertificateMatches: Boolean,
    val findings: List<String>,
    val information: List<String>
) {
    val isHealthy: Boolean
        get() = findings.isEmpty()
}

class AppIntegrityChecker(private val context: Context) {

    companion object {
        private const val EXPECTED_PACKAGE =
            "com.esenkn.appshield"

        private const val RELEASE_CERTIFICATE_SHA256 =
            "0a8b0ec69a8799cf891e28a2574c25e5b93b8f8e5862a8b08411df1a261d9b8b"
    }

    fun check(): IntegrityResult {
        val findings = mutableListOf<String>()
        val information = mutableListOf<String>()

        val packageName = context.packageName
        val isDebuggable =
            (context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val packageIdentityValid =
            packageName == EXPECTED_PACKAGE

        if (!packageIdentityValid) {
            findings.add("Unexpected application package identity")
        }

        if (isDebuggable) {
            information.add("Development build: debugging is enabled")
        }

        val certificate = getSigningCertificate()

        val certificateSha256 = certificate?.let {
            sha256(it)
        } ?: ""

        val certificateAvailable = certificate != null

        if (!certificateAvailable) {
            findings.add("Signing certificate is unavailable")
        }

        val releaseCertificateMatches =
            certificateSha256.equals(
                RELEASE_CERTIFICATE_SHA256,
                ignoreCase = true
            )

        if (certificateAvailable && !releaseCertificateMatches) {
            if (isDebuggable) {
                information.add(
                    "Development signing certificate detected"
                )
            } else {
                findings.add(
                    "Release signing certificate does not match"
                )
            }
        }

        val apkSha256 = calculateApkSha256()

        if (apkSha256 == null) {
            information.add("APK file fingerprint could not be calculated")
        }

        return IntegrityResult(
            packageName = packageName,
            certificateSha256 = certificateSha256,
            apkSha256 = apkSha256,
            isDebuggable = isDebuggable,
            packageIdentityValid = packageIdentityValid,
            signingCertificateAvailable = certificateAvailable,
            releaseCertificateMatches = releaseCertificateMatches,
            findings = findings,
            information = information
        )
    }

    private fun getSigningCertificate(): ByteArray? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )

                val signingInfo = info.signingInfo ?: return null

                val signatures =
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }

                signatures.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )

                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateApkSha256(): String? {
        return try {
            val path = context.applicationInfo.sourceDir
                ?: return null

            val digest = MessageDigest.getInstance("SHA-256")

            File(path).inputStream().use { input ->
                val buffer = ByteArray(8192)

                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    digest.update(buffer, 0, count)
                }
            }

            digest.digest().joinToString("") {
                "%02x".format(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")

        return digest.digest(data).joinToString("") {
            "%02x".format(it)
        }
    }
}
