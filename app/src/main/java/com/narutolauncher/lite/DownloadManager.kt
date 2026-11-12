package com.narutolauncher.lite

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object DownloadManager {

    // URL download komponen emulator (ganti dengan URL yang valid)
    private const val WINE_URL = "https://github.com/brunodev85/winlator/releases/download/v7.1/wine-9.0-arm64.tar.gz"
    private const val BOX64_URL = "https://github.com/ptitSeb/box64/releases/download/v0.2.8/box64-0.2.8-android-arm64.tar.gz"
    
    private val client = OkHttpClient()

    fun downloadEmulatorFiles(context: Context, onProgress: (Int, String) -> Unit): Boolean {
        val baseDir = File(context.getExternalFilesDir(null), "NarutoStorm")
        if (!baseDir.exists()) baseDir.mkdirs()

        try {
            // Download Wine
            onProgress(10, "📥 Downloading Wine...")
            val wineFile = File(baseDir, "wine.tar.gz")
            downloadFile(WINE_URL, wineFile) { progress ->
                onProgress(10 + (progress * 0.4).toInt(), "📥 Wine: $progress%")
            }
            
            // Extract Wine
            onProgress(50, "📦 Extracting Wine...")
            extractTarGz(wineFile, File(baseDir, "wine"))
            wineFile.delete()

            // Download Box64
            onProgress(60, "📥 Downloading Box64...")
            val box64File = File(baseDir, "box64.tar.gz")
            downloadFile(BOX64_URL, box64File) { progress ->
                onProgress(60 + (progress * 0.3).toInt(), "📥 Box64: $progress%")
            }
            
            // Extract Box64
            onProgress(90, "📦 Extracting Box64...")
            extractTarGz(box64File, File(baseDir, "box64"))
            box64File.delete()

            onProgress(100, "✅ Setup selesai!")
            return true

        } catch (e: Exception) {
            onProgress(-1, "❌ Error: ${e.message}")
            return false
        }
    }

    private fun downloadFile(url: String, outputFile: File, onProgress: (Int) -> Unit) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw Exception("Empty response")
        
        val contentLength = body.contentLength()
        var bytesRead = 0L
        
        body.byteStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    val progress = ((bytesRead * 100) / contentLength).toInt()
                    onProgress(progress)
                }
            }
        }
    }

    private fun extractTarGz(tarGzFile: File, outputDir: File) {
        if (!outputDir.exists()) outputDir.mkdirs()
        
        // Gunakan command line tar (butuh Android dengan akses shell)
        val process = ProcessBuilder(
            "tar", "-xzf", tarGzFile.absolutePath, "-C", outputDir.absolutePath
        ).start()
        process.waitFor()
        
        if (process.exitValue() != 0) {
            throw Exception("Extraction failed")
        }
    }
}
