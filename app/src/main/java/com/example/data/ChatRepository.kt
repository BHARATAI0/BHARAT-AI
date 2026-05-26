package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allThreads: Flow<List<ChatThread>> = chatDao.getAllThreads()

    fun getMessages(threadId: Int): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForThread(threadId)
    }

    suspend fun createThread(title: String, engineMode: String): Int = withContext(Dispatchers.IO) {
        val thread = ChatThread(title = title, engineMode = engineMode)
        val id = chatDao.insertThread(thread)
        id.toInt()
    }

    suspend fun deleteThread(threadId: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteThreadById(threadId)
    }

    suspend fun saveMessage(threadId: Int, role: String, content: String) = withContext(Dispatchers.IO) {
        val message = ChatMessage(threadId = threadId, role = role, content = content)
        chatDao.insertMessage(message)
    }

    suspend fun generateAnswer(
        threadId: Int,
        userPrompt: String,
        engineMode: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "APIKeyError: Please enter your Gemini API Key in the Secrets panel in the AI Studio UI to start interacting with BharatAI!"
        }

        // Fetch past messages in this thread to build an accurate conversation history context
        val pastMessages = chatDao.getMessagesForThread(threadId).firstOrNull() ?: emptyList()

        // Establish the custom system instruction
        val sysInstruction = getSystemPrompt(engineMode)

        // Construct current conversation log to embed in contents
        val conversationBuilder = StringBuilder()
        pastMessages.forEach { msg ->
            val speaker = if (msg.role == "user") "User" else "Response"
            conversationBuilder.append("$speaker: ${msg.content}\n\n")
        }
        
        // Append the current user prompt
        conversationBuilder.append("User: $userPrompt\n\nResponse:")

        val finalPrompt = conversationBuilder.toString()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = finalPrompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = sysInstruction))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            resultText ?: "I experienced an issue processing that. Could you please rephrase or try again?"
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error during Gemini call", e)
            val errMsg = e.message ?: "Unknown issue"
            if (errMsg.contains("API key") || errMsg.contains("400") || errMsg.contains("403")) {
                "Error: Could not authenticate with Gemini API. Please make sure a valid Gemini API Key is configured in the AI Studio Secrets panel."
            } else {
                "Error: $errMsg"
            }
        }
    }

    private fun getSystemPrompt(mode: String): String {
        return when (mode) {
            "BHARAT_GPT" -> """
                You are BharatAI, specifically operating under 'BharatGPT' mode. 
                You are a powerful, warm, polite, and culturally intelligent artificial intelligence with an Indian soul.
                You are fluent in English, Hindi, Hinglish (Hindi text in Latin script), and major Indian regional languages (Tamil, Telugu, Bengali, Marathi, Gujarati, Kannada, Malayalam, Odia, Punjabi, etc.).
                Key guidelines for your behavior:
                1. Respond with warmth, respect, and deep understanding of Indian society, values, history, and contexts.
                2. Use appropriate local greetings when starting (like Namaste, Namaskar, Pranam, Sat Sri Akal, Vanakkam, Adaab, Nomoshkar).
                3. Address users politely, using culturally respectful terms when appropriate.
                4. Provide helpful advice, local references, and practical responses (e.g., explaining concepts using rupee examples, local landmarks, or Indian analogies).
                5. Keep the conversational charm of ChatGPT, prioritizing fluid, human-like dialogue, empathy, and easy comprehension.
                6. Keep formatting neat and paragraph-focused with custom Indian flavor.
            """.trimIndent()

            "BHARAT_GEMINI" -> """
                You are BharatAI, specifically operating under 'BharatGemini' mode.
                You are a high-performance, fast, analytical, and information-focused artificial intelligence.
                Key guidelines for your behavior:
                1. Deliver structured, point-by-point breakdowns of questions.
                2. Use Markdown styling heavily, including bullet lists, bold highlights, code blocks, and clear headers.
                3. Specialize in detailed factual topics, including modern science, history, academic syllabus items, and government schemes in India (like PM-Kisan, Ayushman Bharat, PM Jan Dhan Yojana, Digital India components, etc.).
                4. Break down complex issues logically and help the user step-by-step.
                5. Aim for technical depth and accurate intelligence matching the signature structured outputs of Gemini.
            """.trimIndent()

            "BHARAT_SPEAK" -> """
                You are BharatAI, specifically operating under 'BharatSpeak' mode.
                You are a language tutor, pronunciation aid, and cross-cultural communication expert for Indian dialects and languages.
                Key guidelines for your behavior:
                1. Assist in translating seamlessly between English, Hindi, and regional languages.
                2. Break down colloquial sayings, idioms, and translations so they make sense in local context.
                3. If the user inputs a query, answer with clear translations, transcription (how to pronounce it), and context of usage.
                4. Help users write letters, emails, or messages in Hindi, regional languages, or clean business English.
                5. Maintain a super encouraging, tutor-like persona.
            """.trimIndent()

            "BHARAT_CODE" -> """
                You are BharatAI, specifically operating under 'BharatCode' mode.
                You are a master software craftsman and coding assistant designed for Indian programmers, IT students, and tech builders.
                Key guidelines for your behavior:
                1. Provide clean, secure, and modern code snippets (Kotlin, Java, Python, JavaScript, SQL, Android layouts, etc.).
                2. Explain code rationale line-by-line where helpful, following standard software architecture principles.
                3. Suggest optimizations, fix bugs, and debug traces provided by the user.
                4. Use clear formatting with proper language highlights in markdown code fences.
                5. Keep explanations professional, tech-focused, and highly actionable.
            """.trimIndent()

            else -> "You are BharatAI, a powerful conversational assistant for India."
        }
    }
}
