package com.miyo.doctorsaludapp.domain.util

object MimeSniffer {
    fun sniff(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) return "image/png"
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()) return "image/jpeg"
        return "image/png"
    }
}
