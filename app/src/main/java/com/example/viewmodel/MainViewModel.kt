package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    WELCOME,
    CAKE_SELECTION,
    FROSTING_SELECTION,
    SPATULA_SELECTION,
    DECORATING,
    FINISH_CAPTURE,
    GUESTBOOK
}

data class PaintStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float,
    val isDaisy: Boolean = false
)

data class PlacedTopping(
    val id: String,
    val name: String,
    val offset: Offset
)

data class AppUiState(
    val currentScreen: AppScreen = AppScreen.WELCOME,
    val activeGuest: DailyGuest = GuestManager.getGuestForDay(System.currentTimeMillis()),
    val isGuestCompletedToday: Boolean = false,
    val isFreePlayMode: Boolean = false,
    val streakCount: Int = 0,
    val selectedBase: CakeBase = DecorationOptions.cakeBases[0],
    val selectedFlavor: FrostingFlavor = DecorationOptions.frostingFlavors[0],
    val selectedSpatula: Spatula = DecorationOptions.spatulas[2], // default No. 3
    val selectedTopping: Topping? = null, // null means paint brush, not null means paint is disabled to place topping
    val strokes: List<PaintStroke> = emptyList(),
    val redoStrokes: List<PaintStroke> = emptyList(),
    val placedToppings: List<PlacedTopping> = emptyList(),
    val unlockedCollections: Set<String> = emptySet(),
    val isPurchaseDialogOpen: Boolean = false,
    val purchaseDialogCollection: String? = null,
    val feedbackMessage: String? = null,
    val lastCakeImagePath: String? = null,
    val lastSavedEntryId: Long? = null,
    val customGreetingText: String = "",
    val returningDialogue: String? = null,
    val lastEntryForGuest: GuestbookEntry? = null,
    val isTutorialActive: Boolean = false,
    val isTutorialCompleted: Boolean = false,
    val tutorialStep: Int = 0, // 0: welcome/start, 1: base selection, 2: frosting, 3: spatula, 4: decorating, 5: finish
    val strokeSensitivity: Float = 1.0f,
    val isDailyReminderEnabled: Boolean = false
)

