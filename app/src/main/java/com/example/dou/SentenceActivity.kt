package com.example.dou

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivitySentenceBinding
import model.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SentenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySentenceBinding
    private lateinit var adapter: SentenceAdapter
    private val sentenceItems = mutableListOf<SentenceItem>()

    private lateinit var chatadapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()
    private val messageList = ArrayList<Message>()

    private val emotionDataList = mutableListOf<EmotionResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySentenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //adapter = SentenceAdapter(sentenceItems)
        adapter = SentenceAdapter(sentenceItems) { position ->
            onSentenceItemClick(position)
        }
        binding.chatListRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chatListRecycler.adapter = adapter

        sentenceItems.apply {
            add(SentenceItem("대화1", true))
            add(SentenceItem("대화2"))
            add(SentenceItem("대화3"))
            add(SentenceItem("대화4"))
            add(SentenceItem("대화5"))
            add(SentenceItem("대화6"))
            add(SentenceItem("대화7"))
            add(SentenceItem("대화8"))
            add(SentenceItem("대화9"))
        }

        chatadapter = ChatAdapter(chatItems)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)
        binding.chatRecycler.adapter = adapter

        // Intent에서 sentences 값을 가져옵니다.
        val sentences = intent.getStringExtra("sentences")
        val originalSentences = intent.getStringExtra("originalSentences")
        Log.d("ChatActivity", "Received sentences: $sentences")

        // Null 체크 후 analyzeEmotion 함수 호출
        if (sentences != null && originalSentences != null) {
            // gpt로부터 받은 내용을 가지고 요약한걸 제일 먼저 화면에 보여주기
            // summaryEmotion(originalSentences)

            // 그리고 다음으로는 감정 인식을 통해서 제일 먼저 받은 내용을 사용자에게 보내기
            analyzeEmotion(sentences)

        } else {
            Log.d("ChatActivity", "Sentences is null")
        }

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }
    }

    private fun onSentenceItemClick(position: Int) {
        // 모든 아이템의 선택 상태를 해제하고 클릭된 아이템만 선택 상태로 설정
        sentenceItems.forEachIndexed { index, item ->
            item.isSelected = index == position
        }
        adapter.notifyDataSetChanged()

        // 여기서 클릭된 대화 아이템에 맞는 메시지 데이터를 로드하여 chatItems를 업데이트하는 코드를 추가하세요.
    }

