package com.example.criminalintent.limingxuan249400218

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class Crime(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var suspect: String = "",
    var audioPath: String = "",
    var audioData: ByteArray? = null
) {
    val photoFileName
        get() = "IMG_$id.jpg"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Crime

        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false

        return true
    }

    override fun hashCode(): Int {
        return audioData?.contentHashCode() ?: 0
    }
}