class MainViewModel(
    application: Application,
    private val repository: GuestbookRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bloom_crumb_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val guestbookEntries = repository.allEntries

    init {
        loadUserState()
        checkTodayCompletion()
    }

    private fun loadUserState() {
        val streak = prefs.getInt("streak_count", 0)
        val unlockedList = prefs.getStringSet("unlocked_collections", emptySet()) ?: emptySet()
        val lastVisit = prefs.getString("last_visit_date", "") ?: ""
        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)
        val strokeSens = prefs.getFloat("stroke_sensitivity", 1.0f)
        val remindersEnabled = prefs.getBoolean("daily_reminder_enabled", false)

        // Streak breaking check: if last visit was not today and not yesterday, clear steak
        var finalStreak = streak
        if (lastVisit.isNotEmpty()) {
            if (lastVisit != getCurrentDayString() && !isYesterday(lastVisit)) {
                finalStreak = 0 // reset gracefully without punishment
            }
        }

        _uiState.update {
            it.copy(
                streakCount = finalStreak,
                unlockedCollections = unlockedList,
                isTutorialCompleted = tutorialCompleted,
                isTutorialActive = !tutorialCompleted, // start with tutorial if not completed
                activeGuest = if (!tutorialCompleted) GuestManager.tutorialGuest else GuestManager.getGuestForDay(System.currentTimeMillis()),
                strokeSensitivity = strokeSens,
                isDailyReminderEnabled = remindersEnabled
            )
        }
    }

    private fun checkTodayCompletion() {
        viewModelScope.launch {
            val todayStr = getCurrentDayString()
            repository.allEntries.collect { entries ->
                val dayCompleted = entries.any { entry ->
                    val entryDate = Date(entry.dateMs)
                    val sdf = SimpleDateFormat("yyyy-M-d", Locale.getDefault())
                    sdf.format(entryDate) == todayStr && entry.story.isNotEmpty() // completed daily guest
                }
                
                // Also check relationship memory for today's guest
                val activeGuest = _uiState.value.activeGuest
                val lastEntry = repository.getLastEntryForGuest(activeGuest.name)
                val dialogue = if (lastEntry != null) {
                    activeGuest.recurringDialogue
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        isGuestCompletedToday = dayCompleted,
                        returningDialogue = dialogue,
                        lastEntryForGuest = lastEntry
                    )
                }
            }
        }
    }

    fun startDailyCake() {
        val todayGuest = GuestManager.getGuestForDay(System.currentTimeMillis())
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.CAKE_SELECTION,
                activeGuest = todayGuest,
                isFreePlayMode = false,
                strokes = emptyList(),
                redoStrokes = emptyList(),
                placedToppings = emptyList(),
                selectedTopping = null,
                customGreetingText = "Made with love for ${todayGuest.name}"
            )
        }
    }

    fun startFreePlay() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.CAKE_SELECTION,
                isFreePlayMode = true,
                strokes = emptyList(),
                redoStrokes = emptyList(),
                placedToppings = emptyList(),
                selectedTopping = null,
                customGreetingText = "Sweet freeform creation"
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun selectCakeBase(base: CakeBase) {
        if (base.isLimited && !isCollectionUnlocked(base.limitedCollectionName)) {
            triggerPurchaseFlow(base.limitedCollectionName!!)
            return
        }
        val nextStep = if (_uiState.value.isTutorialActive && _uiState.value.tutorialStep == 1) 2 else _uiState.value.tutorialStep
        _uiState.update { it.copy(selectedBase = base, tutorialStep = nextStep) }
        navigateTo(AppScreen.FROSTING_SELECTION)
    }

    fun selectFrostingFlavor(flavor: FrostingFlavor) {
        if (flavor.isLimited && !isCollectionUnlocked(flavor.limitedCollectionName)) {
            triggerPurchaseFlow(flavor.limitedCollectionName!!)
            return
        }
        val nextStep = if (_uiState.value.isTutorialActive && _uiState.value.tutorialStep == 2) 3 else _uiState.value.tutorialStep
        _uiState.update { it.copy(selectedFlavor = flavor, tutorialStep = nextStep) }
        navigateTo(AppScreen.SPATULA_SELECTION)
    }

    fun selectSpatula(spatula: Spatula) {
        val nextStep = if (_uiState.value.isTutorialActive && _uiState.value.tutorialStep == 3) 4 else _uiState.value.tutorialStep
        _uiState.update { it.copy(selectedSpatula = spatula, tutorialStep = nextStep) }
        navigateTo(AppScreen.DECORATING)
    }

    fun updateSelectedTopping(topping: Topping?) {
        _uiState.update { it.copy(selectedTopping = topping) }
    }

    fun addStroke(stroke: PaintStroke) {
        val currentStrokes = _uiState.value.strokes.toMutableList()
        currentStrokes.add(stroke)
        _uiState.update {
            it.copy(
                strokes = currentStrokes,
                redoStrokes = emptyList() // clear redo on new action
            )
        }
        saveIncompleteCake()
    }

    fun undo() {
        val currentStrokes = _uiState.value.strokes
        if (currentStrokes.isNotEmpty()) {
            val last = currentStrokes.last()
            val newStrokes = currentStrokes.dropLast(1)
            val redos = _uiState.value.redoStrokes.toMutableList()
            redos.add(last)
            _uiState.update {
                it.copy(
                    strokes = newStrokes,
                    redoStrokes = redos
                )
            }
            saveIncompleteCake()
        }
    }

    fun redo() {
        val redos = _uiState.value.redoStrokes
        if (redos.isNotEmpty()) {
            val last = redos.last()
            val newRedos = redos.dropLast(1)
            val currentStrokes = _uiState.value.strokes.toMutableList()
            currentStrokes.add(last)
            _uiState.update {
                it.copy(
                    strokes = currentStrokes,
                    redoStrokes = newRedos
                )
            }
            saveIncompleteCake()
        }
    }

    fun clearCanvas() {
        _uiState.update {
            it.copy(
                strokes = emptyList(),
                redoStrokes = emptyList(),
                placedToppings = emptyList()
            )
        }
        saveIncompleteCake()
    }

    fun placeTopping(id: String, name: String, offset: Offset) {
        val currentToppings = _uiState.value.placedToppings.toMutableList()
        currentToppings.add(PlacedTopping(id, name, offset))
        _uiState.update {
            it.copy(placedToppings = currentToppings)
        }
        saveIncompleteCake()
    }

    fun removeLastTopping() {
        val currentToppings = _uiState.value.placedToppings
        if (currentToppings.isNotEmpty()) {
            _uiState.update {
                it.copy(placedToppings = currentToppings.dropLast(1))
            }
            saveIncompleteCake()
        }
    }

    // Purchase Dialog Actions
    fun triggerPurchaseFlow(collectionName: String) {
        _uiState.update {
            it.copy(
                isPurchaseDialogOpen = true,
                purchaseDialogCollection = collectionName
            )
        }
    }

    fun dismissPurchaseDialog() {
        _uiState.update {
            it.copy(
                isPurchaseDialogOpen = false,
                purchaseDialogCollection = null
            )
        }
    }

    fun confirmPurchase() {
        val collection = _uiState.value.purchaseDialogCollection ?: return
        val currentSet = _uiState.value.unlockedCollections.toMutableSet()
        currentSet.add(collection)

        prefs.edit().putStringSet("unlocked_collections", currentSet).apply()

        _uiState.update {
            it.copy(
                unlockedCollections = currentSet,
                isPurchaseDialogOpen = false,
                purchaseDialogCollection = null,
                feedbackMessage = "Unlocked \"$collection Collection\" successfully! Enjoy your new ingredients."
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun updateGreeting(text: String) {
        _uiState.update { it.copy(customGreetingText = text) }
    }

    // Save final decorated artwork to file database and SQLite
    fun finishBaking(bitmap: Bitmap) {
        viewModelScope.launch {
            val filename = "cake_${System.currentTimeMillis()}.png"
            val file = File(getApplication<Application>().cacheDir, filename)
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                out?.close()
            }

            val savedPath = file.absolutePath
            val relation = if (_uiState.value.isFreePlayMode) "Myself" else _uiState.value.activeGuest.relationshipType
            val guestName = if (_uiState.value.isFreePlayMode) "Free Play Creator" else _uiState.value.activeGuest.name
            val storyText = if (_uiState.value.isFreePlayMode) "" else _uiState.value.activeGuest.story

            val newEntry = GuestbookEntry(
                guestName = guestName,
                story = storyText,
                relationshipType = relation,
                cakeBaseId = _uiState.value.selectedBase.id,
                frostingFlavorId = _uiState.value.selectedFlavor.id,
                dateMs = System.currentTimeMillis(),
                imagePath = savedPath,
                note = _uiState.value.customGreetingText
            )

            val newId = repository.insertEntry(newEntry)

            // Update streaks (only for the daily guest completion!)
            if (!_uiState.value.isFreePlayMode) {
                updateStreakInfo()
            }

            clearSavedIncompleteCake()

            val nextStep = if (_uiState.value.isTutorialActive) 5 else _uiState.value.tutorialStep
            _uiState.update {
                it.copy(
                    lastCakeImagePath = savedPath,
                    lastSavedEntryId = newId,
                    currentScreen = AppScreen.FINISH_CAPTURE,
                    tutorialStep = nextStep
                )
            }
        }
    }

    private fun updateStreakInfo() {
        val todayStr = getCurrentDayString()
        val lastVisit = prefs.getString("last_visit_date", "") ?: ""
        val currentStreak = prefs.getInt("streak_count", 0)

        val newStreak = when {
            lastVisit == todayStr -> {
                // Completed another cake today, keep same streak
                currentStreak
            }
            lastVisit.isEmpty() -> {
                1
            }
            isYesterday(lastVisit) -> {
                currentStreak + 1
            }
            else -> {
                // Streak broken, restart at 1
                1
            }
        }

        prefs.edit()
            .putInt("streak_count", newStreak)
            .putString("last_visit_date", todayStr)
            .apply()

        _uiState.update {
            it.copy(streakCount = newStreak)
        }
    }

    fun isCollectionUnlocked(collectionName: String?): Boolean {
        if (collectionName == null) return true
        return _uiState.value.unlockedCollections.contains(collectionName)
    }

    // Helper functions for date checks
    fun getCurrentDayString(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    private fun isYesterday(dayStr: String): Boolean {
        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yYear = yesterdayCal.get(Calendar.YEAR)
        val yMonth = yesterdayCal.get(Calendar.MONTH) + 1
        val yDay = yesterdayCal.get(Calendar.DAY_OF_MONTH)
        return dayStr == "$yYear-$yMonth-$yDay"
    }

    fun deleteEntry(entry: GuestbookEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            // also delete cache file
            try {
                val file = File(entry.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStrokeSensitivity(sensor: Float) {
        prefs.edit().putFloat("stroke_sensitivity", sensor).apply()
        _uiState.update { it.copy(strokeSensitivity = sensor) }
    }

    fun updateDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        _uiState.update { it.copy(isDailyReminderEnabled = enabled) }
    }

    fun startTutorial() {
        _uiState.update {
            it.copy(
                isTutorialActive = true,
                tutorialStep = 1, // step 1 is Cake base selection
                activeGuest = GuestManager.tutorialGuest,
                currentScreen = AppScreen.CAKE_SELECTION,
                strokes = emptyList(),
                redoStrokes = emptyList(),
                placedToppings = emptyList(),
                selectedTopping = null,
                customGreetingText = "My First Masterpiece!"
            )
        }
    }

    fun completeTutorialStep() {
        val currentStep = _uiState.value.tutorialStep
        if (currentStep < 5) {
            _uiState.update { it.copy(tutorialStep = currentStep + 1) }
        } else {
            // Completed Tutorial!
            prefs.edit().putBoolean("tutorial_completed", true).apply()
            _uiState.update {
                it.copy(
                    isTutorialActive = false,
                    isTutorialCompleted = true,
                    tutorialStep = 0,
                    activeGuest = GuestManager.getGuestForDay(System.currentTimeMillis()),
                    currentScreen = AppScreen.WELCOME
                )
            }
        }
    }

    fun forceSkipTutorial() {
        prefs.edit().putBoolean("tutorial_completed", true).apply()
        _uiState.update {
            it.copy(
                isTutorialActive = false,
                isTutorialCompleted = true,
                tutorialStep = 0,
                activeGuest = GuestManager.getGuestForDay(System.currentTimeMillis()),
                currentScreen = AppScreen.WELCOME
            )
        }
    }

    fun prefillGuestChoices(baseId: String, flavorId: String) {
        val selectedBase = DecorationOptions.cakeBases.find { it.id == baseId } ?: DecorationOptions.cakeBases[0]
        val selectedFlavor = DecorationOptions.frostingFlavors.find { it.id == flavorId } ?: DecorationOptions.frostingFlavors[0]
        _uiState.update {
            it.copy(
                selectedBase = selectedBase,
                selectedFlavor = selectedFlavor,
                selectedSpatula = DecorationOptions.spatulas[2], // Default No. 3
                strokes = emptyList(),
                redoStrokes = emptyList(),
                placedToppings = emptyList(),
                selectedTopping = null,
                currentScreen = AppScreen.DECORATING,
                customGreetingText = "Made with love for ${it.activeGuest.name}"
            )
        }
    }

    fun saveIncompleteCake() {
        val strokes = _uiState.value.strokes
        val toppings = _uiState.value.placedToppings
        val isFreePlay = _uiState.value.isFreePlayMode
        val baseId = _uiState.value.selectedBase.id
        val flavorId = _uiState.value.selectedFlavor.id
        val spatulaId = _uiState.value.selectedSpatula.id

        val strokesStr = strokes.joinToString(separator = "|||") { stroke ->
            val pointsStr = stroke.points.joinToString(separator = "|") { "${it.x},${it.y}" }
            "${stroke.isDaisy};${stroke.color.toArgb()};${stroke.strokeWidth};${stroke.alpha};$pointsStr"
        }

        val toppingsStr = toppings.joinToString(separator = "|||") { topping ->
            "${topping.id};${topping.name};${topping.offset.x},${topping.offset.y}"
        }

        prefs.edit()
            .putBoolean("unfinished_exists", true)
            .putBoolean("unfinished_freeplay", isFreePlay)
            .putString("unfinished_base", baseId)
            .putString("unfinished_flavor", flavorId)
            .putInt("unfinished_spatula", spatulaId)
            .putString("unfinished_strokes", strokesStr)
            .putString("unfinished_toppings", toppingsStr)
            .apply()
    }

    fun clearSavedIncompleteCake() {
        prefs.edit()
            .putBoolean("unfinished_exists", false)
            .remove("unfinished_freeplay")
            .remove("unfinished_base")
            .remove("unfinished_flavor")
            .remove("unfinished_spatula")
            .remove("unfinished_strokes")
            .remove("unfinished_toppings")
            .apply()
    }

    fun loadIncompleteCake(): Boolean {
        val exists = prefs.getBoolean("unfinished_exists", false)
        if (!exists) return false

        try {
            val isFreePlay = prefs.getBoolean("unfinished_freeplay", false)
            val baseId = prefs.getString("unfinished_base", "") ?: ""
            val flavorId = prefs.getString("unfinished_flavor", "") ?: ""
            val spatulaId = prefs.getInt("unfinished_spatula", 3)
            val strokesStr = prefs.getString("unfinished_strokes", "") ?: ""
            val toppingsStr = prefs.getString("unfinished_toppings", "") ?: ""

            val selectedBase = DecorationOptions.cakeBases.find { it.id == baseId } ?: DecorationOptions.cakeBases[0]
            val selectedFlavor = DecorationOptions.frostingFlavors.find { it.id == flavorId } ?: DecorationOptions.frostingFlavors[0]
            val selectedSpatula = DecorationOptions.spatulas.find { it.id == spatulaId } ?: DecorationOptions.spatulas[2]

            // Reconstruct strokes
            val strokes = if (strokesStr.isEmpty()) emptyList<PaintStroke>() else strokesStr.split("|||").mapNotNull { strokePart ->
                val tokens = strokePart.split(";")
                if (tokens.size >= 5) {
                    val isDaisy = tokens[0].toBoolean()
                    val colorArgb = tokens[1].toInt()
                    val strokeW = tokens[2].toFloat()
                    val alphaV = tokens[3].toFloat()
                    val pointsStr = tokens[4]
                    val points = if (pointsStr.isEmpty()) emptyList<Offset>() else pointsStr.split("|").mapNotNull { ptStr ->
                        val coords = ptStr.split(",")
                        if (coords.size == 2) {
                            Offset(coords[0].toFloat(), coords[1].toFloat())
                        } else null
                    }
                    PaintStroke(
                        points = points,
                        color = Color(colorArgb),
                        strokeWidth = strokeW,
                        alpha = alphaV,
                        isDaisy = isDaisy
                    )
                } else null
            }

            // Reconstruct toppings
            val toppings = if (toppingsStr.isEmpty()) emptyList<PlacedTopping>() else toppingsStr.split("|||").mapNotNull { topPart ->
                val tokens = topPart.split(";")
                if (tokens.size >= 3) {
                    val id = tokens[0]
                    val name = tokens[1]
                    val coords = tokens[2].split(",")
                    if (coords.size == 2) {
                        PlacedTopping(id, name, Offset(coords[0].toFloat(), coords[1].toFloat()))
                    } else null
                } else null
            }

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.DECORATING,
                    isFreePlayMode = isFreePlay,
                    selectedBase = selectedBase,
                    selectedFlavor = selectedFlavor,
                    selectedSpatula = selectedSpatula,
                    strokes = strokes,
                    placedToppings = toppings,
                    customGreetingText = if (isFreePlay) "Sweet freeform creation" else "Made with love for ${it.activeGuest.name}"
                )
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            clearSavedIncompleteCake()
            return false
        }
    }

    fun hasIncompleteCake(): Boolean {
        return prefs.getBoolean("unfinished_exists", false)
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: GuestbookRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
