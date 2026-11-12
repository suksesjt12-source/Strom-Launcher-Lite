package com.narutolauncher.lite

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnPlay: Button
    private lateinit var btnDownload: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlay)
        btnDownload = findViewById(R.id.btnDownload)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        checkEmulatorFiles()

        btnDownload.setOnClickListener {
            startDownload()
        }

        btnPlay.setOnClickListener {
            launchGame()
        }
    }

    private fun checkEmulatorFiles() {
        val baseDir = File(getExternalFilesDir(null), "NarutoStorm")
        val wineBin = File(baseDir, "wine/bin/wine64")
        val box64Bin = File(baseDir, "box64/box64")

        if (wineBin.exists() && box64Bin.exists()) {
            statusText.text = "✅ Emulator siap digunakan!"
            btnPlay.isEnabled = true
            btnDownload.isEnabled = false
        } else {
            statusText.text = "⚠️ File emulator belum di-download.\nKlik tombol di bawah untuk setup (butuh ~500MB)."
            btnPlay.isEnabled = false
            btnDownload.isEnabled = true
        }
    }

    private fun startDownload() {
        btnDownload.isEnabled = false
        statusText.text = "📥 Memulai download..."
        progressBar.progress = 0

        Thread {
            val success = DownloadManager.downloadEmulatorFiles(this) { progress, message ->
                runOnUiThread {
                    if (progress >= 0) {
                        progressBar.progress = progress
                        statusText.text = message
                    } else {
                        statusText.text = message
                        btnDownload.isEnabled = true
                    }
                }
            }

            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "✅ Setup selesai!", Toast.LENGTH_SHORT).show()
                    checkEmulatorFiles()
                } else {
                    Toast.makeText(this, "❌ Download gagal. Cek koneksi internet.", Toast.LENGTH_LONG).show()
                    btnDownload.isEnabled = true
                }
            }
        }.start()
    }

    private fun launchGame() {
        val gamePath = findGameExe()
        
        if (gamePath == null) {
            Toast.makeText(this, "❌ File game tidak ditemukan!\nTaruh di folder: NarutoStorm/game/", Toast.LENGTH_LONG).show()
            return
        }

        statusText.text = "🚀 Meluncurkan game..."
        
        Thread {
            try {
                val process = EmulatorEngine.launchGame(this, gamePath)
                process?.waitFor()
                runOnUiThread {
                    statusText.text = "✅ Game selesai."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "❌ Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()

        moveTaskToBack(true)
    }

    private fun findGameExe(): String? {
        val gameDir = File(getExternalFilesDir(null), "NarutoStorm/game")
        if (!gameDir.exists()) return null

        return gameDir.walk()
            .filter { it.extension.equals("exe", ignoreCase = true) }
            .firstOrNull()?.absolutePath
    }
}
