package ir.hooshamooz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hooshamooz.app.data.LocalLearningRepository
import ir.hooshamooz.app.data.LearningContent
import ir.hooshamooz.app.ui.HooshAmoozApp

class MainActivity : ComponentActivity() {
    private var learning: LearningViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(18, 20, 45)),
        )
        val repository = LocalLearningRepository(applicationContext)
        val content = LearningContent.load(applicationContext)
        setContent {
            val model: LearningViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LearningViewModel(repository, content) as T
            })
            learning = model
            val state by model.state.collectAsStateWithLifecycle()
            HooshAmoozApp(model, state, onExit = { finishAndRemoveTask() })
        }
    }
    override fun onResume() { super.onResume(); learning?.refreshDay() }
}
