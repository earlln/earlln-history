package com.earlln.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.earlln.history.data.HistoryRepository
import com.earlln.history.ui.EarllnApp
import com.earlln.history.ui.EarllnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = HistoryRepository.get(applicationContext)
        setContent {
            EarllnTheme {
                EarllnApp(repo)
            }
        }
    }
}
