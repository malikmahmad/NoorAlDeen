package com.nooraldeen.app

fun juzStarts(ayahs: List<Ayah>): Map<Int, Ayah> =
    ayahs.filter { it.juz in 1..30 }.groupBy { it.juz }
        .mapValues { (_, records) -> records.minBy { it.id } }

fun bookmarkKey(surahId: Int, ayahNumber: Int): String = "$surahId:$ayahNumber"

fun readingProgress(previous: Int, visibleAyah: Int): Int =
    maxOf(previous, visibleAyah).coerceAtLeast(0)

fun matchesAyah(ayah: Ayah, query: String): Boolean =
    ayah.arabic.contains(query) ||
        ayah.translation.contains(query, ignoreCase = true) ||
        ayah.tafseer.contains(query, ignoreCase = true)
