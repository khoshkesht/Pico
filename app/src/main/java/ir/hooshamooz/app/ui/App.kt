package ir.hooshamooz.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun HooshAmoozApp(model: LearningViewModel, state: AppState, onExit: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                model.refreshDay()
                delay(30_000)
            }
        }
    }
    SpaceTheme(state.data.textScale) {
        var confirmingExit by rememberSaveable { mutableStateOf(false) }
        BackHandler {
            if (state.screen == Screen.HOME) confirmingExit = true else model.navigate(Screen.HOME)
        }
        Scaffold(containerColor = Space, bottomBar = {
            if (state.screen in listOf(Screen.HOME, Screen.PATH, Screen.SETTINGS)) {
                NavigationBar(containerColor = Panel) {
                    listOf(Screen.HOME to "خانه", Screen.PATH to "مسیر", Screen.SETTINGS to "تنظیمات").forEach { (screen, title) ->
                        NavigationBarItem(selected = state.screen == screen, onClick = { model.navigate(screen) },
                            icon = { NavigationIcon(screen, state.screen == screen) },
                            label = { Text(title, fontWeight = if (state.screen == screen) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Violet.copy(alpha = .2f), selectedIconColor = Violet, selectedTextColor = Violet))
                    }
                    NavigationBarItem(selected = false, onClick = { confirmingExit = true },
                        icon = { ExitIcon() }, label = { Text("خروج") })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                StarField(Modifier.matchParentSize())
                if (state.screen in listOf(Screen.ACTIVITY, Screen.HOME, Screen.PATH)) {
                    val background = if (state.screen == Screen.ACTIVITY) {
                        val chapterId = model.activeRun()?.let { model.engine.current(it).chapterId }
                        model.content.chapterBackgrounds[chapterId] ?: "METEORS"
                    } else model.journeyBackground()
                    when (background) {
                        "ORBITAL_LINKS" -> OrbitalLinks(Modifier.matchParentSize())
                        "SIGNAL_WAVES" -> SignalWaves(Modifier.matchParentSize())
                        else -> MeteorField(Modifier.matchParentSize())
                    }
                }
                when (state.screen) {
                    Screen.HOME -> HomeScreen(model, state)
                    Screen.PATH -> PathScreen(model, state)
                    Screen.SETTINGS -> SettingsScreen(model, state)
                    Screen.ACTIVITY -> ActivityScreen(model, state)
                    Screen.RESULT -> ResultScreen(model, state)
                }
            }
        }
        if (confirmingExit) AlertDialog(onDismissRequest = { confirmingExit = false },
            title = { Text("از پیکو خارج می‌شی؟") },
            confirmButton = { TextButton(onClick = { confirmingExit = false; onExit() }) { Text("خروج") } },
            dismissButton = { TextButton(onClick = { confirmingExit = false }) { Text("انصراف") } })
    }
}

@Composable
fun SpaceCard(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(20.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(
        Brush.linearGradient(listOf(Panel, Panel.copy(alpha = .85f))), RoundedCornerShape(24.dp))
        .border(1.dp, Violet.copy(alpha = .20f), RoundedCornerShape(24.dp)).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Space)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScreenColumn(title: String? = null, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (title != null || onBack != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (title != null) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (onBack != null) FilledTonalButton(onClick = onBack,
                modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Violet.copy(alpha = .65f)),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Violet.copy(alpha = .18f), contentColor = Ink),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                NavigationIcon(Screen.HOME, true, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("خانه", fontWeight = FontWeight.Bold)
            }
        }
        content()
        Spacer(Modifier.height(12.dp))
    }
}