//    private fun summaryEmotion(context: String) {
//        val request = SummaryRequest(userId = 0, context = context)
//        Log.d("SummaryRequest", "Request: $request")
//        val service = RetrofitApi.getRetrofitService
//        val call = service.summary(request)
//
//        call.enqueue(object : Callback<SummaryResponse> {
//            override fun onResponse(
//                call: Call<SummaryResponse>,
//                response: Response<SummaryResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val summaryResponse = response.body()
//                    if (summaryResponse != null) {
//
//                        val response = summaryResponse.data
//                        Log.d("SummaryResponse", "Summary: ${response.response}")
//
//                        receiveMessage("\'${response.response}\'" + "라는 대화를 했네")
//                    }
//                } else {
//                    Log.e("SummaryAPI", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(call: Call<SummaryResponse>, t: Throwable) {
//                Log.e("SummaryAPI", "API 호출 실패", t)
//            }
//        })
//    }

    private fun sendMessage() {
        val message = binding.editTxt.text.toString().trim()
        if (message.isNotEmpty()) {

            val chatItem = ChatItem(message, isSentByMe = true)
            chatItems.add(chatItem)
            adapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
            binding.editTxt.text.clear()

            handleUserInput(message)
        } else {
            Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUserInput(userInput: String) {
        addToConversationHistory("User: $userInput")
        sendGPTRequest(userInput)
    }

    private fun receiveMessage(message: String) {
        runOnUiThread {
            val chatItem = ChatItem(message, isSentByMe = false)
            chatItems.add(chatItem)
            adapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)

            addToConversationHistory("GPT: $message")

            val receivedMessage = Message("0", message)
            messageList.add(receivedMessage)

            Log.d("MessageList", "All Messages:")
            for (msg in messageList) {
                Log.d("MessageList", "${msg.sentBy}, ${msg.message}")
            }
        }
    }

    private fun analyzeEmotion(sentence: String) {
        val request = EmotionRequest(userId = 0, sentence = sentence)
        Log.d("EmotionRequest", "Request: $request")
        val service = RetrofitApi.getRetrofitService
        val call = service.emotion(request)

        call.enqueue(object : Callback<EmotionResponse> {
            override fun onResponse(
                call: Call<EmotionResponse>,
                response: Response<EmotionResponse>
            ) {
                if (response.isSuccessful) {
                    val emotionResponse = response.body()
                    if (emotionResponse != null) {
                        emotionDataList.clear() // 기존 데이터를 지우고
                        emotionDataList.addAll(emotionResponse.data.data) // 새로운 데이터를 저장
                        val emoSentences = mutableListOf<String>()
                        val sentiments = mutableListOf<Int>()
                        val dataList = emotionResponse.data.data
                        Log.d("DataList", "$dataList")
                        dataList.forEach { data ->
                            if (data.sentence.isNotEmpty()) {
                                emoSentences.add(data.sentence)
                                sentiments.add(data.sentiment)
                            }
                        }

                        // 각 문장과 감정을 로그에 출력
                        for (i in emoSentences.indices) {
                            Log.d("EmotionResponse", "Sentence: ${emoSentences[i]}, Sentiment: ${sentiments[i]}")
                        }
                        if (dataList.isNotEmpty()) {
                            sendFirstSentenceToGPT(dataList[0])
                        }
                    }
                } else {
                    Log.e("EmotionAnalyzer", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<EmotionResponse>, t: Throwable) {
                Log.e("EmotionAnalyzer", "API 호출 실패", t)
            }
        })
    }

    private fun sendFirstSentenceToGPT(data: EmotionResult) {
        if (data.sentence.isNotEmpty()) {
            val reqType = determineReqType(data.sentiment)
            val reqSent = determineReqSent(data.sentiment)

            val request = GPTRequest(
                userId = 0,
                context = data.sentence,
                reqType = reqType,
                reqSent = reqSent
            )

            Log.d("GPT First Request", "$request")

            val service = RetrofitApi.getRetrofitService
            val call = service.getGPTResponse(request)

            call.enqueue(object : Callback<GPTResponse> {
                override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                    if (response.isSuccessful) {
                        val gptResponse = response.body()
                        gptResponse?.let {
                            val receivedMessage = it.data.response
                            receiveMessage("${data.sentence}"+"라는 말을 했네!\n"+"${receivedMessage}")
                        }
                    } else {
                        val errorMessage = "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
                        Log.e("API Communication_First", errorMessage)
                        Toast.makeText(this@SentenceActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                    Log.e("API Communication", "API 통신 실패", t)
                    runOnUiThread {
                        Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun sendStoredEmotionData() {
        if (emotionDataList.isNotEmpty()) {
            // 원하는 인덱스를 지정하여 데이터를 전송
            val index = 1 // 예시로 두 번째 데이터를 전송
            if (index < emotionDataList.size) {
                val data = emotionDataList[index]
                val message = "Sentiment: ${determineReqSent(data.sentiment)}, Sentence: ${data.sentence}"
                receiveMessage(message)
            } else {
                Toast.makeText(this, "인덱스가 범위를 벗어났습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "저장된 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendGPTRequest(userInput: String) {
        // 0~6사이가 아닌 경우에 받는 내용으로는 변환받을 수 있도록
        val reqType = determineReqType(1)
        val reqSent = determineReqSent(1)

        val request = GPTRequest(
            userId = 0,
            context = userInput,
            reqType = reqType,
            reqSent = reqSent
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.getGPTResponse(request)
        call.enqueue(object : Callback<GPTResponse> {
            override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                if (response.isSuccessful) {
                    val gptResponse = response.body()
                    gptResponse?.let {
                        val receivedMessage = it.data.response
                        receiveMessage("${receivedMessage}" + "\n다음 문장으로 넘어가고 싶으면 \'다음\'이라고 적어줘!")
                    }
                } else {
                    val errorMessage = "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
                    Log.e("API Communication", errorMessage)
                    Toast.makeText(this@SentenceActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                Log.e("API Communication", "API 통신 실패", t)
                runOnUiThread {
                    Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun determineReqType(sentiment: Int): String {
        return when (sentiment) {
            0 -> "HAPPY_RESPONSE"
            1, 2 -> "COMMON_RESPONSE"
            3, 4, 5, 6 -> "SENTIMENT_RESPONSE"
            else -> "TRANSFORM_CONFIRM"
        }
    }

    private fun determineReqSent(sentiment: Int): String {
        return when (sentiment) {
            0 -> "행복"
            1 -> "놀람"
            2 -> "중립"
            3 -> "슬픔"
            4 -> "꺼림"
            5 -> "분노"
            6 -> "두려움"
            else -> "알 수 없음"
        }
    }

    private var currentConversation: String? = null
    private val conversationHistory = mutableListOf<String>()

    private fun addToConversationHistory(message: String) {
        conversationHistory.add(message)
        currentConversation = conversationHistory.joinToString(separator = "\n")
    }
}