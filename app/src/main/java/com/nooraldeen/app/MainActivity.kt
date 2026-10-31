package com.nooraldeen.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class Surah(
    val id: Int,
    val name_arabic: String,
    val name_english: String,
    val name_english_translation: String,
    val revelation_type: String,
    val ayah_count: Int,
    val juz_start: Int,
    val page_start: Int
)

data class Ayah(
    val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val juz: Int,
    val page: Int,
    val arabic: String,
    val translation: String,
    val tafseer: String
)

data class QuranData(val surahs: List<Surah>, val ayahs: List<Ayah>)

data class HadithRecord(
    val id: Int,
    val number: Int,
    val arabic: String,
    val english: String,
    val urdu: String,
    val book: String,
    val chapter: String,
    val narrator: String
)

data class HadithCollection(val name: String, val records: List<HadithRecord>)
data class HadithSource(val key: String, val name: String, val count: Int)
data class JuzStart(val juz: Int, val surahId: Int, val ayahNumber: Int)

private val hadithSources = listOf(
    HadithSource("bukhari", "Sahih al-Bukhari", 7563),
    HadithSource("muslim", "Sahih Muslim", 7564),
    HadithSource("tirmazi", "Jami at-Tirmidhi", 3956),
    HadithSource("abu_dawood", "Sunan Abu Dawood", 5274),
    HadithSource("nasai", "Sunan an-Nasa'i", 5761),
    HadithSource("ibn_majah", "Sunan Ibn Majah", 4341)
)

private enum class Destination { HOME, QURAN, QIBLA, HADITH, MORE, PRAYER, CALENDAR, SEARCH, SETTINGS, TAFSEER, JUZ, LIBRARY }

private const val PREFS = "noor_preferences"
private const val BOOKMARKS = "bookmarks"
private const val LAST_SURAH = "last_surah"
private const val LAST_AYAH = "last_ayah"
private const val HADITH_BOOKMARKS = "hadith_bookmarks"
private const val ARABIC_SIZE = "arabic_size"
private const val TRANSLATION_SIZE = "translation_size"
private const val DAILY_GOAL = "daily_goal"
private const val THEME_MODE = "theme_mode"
private const val LINE_SPACING = "line_spacing"
private const val SHOW_TRANSLATION = "show_translation"
private const val NOTES = "notes"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val data = loadQuran(this)
        setContent { NoorAlDeenApp(data) }
    }

    private fun loadQuran(context: Context): QuranData = try {
        context.assets.open("quran_reader.json").use { stream ->
            InputStreamReader(stream).use { reader ->
                Gson().fromJson(reader, object : TypeToken<QuranData>() {}.type)
            }
        }
    } catch (_: Exception) {
        QuranData(emptyList(), emptyList())
    }
}

