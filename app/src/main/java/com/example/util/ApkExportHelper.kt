package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApkInfo(
    val fileName: String = "app-debug.apk",
    val projectArtifactPath: String = ".build-outputs/app-debug.apk",
    val gradleOutputPath: String = "app/build/outputs/apk/debug/app-debug.apk",
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int = 26,
    val targetSdk: Int = 34,
    val isDebugBuild: Boolean = true,
    val installedApkSize: String,
    val lastUpdateTime: String,
    val sourceDir: String
)

object ApkExportHelper {

    fun getApkInfo(context: Context): ApkInfo {
        val pm = context.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }

        val sourceFile = File(context.applicationInfo.sourceDir)
        val sizeInBytes = if (sourceFile.exists()) sourceFile.length() else 22781361L
        val formattedSize = formatFileSize(sizeInBytes)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val updateTime = dateFormat.format(Date(pkgInfo.lastUpdateTime))

        return ApkInfo(
            packageName = context.packageName,
            versionName = pkgInfo.versionName ?: "1.0",
            versionCode = versionCode,
            minSdk = 26,
            targetSdk = 34,
            isDebugBuild = true,
            installedApkSize = formattedSize,
            lastUpdateTime = updateTime,
            sourceDir = context.applicationInfo.sourceDir
        )
    }

    suspend fun shareApk(context: Context) = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(context.applicationInfo.sourceDir)
            if (!sourceFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cannot locate installed APK on device", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            // Copy to cache directory so FileProvider can safely share it
            val exportDir = File(context.cacheDir, "exported_apks").apply { mkdirs() }
            val destApk = File(exportDir, "GhostShield-v1.0-debug.apk")

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destApk).use { output ->
                    input.copyTo(output)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "GhostShield Android APK (v1.0)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "GhostShield: Anti-Ghost Touch & Dead Zone Blocker APK (v1.0 debug build). Ready for installation."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                val chooser = Intent.createChooser(shareIntent, "Share / Save GhostShield APK").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun copyPathToClipboard(context: Context, path: String = ".build-outputs/app-debug.apk") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("GhostShield APK Path", path)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied APK path to clipboard: $path", Toast.LENGTH_SHORT).show()
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
