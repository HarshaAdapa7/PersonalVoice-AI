package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.VoiceStudioScreen
import com.example.ui.VoiceStudioViewModel
import com.example.ui.theme.PersonalVoiceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalVoiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceStudioScreen(viewModel = viewModel)
                }
            }
        }
    }
}
