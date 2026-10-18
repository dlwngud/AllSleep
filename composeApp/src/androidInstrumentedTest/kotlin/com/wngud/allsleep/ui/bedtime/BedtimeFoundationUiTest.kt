package com.wngud.allsleep.ui.bedtime

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.wngud.allsleep.data.repository.LocalBedtimeRepositoryImpl
import com.wngud.allsleep.domain.model.bedtime.*
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.ui.components.BedtimeNavigationBar
import com.wngud.allsleep.ui.theme.AllSleepTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Before
import android.os.Build
import android.view.WindowManager
import org.junit.Rule
import org.junit.Test
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail

class BedtimeFoundationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun prepareVisibleActivity() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            if (Build.VERSION.SDK_INT >= 27) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private val sample = LocalBedtimeUiState(isLoading = false, owner = LocalOwner("guest:synthetic"), routines = listOf(BedtimeRoutine(RoutineKind.WEEKDAY, "23:30", "07:00"), BedtimeRoutine(RoutineKind.WEEKEND, "00:00", "08:00")))

    @Suppress("DEPRECATION")
    @Test fun installedAppDoesNotContainAdMobSdkOrInitializationMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
        assertFalse(info.metaData?.containsKey("com.google.android.gms.ads.APPLICATION_ID") == true)
        try {
            Class.forName("com.google.android.gms.ads.MobileAds", false, context.classLoader)
            fail("AdMob SDK must not be packaged")
        } catch (_: ClassNotFoundException) {
            // 실제 설치 APK에 광고 초기화 클래스가 없는 정상 결과.
        }
    }

    @Test fun fourTabsAndGuestLoginActionWorkWithoutAccount() {
        var route by mutableStateOf(Screen.Home.route)
        var loginClicks = 0
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        compose.setContent {
            AllSleepTheme {
                Scaffold(bottomBar = { BedtimeNavigationBar(route, { route = it }) }) { padding ->
                    val tab = when (route) { Screen.Alarm.route -> BedtimeTab.ROUTINES; Screen.Stats.route -> BedtimeTab.RECORDS; Screen.Settings.route -> BedtimeTab.MY; else -> BedtimeTab.TODAY }
                    LocalBedtimeContent(tab, sample, padding, { route = Screen.Alarm.route }, { loginClicks++ }, {}, {}, Modifier)
                }
            }
        }
        compose.onNodeWithText("내 약속 확인").performScrollTo().assertIsDisplayed()
        capture("today")
        compose.onNodeWithText("루틴").performClick()
        compose.onNodeWithText("평일 약속 편집").performScrollTo().assertIsDisplayed()
        capture("routines")
        compose.onNodeWithText("기록").performClick()
        compose.onNodeWithText("첫 약속부터 시작해 볼까요?").assertIsDisplayed()
        capture("records")
        compose.onNodeWithText("마이").performClick()
        compose.onNodeWithText("계정 없이 사용 중").assertIsDisplayed()
        assertEquals(0, loginClicks)
        capture("my")
        compose.onNodeWithText("로그인").performScrollTo().performClick()
        assertEquals(1, loginClicks)
    }

    @Test fun compactWidthAndLargeFontKeepRoutineActionReachable() {
        var selected: RoutineKind? = null
        compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                AllSleepTheme {
                    Scaffold(Modifier.width(360.dp), bottomBar = { BedtimeNavigationBar(Screen.Alarm.route, {}) }) { padding ->
                        LocalBedtimeContent(BedtimeTab.ROUTINES, sample, padding, {}, {}, { selected = it }, {})
                    }
                }
            }
        }
        compose.onNodeWithText("주말 약속 편집").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(RoutineKind.WEEKEND, selected)
        capture("routines-360-large-font")
    }

    @Test fun nativeEditorWritesAndReopensLocalSettingsWithoutLogin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "bedtime-ui-${System.nanoTime()}.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val repository = LocalBedtimeRepositoryImpl(store) { "guest:device-test" }
        lateinit var vm: LocalBedtimeViewModel
        compose.runOnUiThread { vm = LocalBedtimeViewModel(repository) }
        try {
            compose.setContent { AllSleepTheme { Scaffold { padding -> LocalBedtimeScreen(BedtimeTab.ROUTINES, padding, {}, {}, viewModel = vm) } } }
            compose.waitUntil(10_000) { !vm.state.value.isLoading && vm.state.value.routines.size == 2 }
            compose.onNodeWithText("주말 약속 편집").performScrollTo().performClick()
            compose.onNodeWithText("사용 제한 시작").performTextReplacement("00:45")
            compose.onNodeWithText("해제 시각").performTextReplacement("08:15")
            compose.onNodeWithText("약속 저장").assertIsDisplayed()
            capture("routine-editor-keyboard")
            compose.onNodeWithText("약속 저장").performClick()
            compose.waitUntil(10_000) { vm.state.value.routines.last().bedtime == "00:45" && !vm.state.value.isSaving }
            compose.onNodeWithText("00:45 → 08:15").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { vm.viewModelScope.cancel() }
            runBlocking { job.cancelAndJoin() }
            val reopenedJob = SupervisorJob()
            val reopenedStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO), produceFile = { file })
            try {
                runBlocking {
                    val reopened = LocalBedtimeRepositoryImpl(reopenedStore) { error("Must retain owner") }
                    assertEquals(LocalOwner("guest:device-test"), reopened.initialize().getOrThrow())
                    assertEquals("00:45", reopened.routines.first().last().bedtime)
                    assertEquals("23:00", reopened.routines.first().first().bedtime)
                }
            } finally { runBlocking { reopenedJob.cancelAndJoin() } }
        } finally { vm.viewModelScope.cancel(); runBlocking { job.cancelAndJoin() }; file.delete() }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "bedtime-foundation-screens").apply { mkdirs() }
        val node = if (name == "routine-editor-keyboard") compose.onNode(isDialog()) else compose.onRoot()
        File(directory, "$name.png").outputStream().use { node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
