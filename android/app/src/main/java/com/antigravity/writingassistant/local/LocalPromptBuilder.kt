package com.antigravity.writingassistant.local

object LocalPromptBuilder {

    /**
     * Builds a Gemma-compatible prompt using its chat template format.
     * Format: <start_of_turn>user\n{instruction}\n<end_of_turn>\n<start_of_turn>model\n
     */
    fun buildPrompt(input: String, instruction: String): String {
        val taskDescription = when {
            instruction.contains("professional", ignoreCase = true) -> 
                "Rewrite the following text in a formal, professional tone. Strictly output ONLY the rewritten text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("formal", ignoreCase = true) -> 
                "Rewrite the following text in a formal tone. Strictly output ONLY the rewritten text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("casual", ignoreCase = true) -> 
                "Rewrite the following text in a casual, friendly tone. Strictly output ONLY the rewritten text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("warm", ignoreCase = true) -> 
                "Rewrite the following text in a warm, caring tone. Strictly output ONLY the rewritten text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("love", ignoreCase = true) -> 
                "Rewrite the following text in an affectionate, loving tone. Strictly output ONLY the rewritten text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("concise", ignoreCase = true) -> 
                "Summarize the following text to be more concise. Strictly output ONLY the summary. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("grammar", ignoreCase = true) -> 
                "Fix all grammar and spelling errors in the following text. Strictly output ONLY the corrected text. Do not include 'Sure', 'Here is', or any conversational filler."
            instruction.contains("emojify", ignoreCase = true) -> 
                "Rewrite the following text and insert relevant emojis throughout. You MUST include emojis. Strictly output ONLY the text with emojis. do not include any preamble."
            else -> 
                "Improve the following text to be clearer and more readable. Strictly output ONLY the improved text. Do NOT include 'Sure', 'Here is', or any conversational filler."
        }
        
        // Gemma instruction-tuned chat template
        return """<start_of_turn>user
$taskDescription

Text: $input
<end_of_turn>
<start_of_turn>model
"""
    }
    
    /**
     * Returns just the task type for simulation/logging.
     */
    fun getTaskType(instruction: String): String {
        return when {
            instruction.contains("professional", ignoreCase = true) -> "Professional"
            instruction.contains("formal", ignoreCase = true) -> "Formal"
            instruction.contains("casual", ignoreCase = true) -> "Casual"
            instruction.contains("warm", ignoreCase = true) -> "Warm"
            instruction.contains("love", ignoreCase = true) -> "Love"
            instruction.contains("concise", ignoreCase = true) -> "Concise"
            instruction.contains("grammar", ignoreCase = true) -> "Grammar"
            instruction.contains("emojify", ignoreCase = true) -> "Emojify"
            else -> "Refine"
        }
    }
}
