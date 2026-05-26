package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.ChatThread
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val allThreads by viewModel.allThreads.collectAsState()
    val activeThreadId by viewModel.activeThreadId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEngineMode by viewModel.selectedEngineMode.collectAsState()
    val apiError by viewModel.apiError.collectAsState()

    val lazyListState = rememberLazyListState()

    // Retrieve active thread title or fallback to "New Chat"
    val activeThread = allThreads.find { it.id == activeThreadId }
    val threadTitle = activeThread?.title ?: "New BharatAI Chat"

    // Automatically scroll to the end when new messages arrive
    LaunchedEffect(activeMessages.size, isLoading) {
        if (activeMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 2.dp
            ) {
                // Header of Drawer representing Indian Identity
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE65100), // Indian Ochre Saffron
                                    Color(0xFF1B5E20)  // Indian Deep Forest Green
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 32.dp)
                ) {
                    Column {
                        Text(
                            text = "BharatAI",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "India's Multi-Model Intelligence Studio",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // New Chat Button in Sidebar list
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Chat") },
                    label = { Text("Start New Conversation", fontWeight = FontWeight.SemiBold) },
                    selected = activeThreadId == -1,
                    onClick = {
                        viewModel.createNewThread()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("drawer_new_chat_button"),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                Text(
                    text = "RECORDS & THREADS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Bold
                )

                // Threads List
                if (allThreads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved chat history. Start typing to create a thread!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        items(allThreads, key = { it.id }) { thread ->
                            val isSelected = thread.id == activeThreadId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectThread(thread.id)
                                        scope.launch { drawerState.close() }
                                    }
                                    .testTag("thread_item_${thread.id}"),
                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (thread.engineMode) {
                                            "BHARAT_GPT" -> Icons.Default.Face
                                            "BHARAT_GEMINI" -> Icons.Default.Settings
                                            "BHARAT_SPEAK" -> Icons.Default.Search
                                            "BHARAT_CODE" -> Icons.Default.Edit
                                            else -> Icons.Default.Face
                                        },
                                        contentDescription = "Thread Mode",
                                        tint = when (thread.engineMode) {
                                            "BHARAT_GPT" -> Color(0xFFE65100)
                                            "BHARAT_GEMINI" -> Color(0xFF1976D2)
                                            "BHARAT_SPEAK" -> Color(0xFF4CAF50)
                                            "BHARAT_CODE" -> Color(0xFF9C27B0)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = thread.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = when (thread.engineMode) {
                                                "BHARAT_GPT" -> "BharatGPT mode"
                                                "BHARAT_GEMINI" -> "BharatGemini mode"
                                                "BHARAT_SPEAK" -> "BharatSpeak Translation"
                                                "BHARAT_CODE" -> "BharatCode engine"
                                                else -> "General Chat"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteThread(thread.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Thread",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer showing version
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BharatAI v1.1 • Made for India",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bharat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFFE65100) // Saffron Orange
                                )
                                Text(
                                    text = "AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF1B5E20) // Deep Forest Green
                                )
                            }
                            Text(
                                text = "India's Smart Assistant Workspace",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Saved Chats")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.createNewThread()
                            },
                            modifier = Modifier.testTag("action_new_chat")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Chats")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Personality Engine Selection Bar
                EngineSelectionBar(
                    selectedMode = selectedEngineMode,
                    onModeSelected = { viewModel.updateEngineMode(it) }
                )

                // Message Area or Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeMessages.isEmpty() && !isLoading) {
                        EmptyStatePromptPanel(
                            selectedMode = selectedEngineMode,
                            onSuggestionClicked = { viewModel.sendSuggestedPrompt(it) }
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                        ) {
                            items(activeMessages, key = { it.id }) { message ->
                                ChatBubbleItem(message = message)
                            }
                            if (isLoading) {
                                item {
                                    LoadingAssistantBubble(engineMode = selectedEngineMode)
                                }
                            }
                        }
                    }
                }

                // Error Warning Banner (e.g. key missing)
                AnimatedVisibility(visible = apiError != null) {
                    apiError?.let { errText ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = errText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Bottom Input Control Dashboard
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            placeholder = {
                                Text(
                                    text = when (selectedEngineMode) {
                                        "BHARAT_GPT" -> "Type a message to BharatGPT..."
                                        "BHARAT_GEMINI" -> "Ask BharatGemini for full details..."
                                        "BHARAT_SPEAK" -> "Input language phrase or sentence..."
                                        "BHARAT_CODE" -> "Write / fix coding question here..."
                                        else -> "Ask BharatAI anything..."
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("chat_input_field"),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            maxLines = 5,
                            shape = RoundedCornerShape(26.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val canSend = inputText.trim().isNotEmpty() && !isLoading
                        FloatingActionButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.sendMessage()
                            },
                            containerColor = if (canSend) {
                                when (selectedEngineMode) {
                                    "BHARAT_GPT" -> Color(0xFFE65100)
                                    "BHARAT_GEMINI" -> Color(0xFF1976D2)
                                    "BHARAT_SPEAK" -> Color(0xFF4CAF50)
                                    "BHARAT_CODE" -> Color(0xFF9C27B0)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Message",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EngineSelectionBar(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = when (selectedMode) {
            "BHARAT_GPT" -> 0
            "BHARAT_GEMINI" -> 1
            "BHARAT_SPEAK" -> 2
            "BHARAT_CODE" -> 3
            else -> 0
        },
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        divider = {},
        indicator = {}
    ) {
        val modesList = listOf(
            Triple("BHARAT_GPT", "BharatGPT", Color(0xFFE65100)),
            Triple("BHARAT_GEMINI", "BharatGemini", Color(0xFF1976D2)),
            Triple("BHARAT_SPEAK", "BharatSpeak", Color(0xFF4CAF50)),
            Triple("BHARAT_CODE", "BharatCode", Color(0xFF9C27B0))
        )

        modesList.forEach { (modeKey, modeLabel, modeColor) ->
            val isSelected = selectedMode == modeKey
            Tab(
                selected = isSelected,
                onClick = { onModeSelected(modeKey) },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("engine_tab_$modeKey")
            ) {
                Surface(
                    color = if (isSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (modeKey) {
                                "BHARAT_GPT" -> Icons.Default.Face
                                "BHARAT_GEMINI" -> Icons.Default.Settings
                                "BHARAT_SPEAK" -> Icons.Default.Search
                                "BHARAT_CODE" -> Icons.Default.Edit
                                else -> Icons.Default.Face
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePromptPanel(
    selectedMode: String,
    onSuggestionClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFE65100), Color(0xFF1B5E20))
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (selectedMode) {
                    "BHARAT_GPT" -> Icons.Default.Face
                    "BHARAT_GEMINI" -> Icons.Default.Settings
                    "BHARAT_SPEAK" -> Icons.Default.Search
                    "BHARAT_CODE" -> Icons.Default.Edit
                    else -> Icons.Default.Face
                },
                contentDescription = "BharatAI Flagship Emblem",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = when (selectedMode) {
                "BHARAT_GPT" -> "BharatGPT • Conversational Context"
                "BHARAT_GEMINI" -> "BharatGemini • Analytical Power"
                "BHARAT_SPEAK" -> "BharatSpeak • Translator & Coach"
                "BHARAT_CODE" -> "BharatCode • Developer Workbench"
                else -> "Welcome to BharatAI"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when (selectedMode) {
                "BHARAT_GPT" -> "India's culturally intelligent AI with deep regional warmth and languages."
                "BHARAT_GEMINI" -> "Detailed structured breakdowns, PM schemes, and logical deep dives."
                "BHARAT_SPEAK" -> "Helping your communication flow smoothly in Hindi, Hinglish, & English."
                "BHARAT_CODE" -> "Expert programming guide for crafting neat software, layouts, & scripts."
                else -> "Experience seamless interactions using tailored AI engines for India."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "TAP TO ASK QUICK SUGGESTION:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val templates = when (selectedMode) {
            "BHARAT_GPT" -> listOf(
                "Write a warm greeting celebrating the festive season in Hinglish.",
                "How do traditional Indian family structures contribute to modern entrepreneurial success?"
            )
            "BHARAT_GEMINI" -> listOf(
                "Give me a clear explanation & eligibility criteria of PM-Kisan Yojana.",
                "What are the target components of the Digital India Scheme?"
            )
            "BHARAT_SPEAK" -> listOf(
                "How can I politely ask in Marathi: 'Where is the nearest Indian post office'?",
                "Translate and transcribe: 'Have a nutritious breakfast with millets.'"
            )
            "BHARAT_CODE" -> listOf(
                "Write a Kotlin Compose Composable function for a beautiful dynamic button.",
                "How do you query database records reactively using Kotlin Coroutines Flow in Room?"
            )
            else -> emptyList()
        }

        templates.forEach { prompt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSuggestionClicked(prompt) }
                    .testTag("suggestion_chip_${prompt.hashCode()}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Suggestion prompt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = bubbleShape,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    // Display Mode Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFE65100), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BHARAT AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                FormattedMessageText(text = message.content)
            }
        }
        Text(
            text = formatTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun FormattedMessageText(text: String) {
    // Detect code blocks representation: e.g. ```code```
    val segments = remember(text) { text.split("```") }

    Column {
        segments.forEachIndexed { index, segment ->
            if (index % 2 == 1) {
                // Code block formatting
                val lines = segment.trim().split("\n")
                val language = lines.firstOrNull() ?: ""
                val code = if (lines.size > 1) {
                    lines.subList(1, lines.size).joinToString("\n")
                } else {
                    segment
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Slate Dark Background
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (language.isNotEmpty() && language != code) {
                            Text(
                                text = language.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0), // Emerald Light
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0) // Off-white
                        )
                    }
                }
            } else {
                // Standard markdown text with **bold** highlights
                val annotatedString = buildAnnotatedString {
                    val parts = segment.split("**")
                    parts.forEachIndexed { pIndex, part ->
                        if (pIndex % 2 == 1) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(part)
                            }
                        } else {
                            append(part)
                        }
                    }
                }
                if (annotatedString.isNotEmpty()) {
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingAssistantBubble(engineMode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = when (engineMode) {
                        "BHARAT_GPT" -> Color(0xFFE65100)
                        "BHARAT_GEMINI" -> Color(0xFF1976D2)
                        "BHARAT_SPEAK" -> Color(0xFF4CAF50)
                        "BHARAT_CODE" -> Color(0xFF9C27B0)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "BharatAI is crafting reply...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Simple time formatter utility as standard java.text
fun formatTime(timestamp: Long): String {
    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return format.format(java.util.Date(timestamp))
}