@Composable
private fun NoorAlDeenApp(data: QuranData) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var previousDestination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var returnDestination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var selectedSurahId by rememberSaveable { mutableStateOf<Int?>(null) }
    var bookmarks by remember {
        mutableStateOf(preferences.getStringSet(BOOKMARKS, emptySet())?.toSet() ?: emptySet())
    }
    var hadithBookmarks by remember {
        mutableStateOf(preferences.getStringSet(HADITH_BOOKMARKS, emptySet())?.toSet() ?: emptySet())
    }
    var themeMode by rememberSaveable { mutableStateOf(preferences.getString(THEME_MODE, "dark") ?: "dark") }
    var arabicSize by rememberSaveable { mutableStateOf(preferences.getInt(ARABIC_SIZE, 27)) }
    var translationSize by rememberSaveable { mutableStateOf(preferences.getInt(TRANSLATION_SIZE, 17)) }
    var lineSpacing by rememberSaveable { mutableStateOf(preferences.getInt(LINE_SPACING, 29)) }
    var selectedHadith by rememberSaveable { mutableStateOf<String?>(null) }
    var startAyah by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedSurah = data.surahs.firstOrNull { it.id == selectedSurahId }
    val isDark = when (themeMode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    BackHandler(enabled = selectedSurahId != null || selectedHadith != null || destination != Destination.HOME.name) {
        when {
            selectedSurahId != null -> {
                selectedSurahId = null
                startAyah = null
                destination = returnDestination
            }
            selectedHadith != null -> {
                selectedHadith = null
                destination = returnDestination
            }
            else -> {
                destination = previousDestination
                previousDestination = Destination.HOME.name
            }
        }
    }

    MaterialTheme(
        colorScheme = if (isDark) darkColors() else lightColors(),
        typography = NoorTypography
    ) {
        SideEffect {
            (context as? Activity)?.window?.apply {
                statusBarColor = android.graphics.Color.parseColor(if (isDark) "#101827" else "#F6F0E5")
                navigationBarColor = android.graphics.Color.parseColor(if (isDark) "#101827" else "#F6F0E5")
                decorView.systemUiVisibility = if (isDark) 0 else
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        Surface(Modifier.fillMaxSize()) {
            if (selectedSurah != null) {
                QuranReader(
                    surah = selectedSurah,
                    allSurahs = data.surahs,
                    ayahs = data.ayahs.filter { it.surahId == selectedSurah.id },
                    bookmarks = bookmarks,
                    startAyah = startAyah,
                    arabicSize = arabicSize,
                    translationSize = translationSize,
                    lineSpacing = lineSpacing,
                    onBookmark = { ayah ->
                        val key = "${selectedSurah.id}:${ayah.ayahNumber}"
                        bookmarks = if (key in bookmarks) bookmarks - key else bookmarks + key
                        preferences.edit { putStringSet(BOOKMARKS, bookmarks) }
                    },
                    onAyahViewed = { ayah ->
                        preferences.edit {
                            putInt(LAST_SURAH, selectedSurah.id)
                            putInt(LAST_AYAH, ayah.ayahNumber)
                            putInt("daily_progress", maxOf(preferences.getInt("daily_progress", 0), ayah.ayahNumber))
                        }
                    },
                    onBack = {
                        selectedSurahId = null
                        startAyah = null
                        destination = returnDestination
                    },
                    onOpenSurah = { selectedSurahId = it.id; startAyah = null }
                )
            } else if (selectedHadith != null) {
                val openedHadith = selectedHadith
                HadithDetail(
                    collection = openedHadith!!.substringBefore('|'),
                    record = rememberHadithRecord(context, openedHadith),
                    bookmarked = openedHadith in hadithBookmarks,
                    onBookmark = {
                        hadithBookmarks = if (openedHadith in hadithBookmarks) hadithBookmarks - openedHadith else hadithBookmarks + openedHadith
                        preferences.edit { putStringSet(HADITH_BOOKMARKS, hadithBookmarks) }
                    },
                    onBack = {
                        selectedHadith = null
                        destination = returnDestination
                    }
                )
            } else {
                MainShell(
                    destination = Destination.valueOf(destination),
                    onDestination = {
                        if (it.name != destination) {
                            previousDestination = destination
                            destination = it.name
                        }
                    },
                    onOpenSurah = {
                        returnDestination = destination
                        selectedSurahId = it.id
                    },
                    onOpenHadith = {
                        returnDestination = destination
                        selectedHadith = it
                    },
                    hadithBookmarks = hadithBookmarks,
                    onHadithBookmark = { key ->
                        hadithBookmarks = if (key in hadithBookmarks) hadithBookmarks - key else hadithBookmarks + key
                        preferences.edit { putStringSet(HADITH_BOOKMARKS, hadithBookmarks) }
                    },
                    surahs = data.surahs,
                    ayahs = data.ayahs,
                    bookmarks = bookmarks,
                    themeMode = themeMode,
                    arabicSize = arabicSize,
                    translationSize = translationSize,
                    lineSpacing = lineSpacing,
                    onSettings = { mode, arabic, translation, spacing, goal ->
                        themeMode = mode
                        arabicSize = arabic
                        translationSize = translation
                        lineSpacing = spacing
                        preferences.edit {
                            putString(THEME_MODE, mode)
                            putBoolean("dark_theme", mode == "dark")
                            putInt(ARABIC_SIZE, arabic)
                            putInt(TRANSLATION_SIZE, translation)
                            putInt(LINE_SPACING, spacing)
                            putInt(DAILY_GOAL, goal)
                        }
                    },
                    onOpenJuz = { juz ->
                        val ayah = data.ayahs.firstOrNull { it.juz == juz }
                        returnDestination = destination
                        selectedSurahId = ayah?.surahId
                        startAyah = ayah?.ayahNumber
                    },
                    onOpenLibrary = {
                        previousDestination = destination
                        destination = Destination.LIBRARY.name
                    }
                )
            }
        }
    }
}

@Composable
private fun MainShell(
    destination: Destination,
    onDestination: (Destination) -> Unit,
    onOpenSurah: (Surah) -> Unit,
    onOpenHadith: (String) -> Unit,
    hadithBookmarks: Set<String>,
    onHadithBookmark: (String) -> Unit,
    surahs: List<Surah>,
    ayahs: List<Ayah>,
    bookmarks: Set<String>,
    themeMode: String,
    arabicSize: Int,
    translationSize: Int,
    lineSpacing: Int,
    onSettings: (String, Int, Int, Int, Int) -> Unit,
    onOpenJuz: (Int) -> Unit,
    onOpenLibrary: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF101827),
                contentColor = Color(0xFFF6F0E5),
                tonalElevation = 0.dp
            ) {
                listOf(
                    Destination.HOME to "Home",
                    Destination.QURAN to "Quran",
                    Destination.QIBLA to "Qibla",
                    Destination.HADITH to "Hadith",
                    Destination.MORE to "More"
                ).forEach { (item, label) ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { onDestination(item) },
                        icon = { Icon(painterResource(item.icon()), contentDescription = label) },
                        label = { Text(label) },
                        alwaysShowLabel = true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFD6B46A),
                            selectedTextColor = Color(0xFFD6B46A),
                            unselectedIconColor = Color(0xFFAEB8C5),
                            unselectedTextColor = Color(0xFFAEB8C5),
                            indicatorColor = Color(0xFFD6B46A).copy(alpha = .16f)
                        )
                    )
                }

            }
        }
    ) { padding ->
        when (destination) {
            Destination.HOME -> HomeScreen(padding, surahs, ayahs, bookmarks, hadithBookmarks, onOpenSurah, onOpenHadith, onDestination, onOpenJuz, onOpenLibrary)
            Destination.QURAN -> SurahIndex(padding, surahs, onOpenSurah, onOpenJuz)
            Destination.QIBLA -> QiblaUnavailable(padding)
            Destination.HADITH -> HadithLibrary(padding, onOpenHadith, hadithBookmarks, onHadithBookmark)
            Destination.MORE -> MoreScreen(padding, onDestination)
            Destination.PRAYER -> PrayerTimesScreen(padding)
            Destination.CALENDAR -> CalendarScreen(padding)
            Destination.SEARCH -> SearchScreen(padding, surahs, ayahs, onOpenSurah, onOpenHadith)
            Destination.SETTINGS -> SettingsScreen(padding, themeMode, arabicSize, translationSize, lineSpacing, onSettings)
            Destination.TAFSEER -> TafseerScreen(padding, surahs, ayahs, onOpenSurah)
            Destination.JUZ -> JuzIndex(padding, ayahs, surahs, onOpenJuz)
            Destination.LIBRARY -> LibraryScreen(padding, surahs, ayahs, bookmarks, hadithBookmarks, onOpenSurah, onOpenHadith, onDestination)
        }
    }
}

