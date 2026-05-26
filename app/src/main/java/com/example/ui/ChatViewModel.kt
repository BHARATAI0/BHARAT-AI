package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _activeThreadId = MutableStateFlow<Int>(-1)
    val activeThreadId: StateFlow<Int> = _activeThreadId.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedEngineMode = MutableStateFlow("BHARAT_GPT")
    val selectedEngineMode: StateFlow<String> = _selectedEngineMode.asStateFlow()

    // Retrieve all threads reactively from Room
    val allThreads: StateFlow<List<ChatThread>> = repository.allThreads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()

    // Keep track of any active API warning (e.g., missing API key)
    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    init {
        // Observe changes in the active thread ID to load its respective messages reactively
        viewModelScope.launch {
            _activeThreadId.collect { threadId ->
                if (threadId != -1) {
                    repository.getMessages(threadId).collect { msgList ->
                        _activeMessages.value = msgList
                    }
                } else {
                    _activeMessages.value = emptyList()
                }
            }
        }

        // Auto-select latest thread if available when threads list gets updated initially
        viewModelScope.launch {
            allThreads.collect { threads ->
                if (_activeThreadId.value == -1 && threads.isNotEmpty()) {
                    _activeThreadId.value = threads.first().id
                    _selectedEngineMode.value = threads.first().engineMode
                }
            }
        }
    }

    fun selectThread(threadId: Int) {
        _activeThreadId.value = threadId
        // Sync engine mode with the thread's configured engine mode
        val thread = allThreads.value.find { it.id == threadId }
        thread?.let {
            _selectedEngineMode.value = it.engineMode
        }
        _apiError.value = null
    }

    fun createNewThread() {
        _activeThreadId.value = -1
        _activeMessages.value = emptyList()
        _apiError.value = null
    }

    fun deleteThread(threadId: Int) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
            if (_activeThreadId.value == threadId) {
                val remaining = allThreads.value.filter { it.id != threadId }
                if (remaining.isNotEmpty()) {
                    _activeThreadId.value = remaining.first().id
                } else {
                    _activeThreadId.value = -1
                    _activeMessages.value = emptyList()
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun updateEngineMode(mode: String) {
        _selectedEngineMode.value = mode
    }

    fun sendMessage() {
        val messageText = _inputText.value.trim()
        if (messageText.isEmpty() || _isLoading.value) return

        _inputText.value = ""
        _apiError.value = null

        viewModelScope.launch {
            var threadId = _activeThreadId.value

            // 1. If we are on a new thread, create it first in SQLite
            if (threadId == -1) {
                // Generate a friendly dynamic thread title from user prompt
                val cleanTitle = if (messageText.length > 28) {
                    messageText.substring(0, 26) + "..."
                } else {
                    messageText
                }
                threadId = repository.createThread(cleanTitle, _selectedEngineMode.value)
                _activeThreadId.value = threadId
            }

            // 2. Save the user input message
            repository.saveMessage(threadId, "user", messageText)

            // 3. Trigger remote API call under loading wrapper
            _isLoading.value = true
            val aiResponse = repository.generateAnswer(threadId, messageText, _selectedEngineMode.value)
            _isLoading.value = false

            // 4. Handle errors vs successful replies
            if (aiResponse.startsWith("APIKeyError:")) {
                _apiError.value = aiResponse.removePrefix("APIKeyError:")
            } else {
                repository.saveMessage(threadId, "model", aiResponse)
            }
        }
    }

    // Direct interface helper for prepopulated Indian template queries
    fun sendSuggestedPrompt(promptText: String) {
        updateInputText(promptText)
        sendMessage()
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
