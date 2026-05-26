package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = ChatRepository(database.chatDao())
        
        // Instantiate the ChatViewModel with Factory
        val viewModel: ChatViewModel by viewModels {
            ChatViewModelFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
