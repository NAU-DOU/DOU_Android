package com.example.dou

import com.google.gson.annotations.SerializedName

object OpenAIDataClass {
    data class Request(
        @SerializedName("model") val model: String,
        @SerializedName("prompt") val prompt: String,
        @SerializedName("temperature") val temperature: Float,
        @SerializedName("max_tokens") val maxTokens: Int
    )

    data class Response(
        @SerializedName("choices") val choices: List<Choice>
    )

    data class Choice(
        @SerializedName("text") val text: String
    )
}