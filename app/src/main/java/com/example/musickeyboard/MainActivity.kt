package com.example.musickeyboard

import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private lateinit var tvStatus: TextView
    private val idSuara = ConcurrentHashMap<String, Int>()
    @Volatile private var jumlahTerisi = 0
    private val jumlahNada = NOTA_PUTIH.size + NOTA_HITAM.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val baris = findViewById<LinearLayout>(R.id.barisPutih)
        val piano = findViewById<FrameLayout>(R.id.piano)

        // 1. Siapkan SoundPool untuk bunyi pendek dengan latensi rendah.
        val atribut = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(atribut)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                jumlahTerisi++
                if (jumlahTerisi == jumlahNada) {
                    runOnUiThread {
                        tvStatus.text = "Siap dimainkan"
                    }
                }
            } else {
                runOnUiThread {
                    tvStatus.text = "Gagal memuat salah satu nada"
                }
            }
        }

        // 2. Buat & muat berkas WAV di background thread agar tidak membekukan UI thread (ANR / System UI freeze)
        lifecycleScope.launch(Dispatchers.IO) {
            val folder = File(cacheDir, "nada").apply { mkdirs() }
            val semuaNota = NOTA_PUTIH + NOTA_HITAM
            for (nota in semuaNota) {
                val file = simpanWav(folder, nota)
                val id = soundPool.load(file.absolutePath, 1)
                idSuara[nota.nama] = id
            }
        }

        // 3. Tuts putih: delapan tuts dengan lebar sama.
        NOTA_PUTIH.forEach { nota ->
            val tuts = buatTuts(nota, Color.WHITE, Color.BLACK)
            tuts.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            )
            baris.addView(tuts)
        }

        // 4. Tuts hitam: diletakkan di antara tuts putih setelah ukuran diketahui.
        piano.post {
            val lebarPutih = baris.width / NOTA_PUTIH.size.toFloat()
            val lebarHitam = lebarPutih * 0.6f
            val tinggiHitam = baris.height * 0.6f

            NOTA_HITAM.forEachIndexed { i, nota ->
                val tuts = buatTuts(nota, Color.BLACK, Color.WHITE)
                tuts.layoutParams = FrameLayout.LayoutParams(
                    lebarHitam.toInt(), tinggiHitam.toInt(), Gravity.TOP or Gravity.START
                )
                tuts.x = (POSISI_HITAM[i] + 1) * lebarPutih - lebarHitam / 2
                piano.addView(tuts)
            }
        }
    }

    private fun buatTuts(nota: Nota, warna: Int, warnaTeks: Int): Button {
        return Button(this).apply {
            text = nota.nama
            setBackgroundColor(warna)
            setTextColor(warnaTeks)
            setOnClickListener { mainkan(nota.nama) }
        }
    }

    private fun mainkan(nama: String) {
        val id = idSuara[nama] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
        tvStatus.text = "Memainkan $nama"
    }

    override fun onDestroy() {
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
        super.onDestroy()
    }
}
