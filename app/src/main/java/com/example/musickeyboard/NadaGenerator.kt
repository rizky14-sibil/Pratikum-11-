package com.example.musickeyboard

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

data class Nota(val nama: String, val frekuensi: Double)

// Tuts putih, dari C4 sampai C5
val NOTA_PUTIH = listOf(
    Nota("C4", 261.63), Nota("D4", 293.66), Nota("E4", 329.63), Nota("F4", 349.23),
    Nota("G4", 392.00), Nota("A4", 440.00), Nota("B4", 493.88), Nota("C5", 523.25)
)

// Tuts hitam (kres dan mol)
val NOTA_HITAM = listOf(
    Nota("C#4", 277.18), Nota("D#4", 311.13), Nota("F#4", 369.99),
    Nota("G#4", 415.30), Nota("A#4", 466.16)
)

// Tuts hitam ke-i berada di sebelah kanan tuts putih ke-POSISI_HITAM[i]
val POSISI_HITAM = listOf(0, 1, 3, 4, 5)

private const val SAMPLE_RATE = 44100

// Membuat isi berkas WAV (16-bit, mono) berisi gelombang sinus.
fun buatWav(frekuensi: Double, durasiDetik: Double = 1.0): ByteArray {
    val jumlah = (SAMPLE_RATE * durasiDetik).toInt()
    val data = ByteBuffer.allocate(jumlah * 2).order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until jumlah) {
        val t = i.toDouble() / SAMPLE_RATE
        // Fade-in pendek dan peluruhan agar tidak terdengar klik.
        val amplop = min(1.0, i / 500.0) * min(1.0, (jumlah - i) / 2000.0) * exp(-2.0 * t)
        val nilai = sin(2 * PI * frekuensi * t) * amplop * 0.6
        data.putShort((nilai * Short.MAX_VALUE).toInt().toShort())
    }

    // Header WAV 44 byte
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray()).putInt(36 + data.capacity()).put("WAVE".toByteArray())
    header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
    header.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
    header.put("data".toByteArray()).putInt(data.capacity())

    return header.array() + data.array()
}

fun simpanWav(folder: File, nota: Nota): File {
    // Tanda # tidak dipakai di nama berkas, jadi diganti dengan s.
    val nama = nota.nama.replace("#", "s")
    val file = File(folder, "$nama.wav")
    if (!file.exists() || file.length() == 0L) {
        file.writeBytes(buatWav(nota.frekuensi))
    }
    return file
}