private fun Destination.icon(): Int = when (this) {
    Destination.HOME -> R.drawable.ic_home
    Destination.QURAN -> R.drawable.ic_book
    Destination.HADITH -> R.drawable.ic_library
    Destination.QIBLA -> R.drawable.ic_book
    Destination.MORE -> R.drawable.ic_settings
    Destination.PRAYER -> R.drawable.ic_settings
    Destination.CALENDAR -> R.drawable.ic_book
    Destination.SEARCH -> R.drawable.ic_search
    Destination.SETTINGS -> R.drawable.ic_settings
    Destination.TAFSEER -> R.drawable.ic_book
    Destination.JUZ -> R.drawable.ic_book
    Destination.LIBRARY -> R.drawable.ic_library
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    padding: PaddingValues,
    surahs: List<Surah>,
    ayahs: List<Ayah>,
    bookmarks: Set<String>,
    hadithBookmarks: Set<String>,
    onOpenSurah: (Surah) -> Unit,
    onOpenHadith: (String) -> Unit,
    onDestination: (Destination) -> Unit,
    onOpenJuz: (Int) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val lastSurah = surahs.firstOrNull { it.id == prefs.getInt(LAST_SURAH, 1) } ?: surahs.firstOrNull()
    val dailyAyah = remember(ayahs) { dailyAyahForToday(ayahs) }
    val dailyHadith = rememberDailyHadith(context)
    val goal = prefs.getInt(DAILY_GOAL, 5)
    val progress = prefs.getInt("daily_progress", 0)
    val green = Color(0xFF17243A)
    val ivory = Color(0xFFF6F0E5)
    val parchment = Color(0xFF1D2A40)
    val gold = Color(0xFFD6B46A)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(green).padding(padding),
        contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Assalamu Alaikum,", color = ivory.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
                    Text("Noor Al-Deen", color = ivory, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                    Text("A quiet place for Quran, reflection, and remembrance.", color = ivory.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            DatePrayerBar(green, parchment, ivory, gold)
        }
        item {

            Card(colors = CardDefaults.cardColors(containerColor = parchment), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = green, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("۞", color = gold, fontSize = 27.sp) }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Continue Reading", color = ivory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${lastSurah?.name_english ?: "Al-Fatihah"} · Ayah ${prefs.getInt(LAST_AYAH, 1)}", color = ivory.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.LinearProgressIndicator(progress = (progress.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = gold, trackColor = green)
                    }
                    if (lastSurah != null) TextButton(onClick = { onOpenSurah(lastSurah) }) { Text("›", fontSize = 28.sp, color = gold) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Explore Islam", color = ivory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf(
                    listOf("Surah" to Destination.QURAN, "Para / Juz" to Destination.JUZ, "Prayer Times" to Destination.MORE),
                    listOf("Tafseer" to Destination.TAFSEER, "Calendar" to Destination.MORE, "Bookmarks" to Destination.LIBRARY),
                    listOf("Search" to Destination.SEARCH, "Settings" to Destination.MORE, "Library" to Destination.LIBRARY)
                ).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, target) ->
                            Surface(modifier = Modifier.weight(1f).clickable { onDestination(target) }, shape = RoundedCornerShape(12.dp), color = parchment) {
                                Column(Modifier.padding(vertical = 12.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(painterResource(target.icon()), contentDescription = label, tint = gold, modifier = Modifier.size(22.dp))
                                    Text(label, color = ivory, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Goal", color = ivory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$progress of $goal ayahs read today", color = ivory.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.LinearProgressIndicator(progress = (progress.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = gold, trackColor = ivory.copy(alpha = .2f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = parchment), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Ayah", color = ivory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Quran", color = gold, style = MaterialTheme.typography.labelMedium)
                    }
                    dailyAyah?.let {
                        Text(it.arabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), fontSize = 25.sp, lineHeight = 42.sp, color = ivory)
                        Divider(color = gold.copy(alpha = .5f))
                        Text(it.translation, style = MaterialTheme.typography.bodyMedium, color = ivory.copy(alpha = .85f), lineHeight = 23.sp, maxLines = 4)
                        if (it.tafseer.isNotBlank()) {
                            Text("Tafseer", color = gold, style = MaterialTheme.typography.labelMedium)
                            Text(it.tafseer.trim(), color = ivory.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp, maxLines = 4)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Surah ${it.surahId} · Ayah ${it.ayahNumber}", color = ivory.copy(alpha = .65f), style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { onOpenSurah(surahs.firstOrNull { surah -> surah.id == it.surahId } ?: surahs.first()) }) { Text("Read in context", color = gold) }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Daily Hadith", color = ivory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                dailyHadith?.let { (collection, record) ->
                    Column(Modifier.fillMaxWidth().clickable { onOpenHadith("$collection|${record.id}") }) {
                        Text(record.english.ifBlank { record.urdu }.trim(), maxLines = 3, style = MaterialTheme.typography.bodyMedium, color = ivory.copy(alpha = .88f))
                        Text("$collection · Hadith ${record.number}", color = gold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun dailyAyahForToday(ayahs: List<Ayah>): Ayah? {
    if (ayahs.isEmpty()) return null
    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val eligible = ayahs.filter { it.ayahNumber > 1 && it.arabic.isNotBlank() && it.translation.isNotBlank() }
    if (eligible.isEmpty()) return ayahs.firstOrNull()
    return eligible.getOrNull((day - 1) % eligible.size) ?: eligible.firstOrNull()
}

@Composable
private fun DatePrayerBar(green: Color, surface: Color, ivory: Color, gold: Color) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var location by remember {
        mutableStateOf(
            preferences.getString("location_latitude", null)?.toDoubleOrNull()?.let { lat ->
                preferences.getString("location_longitude", null)?.toDoubleOrNull()?.let { lon -> lat to lon }
            }
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            location = readLastLocation(context)
            location?.let { (lat, lon) ->
                preferences.edit { putString("location_latitude", lat.toString()); putString("location_longitude", lon.toString()) }
            }
        }
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            location = readLastLocation(context)
        } else if (!permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val now = remember { Calendar.getInstance() }
    val englishDate = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now.time) }
    val hijri = remember { civilHijriDate(now) }
    val prayerTimes = location?.let { calculatePrayerTimes(it.first, it.second, now) }
    val nextPrayer = prayerTimes?.next(now)
    Surface(color = surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(hijri, color = ivory, style = MaterialTheme.typography.titleMedium)
                Text(englishDate, color = ivory.copy(alpha = .68f), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(nextPrayer?.let { "Next: ${it.name}" } ?: "Prayer times", color = gold, style = MaterialTheme.typography.labelMedium)
                Text(nextPrayer?.let { it.time } ?: "Location permission required", color = ivory.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun readLastLocation(context: Context): Pair<Double, Double>? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    return providers.asSequence().mapNotNull { provider ->
        try {
            manager.getLastKnownLocation(provider)?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        }
    }.firstOrNull()
}

private fun civilHijriDate(calendar: Calendar): String {
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    val jd = (367 * year - (7 * (year + (month + 9) / 12)) / 4 + (275 * month) / 9 + day + 1721013.5).toInt()
    val islamicYear = ((30 * (jd - 1948439) + 10646) / 10631)
    val firstDay = 1948439 + 354 * (islamicYear - 1) + (3 + 11 * islamicYear) / 30
    val islamicMonth = (((jd - firstDay) / 29.5).toInt() + 1).coerceIn(1, 12)
    val islamicDay = jd - firstDay - (29.5 * (islamicMonth - 1)).toInt() + 1
    val months = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul Qi'dah", "Dhul Hijjah")
    return "$islamicDay ${months[(islamicMonth - 1).coerceIn(0, 11)]} $islamicYear AH"
}

private data class PrayerTime(val name: String, val minutes: Int) {
    val time: String get() = "%02d:%02d".format(Locale.US, (minutes / 60) % 24, minutes % 60)
}

private fun calculatePrayerTimes(latitude: Double, longitude: Double, calendar: Calendar): List<PrayerTime> {
    val radians = Math.PI / 180.0
    val day = calendar.get(Calendar.DAY_OF_YEAR)
    val gamma = 2.0 * Math.PI / 365.0 * (day - 1)
    val equationOfTime = 229.18 * (
        0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
        )
    val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
        0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
        0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    val timezone = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / 3_600_000.0
    val solarNoon = 720.0 - 4.0 * longitude - equationOfTime + timezone * 60.0

    fun event(name: String, altitudeDegrees: Double, morning: Boolean): PrayerTime? {
        val altitude = altitudeDegrees * radians
        val cosineHour = (sin(altitude) - sin(latitude * radians) * sin(declination)) /
            (cos(latitude * radians) * cos(declination))
        if (cosineHour < -1.0 || cosineHour > 1.0) return null
        val hourAngle = acos(cosineHour) / radians / 15.0
        val minutes = solarNoon + if (morning) -hourAngle * 60.0 else hourAngle * 60.0
        return PrayerTime(name, minutes.roundToInt().mod(1440))
    }

    val asrAltitude = atan(1.0 / (1.0 + tan(abs(latitude * radians - declination)))) / radians
    return listOfNotNull(
        event("Fajr", -18.0, true),
        event("Sunrise", -0.833, true),
        PrayerTime("Dhuhr", solarNoon.roundToInt().mod(1440)),
        event("Asr", asrAltitude, false),
        event("Maghrib", -0.833, false),
        event("Isha", -18.0, false)
    )
}

private fun List<PrayerTime>.next(now: Calendar): PrayerTime? {
    val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    return firstOrNull { it.minutes >= current } ?: firstOrNull()
}

@Composable
private fun MoreScreen(padding: PaddingValues, onDestination: (Destination) -> Unit) {
    val items = listOf(
        "Tafseer Library" to Destination.TAFSEER,
        "Prayer Times" to Destination.PRAYER,
        "Islamic Calendar" to Destination.CALENDAR,
        "Bookmarks & Notes" to Destination.LIBRARY,
        "Global Search" to Destination.SEARCH,
        "Settings" to Destination.SETTINGS
    )
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("More", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Your Islamic companion tools", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        items(items) { (label, destination) ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onDestination(destination) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(destination.icon()), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (destination) {
                                Destination.PRAYER -> "Daily timetable and next prayer"
                                Destination.CALENDAR -> "Hijri and Gregorian dates"
                                Destination.TAFSEER -> "Read ayah-linked commentary"
                                Destination.LIBRARY -> "Bookmarks, notes and progress"
                                Destination.SEARCH -> "Find Quran and Hadith content"
                                else -> "Personalize your reading experience"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaUnavailable(padding: PaddingValues) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val location = remember {
        prefs.getString("location_latitude", null)?.toDoubleOrNull()?.let { lat ->
            prefs.getString("location_longitude", null)?.toDoubleOrNull()?.let { lon -> lat to lon }
        }
    }
    val bearing = location?.let { qiblaBearing(it.first, it.second) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Qibla", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Text("Find the direction of the Kaaba from your location.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Surface(Modifier.fillMaxWidth().height(270.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Text("⌾", color = MaterialTheme.colorScheme.secondary, fontSize = 82.sp)
                Text(bearing?.let { "${it.roundToInt()}° from North" } ?: "Location permission required", style = MaterialTheme.typography.titleLarge)
                Text(if (bearing != null) "Qibla bearing calculated from your saved location" else "Allow location access on Home to calculate Qibla", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("Keep your phone flat and use the bearing above with your device compass. A live rotating compass will be enabled when orientation sensors are available.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun qiblaBearing(latitude: Double, longitude: Double): Double {
    val radians = Math.PI / 180.0
    val kaabaLat = 21.422487 * radians
    val kaabaLon = 39.826206 * radians
    val userLat = latitude * radians
    val deltaLon = kaabaLon - longitude * radians
    return ((atan2(sin(deltaLon), cos(userLat) * tan(kaabaLat) - sin(userLat) * cos(deltaLon)) / radians) + 360.0) % 360.0
}

@Composable
private fun PrayerTimesScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val location = remember {
        prefs.getString("location_latitude", null)?.toDoubleOrNull()?.let { lat ->
            prefs.getString("location_longitude", null)?.toDoubleOrNull()?.let { lon -> lat to lon }
        }
    }
    val now = remember { Calendar.getInstance() }
    val prayers = location?.let { calculatePrayerTimes(it.first, it.second, now) }
    val next = prayers?.next(now)
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Prayer Times", style = MaterialTheme.typography.headlineMedium)
            Text("Today’s timetable based on your saved location.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp)) {
                    Text(next?.let { "Next prayer · ${it.name}" } ?: "Location required", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                    Text(next?.time ?: "Allow location access from the Home screen", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                    Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now.time), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        if (prayers == null) {
            item { Text("Prayer times become available after location permission is granted. Noor Al-Deen never displays made-up timings.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(prayers.filter { it.name != "Sunrise" }, key = { it.name }) { prayer ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (prayer == next) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(prayer.name, style = MaterialTheme.typography.titleMedium)
                        Text(prayer.time, color = if (prayer == next) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(padding: PaddingValues) {
    val today = remember { Calendar.getInstance() }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Islamic Calendar", style = MaterialTheme.typography.headlineMedium); Text("Today in both calendars", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(civilHijriDate(today), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.headlineSmall)
                    Text(SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(today.time), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    Text("Dates use the civil Hijri conversion and may differ from local moon-sighting announcements.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahIndex(padding: PaddingValues, surahs: List<Surah>, onOpenSurah: (Surah) -> Unit, onOpenJuz: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = surahs.filter { query.isBlank() || it.name_english.contains(query, true) || it.name_arabic.contains(query) }
    Scaffold(topBar = { TopAppBar(title = { Column { Text("The Quran"); Text("114 surahs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) } }) }) { inner ->
        Column(Modifier.fillMaxSize().padding(padding).padding(inner)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), label = { Text("Search Surahs") }, singleLine = true)
            TextButton(onClick = { onOpenJuz(1) }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Browse all 30 Juz") }
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(filtered, key = { it.id }) { surah ->
                    Column(Modifier.fillMaxWidth().clickable { onOpenSurah(surah) }) {
                        Row(Modifier.padding(vertical = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${surah.id.toString().padStart(3, '0')}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.size(16.dp))
                                Column {
                                    Text(surah.name_english, style = MaterialTheme.typography.titleMedium)
                                    Text("${surah.name_english_translation}  ·  ${surah.revelation_type}  ·  ${surah.ayah_count} ayahs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Text(surah.name_arabic, fontSize = 22.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onOpenSurah(surah) }) {
                                Text("Listen", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuranReader(
    surah: Surah,
    allSurahs: List<Surah>,
    ayahs: List<Ayah>,
    bookmarks: Set<String>,
    startAyah: Int?,
    arabicSize: Int,
    translationSize: Int,
    lineSpacing: Int,
    onBookmark: (Ayah) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onBack: () -> Unit,
    onOpenSurah: (Surah) -> Unit
) {
    var showTranslation by rememberSaveable { mutableStateOf(true) }
    var fontSize by rememberSaveable { mutableStateOf(arabicSize) }
    var audioPlaying by rememberSaveable { mutableStateOf(false) }
    var audioLoading by rememberSaveable { mutableStateOf(false) }
    var audioError by rememberSaveable { mutableStateOf<String?>(null) }
    val player = remember(surah.id) { MediaPlayer() }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
    fun stopAudio() {
        if (player.isPlaying) player.stop()
        player.reset()
        audioPlaying = false
        audioLoading = false
    }
    fun playAudio() {
        audioError = null
        audioLoading = true
        try {
            player.reset()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource("https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/${surah.id.toString().padStart(3, '0')}.mp3")
            player.setOnPreparedListener {
                audioLoading = false
                audioPlaying = true
                it.start()
            }
            player.setOnCompletionListener {
                audioPlaying = false
            }
            player.setOnErrorListener { _, _, _ ->
                audioLoading = false
                audioPlaying = false
                audioError = "Audio could not be loaded. Check your internet connection."
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            audioLoading = false
            audioPlaying = false
            audioError = "Audio could not be loaded. Check your internet connection."
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(surah.id) {
        if (startAyah == null) listState.scrollToItem(0)
    }
    LaunchedEffect(startAyah, surah.id) {
        startAyah?.let { target -> listState.scrollToItem(target.coerceIn(1, ayahs.size)) }
    }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        ayahs.getOrNull((listState.firstVisibleItemIndex - 1).coerceAtLeast(0))?.let(onAyahViewed)
    }
    val previous = allSurahs.firstOrNull { it.id == surah.id - 1 }
    val next = allSurahs.firstOrNull { it.id == surah.id + 1 }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface, topBar = {
        TopAppBar(
            title = { Text("${surah.id}. ${surah.name_english}") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            actions = {
                TextButton(onClick = {
                    if (audioLoading || audioPlaying) stopAudio() else playAudio()
                }) {
                    Text(if (audioLoading) "Loading…" else if (audioPlaying) "Stop audio" else "Listen")
                }
                TextButton(onClick = { showTranslation = !showTranslation }) { Text(if (showTranslation) "Hide" else "Translate") }
            }
        )
    }) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Text(
                    "Qari: Sheikh Abdul Rahman Al-Sudais",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium
                )
                audioError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(surah.name_arabic, fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${surah.revelation_type}  ·  ${surah.ayah_count} ayahs  ·  Juz ${surah.juz_start}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                    if (surah.id != 9) Text("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ", fontSize = 24.sp, modifier = Modifier.padding(top = 18.dp), color = MaterialTheme.colorScheme.onSurface)
                    Row { TextButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(20) }) { Text("A-") }; TextButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(42) }) { Text("A+") } }
                }
            }
            items(ayahs, key = { it.id }) { ayah ->
                Column(Modifier.fillMaxWidth()) {
                    Text(ayah.arabic, Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), fontSize = fontSize.sp, lineHeight = (fontSize * 1.9f).sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("۝ ${ayah.ayahNumber}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { onBookmark(ayah) }) { Text(if ("${surah.id}:${ayah.ayahNumber}" in bookmarks) "Saved" else "Bookmark") }
                    }
                    if (showTranslation && ayah.translation.isNotBlank()) Text(ayah.translation, fontSize = translationSize.sp, lineHeight = lineSpacing.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ayah.tafseer.isNotBlank()) Text("Tafseer available", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Divider(Modifier.padding(top = 12.dp))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (previous != null) TextButton(onClick = { onOpenSurah(previous) }) { Text("‹ Previous") }
                    if (next != null) TextButton(onClick = { onOpenSurah(next) }) { Text("Next ›") }
                }
            }
        }
    }
}

@Composable
private fun HadithLibrary(
    padding: PaddingValues,
    onOpenHadith: (String) -> Unit,
    bookmarks: Set<String>,
    onBookmark: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = selectedCollection != null) {
        selectedCollection = null
        query = ""
    }
    val source = hadithSources.firstOrNull { it.key == selectedCollection }
    val collection = source?.let { rememberHadithCollection(context, it.key) }
    if (source == null) {
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                Text("Hadith library", style = MaterialTheme.typography.headlineMedium)
                Text("Six offline collections", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            }
            items(hadithSources, key = { it.key }) { item ->
                Column(Modifier.fillMaxWidth().clickable { selectedCollection = item.key }) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(item.name, style = MaterialTheme.typography.titleLarge)
                            Text("${item.count} records · offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text("›", fontSize = 26.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedCollection = null }) { Text("Back") }
                Text(source.name, style = MaterialTheme.typography.titleLarge)
            }
            if (collection == null) {
                Text("Loading ${source.name}…", color = MaterialTheme.colorScheme.secondary)
                return@Column
            }
            val loadedCollection = collection
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search this collection") }, singleLine = true)
            val records = loadedCollection.records.filter {
                query.length < 2 || it.arabic.contains(query) || it.english.contains(query, true) || it.urdu.contains(query)
            }.take(300)
            LazyColumn(contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(records, key = { it.id }) { record ->
                    Column(Modifier.fillMaxWidth().clickable { onOpenHadith("${loadedCollection.name}|${record.id}") }.padding(vertical = 14.dp)) {
                        Text("Hadith ${record.number}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        if (record.arabic.isNotBlank()) Text(record.arabic, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), fontSize = 20.sp, lineHeight = 32.sp)
                        Text((record.english.ifBlank { record.urdu }).trim(), maxLines = 3, modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onBookmark("${loadedCollection.name}|${record.id}") }) {
                            Text(if ("${loadedCollection.name}|${record.id}" in bookmarks) "Saved" else "Save")
                        }
                        Divider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithDetail(
    collection: String,
    record: HadithRecord?,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(collection) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { padding ->
        if (record == null) {
            Text("This Hadith could not be opened from the packaged collection.", Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Text("Hadith ${record.number}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    if (record.book.isNotBlank()) Text(record.book, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onBookmark) { Text(if (bookmarked) "Remove bookmark" else "Bookmark") }
                }
                item { Text(record.arabic, Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), fontSize = 24.sp, lineHeight = 39.sp) }
                if (record.english.isNotBlank()) item { Text(record.english.trim(), style = MaterialTheme.typography.bodyLarge) }
                if (record.urdu.isNotBlank()) item { Text(record.urdu.trim(), style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), textAlign = TextAlign.End) }
                if (record.chapter.isNotBlank()) item { Text(record.chapter.trim(), color = MaterialTheme.colorScheme.secondary) }
                if (record.narrator.isNotBlank()) item { Text("Narrator: ${record.narrator.trim()}", color = MaterialTheme.colorScheme.secondary) }
            }
        }
    }
}

@Composable
private fun rememberHadithCollection(context: Context, key: String): HadithCollection? {
    val state = produceState<HadithCollection?>(initialValue = null, context, key) {
        value = withContext(Dispatchers.IO) { loadHadithCollection(context, key) }
    }
    return state.value
}

@Composable
private fun rememberDailyHadith(context: Context): Pair<String, HadithRecord>? {
    val state = produceState<Pair<String, HadithRecord>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open("hadith_daily.json").use { stream ->
                    InputStreamReader(stream).use { reader ->
                        val type = object : TypeToken<List<String>>() {}.type
                        val entries = Gson().fromJson<List<String>>(reader, type)
                        val entry = entries.firstOrNull() ?: return@withContext null
                        val collection = entry.substringBefore('|')
                        val record = Gson().fromJson(entry.substringAfter('|'), HadithRecord::class.java)
                        collection to record
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("NoorAlDeen", "Unable to load daily Hadith", error)
                null
            }
        }
    }
    return state.value
}

private fun loadHadithCollection(context: Context, key: String): HadithCollection? = try {
    context.assets.open("hadith_$key.json").use { stream ->
        InputStreamReader(stream).use { reader ->
            Gson().fromJson(reader, HadithCollection::class.java)
        }
    }
} catch (error: Exception) {
    android.util.Log.e("NoorAlDeen", "Unable to load Hadith collection: $key", error)
    null
}

private fun loadHadithRecord(context: Context, selection: String): HadithRecord? {
    val name = selection.substringBefore('|')
    val id = selection.substringAfter('|').toIntOrNull() ?: return null
    val key = when (name) {
        "Sahih al-Bukhari" -> "bukhari"
        "Sahih Muslim" -> "muslim"
        "Jami at-Tirmidhi" -> "tirmazi"
        "Sunan Abu Dawood" -> "abu_dawood"
        "Sunan an-Nasa'i" -> "nasai"
        "Sunan Ibn Majah" -> "ibn_majah"
        else -> return null
    }

    return loadHadithCollection(context, key)?.records?.firstOrNull { it.id == id }
}

@Composable
private fun rememberHadithRecord(context: Context, selection: String): HadithRecord? {
    val state = androidx.compose.runtime.produceState<HadithRecord?>(initialValue = null, context, selection) {
        value = withContext(Dispatchers.IO) { loadHadithRecord(context, selection) }
    }
    return state.value
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchScreen(padding: PaddingValues, surahs: List<Surah>, ayahs: List<Ayah>, onOpenSurah: (Surah) -> Unit, onOpenHadith: (String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val hadithCollections by produceState<Map<String, HadithCollection>?>(null, query) {
        value = if (query.length >= 2) withContext(Dispatchers.IO) {
            hadithSources.mapNotNull { source -> loadHadithCollection(context, source.key)?.let { source.name to it } }.toMap()
        } else null
    }
    val matches = if (query.length < 2) emptyList() else ayahs.filter {
        (filter == "All" || filter == "Quran") &&
            (it.arabic.contains(query) || it.translation.contains(query, true) || it.tafseer.contains(query, true))
    }.take(50)
    val hadithMatches = if (query.length < 2 || (filter != "All" && filter != "Hadith")) emptyList()
    else hadithCollections?.flatMap { (name, collection) -> collection.records.filter { it.arabic.contains(query) || it.english.contains(query, true) || it.urdu.contains(query) }.take(20).map { name to it } } ?: emptyList()
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineMedium)
        Text("Quran, translations, tafseer, and Hadith", color = MaterialTheme.colorScheme.secondary)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 12.dp), label = { Text("Search offline content") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Quran", "Hadith").forEach { value -> FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) }) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = { it.id }) { ayah ->
                val surah = surahs.firstOrNull { it.id == ayah.surahId } ?: return@items
                Card(Modifier.fillMaxWidth().clickable { onOpenSurah(surah) }) {
                    Column(Modifier.padding(14.dp)) { Text("${surah.name_english} · Ayah ${ayah.ayahNumber}", color = MaterialTheme.colorScheme.primary); Text(ayah.arabic, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), fontSize = 20.sp); Text(ayah.translation, maxLines = 2) }
                }
            }
            items(hadithMatches, key = { "${it.first}|${it.second.id}" }) { (name, record) ->
                Card(Modifier.fillMaxWidth().clickable { onOpenHadith("$name|${record.id}") }) {
                    Column(Modifier.padding(14.dp)) {
                        Text("$name · Hadith ${record.number}", color = MaterialTheme.colorScheme.primary)
                        Text(record.english.ifBlank { record.urdu }.trim(), maxLines = 3)
                    }
                }
            }
        }
    }
}

@Composable
private fun TafseerScreen(
    padding: PaddingValues,
    surahs: List<Surah>,
    ayahs: List<Ayah>,
    onOpenSurah: (Surah) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val records = ayahs.filter {
        it.tafseer.isNotBlank() && (query.isBlank() || it.tafseer.contains(query) || it.translation.contains(query, true))
    }.take(100)
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Text("Tafseer library", style = MaterialTheme.typography.headlineMedium)
        Text("Tafseer Maududi · Urdu · Ayah-linked commentary", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 14.dp), label = { Text("Search commentary") }, singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(records, key = { it.id }) { ayah ->
                val surah = surahs.firstOrNull { it.id == ayah.surahId }
                Column(Modifier.fillMaxWidth().clickable { surah?.let(onOpenSurah) }.padding(vertical = 14.dp)) {
                    Text("${surah?.name_english ?: "Surah"} · Ayah ${ayah.ayahNumber}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                    Text(ayah.tafseer.trim(), maxLines = 5, modifier = Modifier.padding(top = 8.dp), lineHeight = 24.sp)
                    Divider(Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun JuzIndex(padding: PaddingValues, ayahs: List<Ayah>, surahs: List<Surah>, onOpenJuz: (Int) -> Unit) {
    val starts = (1..30).mapNotNull { juz ->
        ayahs.filter { it.juz == juz }.minWithOrNull(compareBy<Ayah> { it.id })?.let { juz to it }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp)) {
        item { Text("Juz index", style = MaterialTheme.typography.headlineMedium); Text("Verified starts from the bundled Quran dataset", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) }
        items(starts, key = { it.first }) { (juz, ayah) ->
            val surah = surahs.firstOrNull { it.id == ayah.surahId }
            Column(Modifier.fillMaxWidth().clickable { onOpenJuz(juz) }.padding(vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Juz $juz", style = MaterialTheme.typography.titleLarge); Text("${surah?.name_english ?: "Surah"} ${ayah.ayahNumber}", color = MaterialTheme.colorScheme.secondary) }
                Text("Starts at ${surah?.name_arabic ?: ""} · Ayah ${ayah.ayahNumber}", color = MaterialTheme.colorScheme.secondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onOpenJuz(juz) }) {
                        Text("Listen", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Divider(Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun LibraryScreen(padding: PaddingValues, surahs: List<Surah>, ayahs: List<Ayah>, bookmarks: Set<String>, hadithBookmarks: Set<String>, onOpenSurah: (Surah) -> Unit, onOpenHadith: (String) -> Unit, onDestination: (Destination) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var note by rememberSaveable { mutableStateOf(prefs.getString(NOTES, "") ?: "") }
    var editing by rememberSaveable { mutableStateOf(false) }
    val quranSaved = bookmarks.mapNotNull { key ->
        val parts = key.split(":")
        val surah = parts.getOrNull(0)?.toIntOrNull()?.let { id -> surahs.firstOrNull { it.id == id } }
        val ayah = parts.getOrNull(1)?.toIntOrNull()?.let { n -> surah?.let { s -> ayahs.firstOrNull { it.surahId == s.id && it.ayahNumber == n } } }
        if (surah != null && ayah != null) surah to ayah else null
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My Library", style = MaterialTheme.typography.headlineMedium); Text("Your saved reading, kept offline.", color = MaterialTheme.colorScheme.secondary) }
        item { Text("Continue reading", style = MaterialTheme.typography.titleLarge); TextButton(onClick = { onDestination(Destination.HOME) }) { Text("Open current position") } }
        item { Text("Saved Quran · ${quranSaved.size}", style = MaterialTheme.typography.titleLarge) }
        items(quranSaved, key = { "${it.first.id}:${it.second.ayahNumber}" }) { (surah, ayah) -> Text("${surah.name_english} · Ayah ${ayah.ayahNumber}", Modifier.fillMaxWidth().clickable { onOpenSurah(surah) }.padding(vertical = 8.dp)) }
        item { Text("Saved Hadith · ${hadithBookmarks.size}", style = MaterialTheme.typography.titleLarge) }
        items(hadithBookmarks.toList(), key = { it }) { key -> Text(key, Modifier.fillMaxWidth().clickable { onOpenHadith(key) }.padding(vertical = 8.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Personal note", style = MaterialTheme.typography.titleLarge); TextButton(onClick = { editing = !editing }) { Text(if (editing) "Close" else "Edit") } }
            if (editing) {
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Write a private note") })
                Row { Button(onClick = { prefs.edit { putString(NOTES, note) }; editing = false }) { Text("Save") }; TextButton(onClick = { note = ""; prefs.edit { remove(NOTES) } }) { Text("Delete") } }
            } else if (note.isBlank()) Text("No personal notes yet.", color = MaterialTheme.colorScheme.secondary) else Text(note, maxLines = 5)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(padding: PaddingValues, themeMode: String, arabicSize: Int, translationSize: Int, lineSpacing: Int, onSettings: (String, Int, Int, Int, Int) -> Unit) {
    val context = LocalContext.current
    var goal by remember { mutableStateOf(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(DAILY_GOAL, 5)) }
    var mode by remember(themeMode) { mutableStateOf(themeMode) }
    var arabic by remember(arabicSize) { mutableStateOf(arabicSize) }
    var translation by remember(translationSize) { mutableStateOf(translationSize) }
    var spacing by remember(lineSpacing) { mutableStateOf(lineSpacing) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Appearance", style = MaterialTheme.typography.titleLarge); listOf("light" to "Light", "dark" to "Dark", "system" to "Follow system").forEach { (value, label) -> Row(Modifier.fillMaxWidth().clickable { mode = value; onSettings(mode, arabic, translation, spacing, goal) }, verticalAlignment = Alignment.CenterVertically) { RadioButton(mode == value, { mode = value; onSettings(mode, arabic, translation, spacing, goal) }); Text(label) } } }
        item { Text("Reading", style = MaterialTheme.typography.titleLarge); Row(verticalAlignment = Alignment.CenterVertically) { Text("Arabic ${arabic}sp"); TextButton(onClick = { arabic = (arabic - 2).coerceAtLeast(20); onSettings(mode, arabic, translation, spacing, goal) }) { Text("A-") }; TextButton(onClick = { arabic = (arabic + 2).coerceAtMost(42); onSettings(mode, arabic, translation, spacing, goal) }) { Text("A+") } }; Row(verticalAlignment = Alignment.CenterVertically) { Text("Translation ${translation}sp"); TextButton(onClick = { translation = (translation - 1).coerceAtLeast(13); onSettings(mode, arabic, translation, spacing, goal) }) { Text("-") }; TextButton(onClick = { translation += 1; onSettings(mode, arabic, translation, spacing, goal) }) { Text("+") } }; Row(verticalAlignment = Alignment.CenterVertically) { Text("Line spacing ${spacing}sp"); TextButton(onClick = { spacing = (spacing - 2).coerceAtLeast(22); onSettings(mode, arabic, translation, spacing, goal) }) { Text("-") }; TextButton(onClick = { spacing += 2; onSettings(mode, arabic, translation, spacing, goal) }) { Text("+") } } }
        item { Text("Daily goal", style = MaterialTheme.typography.titleLarge); Text("$goal ayahs per day", color = MaterialTheme.colorScheme.secondary); Row { TextButton(onClick = { goal = (goal - 1).coerceAtLeast(1); onSettings(mode, arabic, translation, spacing, goal) }) { Text("-") }; TextButton(onClick = { goal += 1; onSettings(mode, arabic, translation, spacing, goal) }) { Text("+") } } }
        item { Text("Offline content", style = MaterialTheme.typography.titleLarge); Text("Quran Arabic, Urdu translation, and mapped tafseer are bundled from the verified local dataset.", color = MaterialTheme.colorScheme.secondary) }
        item { Text("Privacy", style = MaterialTheme.typography.titleLarge); Text("Bookmarks, notes, progress, and preferences stay in local app storage.", color = MaterialTheme.colorScheme.secondary) }
        item { Text("Noor Al-Deen · Version 1.0", color = MaterialTheme.colorScheme.secondary) }
    }
}

private fun lightColors() = androidx.compose.material3.lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF101827),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFF6F0E5),
    secondary = androidx.compose.ui.graphics.Color(0xFFD6B46A),
    background = androidx.compose.ui.graphics.Color(0xFFF6F0E5),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFCF6),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE4DCCE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF26352E),
    onSurface = androidx.compose.ui.graphics.Color(0xFF26352E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF77766D),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFE4DCCE)
)

private fun darkColors() = androidx.compose.material3.darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF101827),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFF6F0E5),
    secondary = androidx.compose.ui.graphics.Color(0xFFD6B46A),
    background = androidx.compose.ui.graphics.Color(0xFF101827),
    surface = androidx.compose.ui.graphics.Color(0xFF17243A),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1D2A40),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF6F0E5),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF6F0E5),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFAEB8C5),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF354258)
)

private val NoorTypography = androidx.compose.material3.Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium)
    )
}
