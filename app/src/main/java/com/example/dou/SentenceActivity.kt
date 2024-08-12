package com.example.dou

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivitySentenceBinding
import model.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/*
TODO 1) API 요청 실패한 경우에 다시 요청할 수 있도록 해야 함 => 현재는 한번 요청한 경우 다시 요청할 수 없도록 막혀 있음
     2) API 요청이 성공하여도 화면에 뜨기 전에 다른 탭으로 이동하면 다른 탭에서 이전 요청 내용이 작성되는 문제가 발생함 '
     => 작성이 완료될 때 까지 다른 탭으로 넘어가지 못하도록 해야 할 듯
*/

class SentenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySentenceBinding
    private lateinit var adapter: SentenceAdapter
    private val sentenceItems = mutableListOf<SentenceItem>()

    private lateinit var chatadapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()
    private val messageList = ArrayList<Message>()

    private val emotionDataList = mutableListOf<EmotionResult>()
    private val conversationHistoryMap = mutableMapOf<Int, MutableList<ChatItem>>()
    private var selectedConversation: Int = 0
    private var waitingForPositiveResponse: Boolean = false
    private var isApiRequestInProgress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySentenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼을 눌렀을 때 처리할 콜백 설정
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 대화 내용을 저장
                logAllConversations()

                // 뒤로가기 버튼을 누를 때 Toast 메시지 표시
                Toast.makeText(this@SentenceActivity, "뒤로가기를 할 수 없어", Toast.LENGTH_LONG).show()
            }
        })

        adapter = SentenceAdapter(sentenceItems) { position ->
            onSentenceItemClick(position)
        }
        binding.chatListRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chatListRecycler.adapter = adapter

        chatadapter = ChatAdapter(chatItems)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)
        binding.chatRecycler.adapter = chatadapter

        val sentences = intent.getStringExtra("sentences")
        val originalSentences = intent.getStringExtra("originalSentences")
        Log.d("ChatActivity", "Received sentences: $sentences")

        if (sentences != null && originalSentences != null) {
            analyzeEmotion(sentences)
        } else {
            Log.d("ChatActivity", "Sentences is null")
        }

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }

        binding.btnEnd.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Toast 메시지 표시
            Toast.makeText(this, "너랑 대화해서 좋았어! 다음에 또 얘기하자!", Toast.LENGTH_LONG).show()

            // 현재 Activity 종료
            finish()
        }
    }

    private fun onSentenceItemClick(position: Int) {
        if (isApiRequestInProgress) {
            Toast.makeText(this, "현재 요청이 진행 중입니다. 완료 후 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 현재 대화 내용을 저장
        saveConversation(selectedConversation)

        // 화면 초기화
        chatItems.clear()
        chatadapter.notifyDataSetChanged()

        // 선택된 대화 항목의 내용을 불러오기
        conversationHistoryMap[position]?.let { savedChatItems ->
            chatItems.addAll(savedChatItems)
        }

        // 새로운 대화 시작 시, 선택된 대화 항목의 내용을 바인딩
        sentenceItems.forEachIndexed { index, item ->
            item.isSelected = index == position
        }
        adapter.notifyDataSetChanged()

        if (position < emotionDataList.size) {
            binding.tvSentence.text = emotionDataList[position].sentence
            if (conversationHistoryMap[position] == null) {
                sendFirstSentenceToGPT(emotionDataList[position]) // 클릭된 항목에 해당하는 문장을 사용하여 GPT 호출
                conversationHistoryMap[position] = mutableListOf() // 초기화
            }
        }

        // 선택된 대화 항목을 업데이트
        selectedConversation = position

        // 대화 내용을 업데이트
        chatadapter.notifyDataSetChanged()

        // 유효한 위치로만 스크롤하도록 수정
        if (chatItems.size > 0) {
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
        }
    }

    private fun saveConversation(position: Int) {
        conversationHistoryMap[position] = chatItems.toMutableList()
    }

    private fun analyzeEmotion(sentence: String) {
        val request = EmotionRequest(userId = 0, sentence = sentence)
        Log.d("EmotionRequest", "Request: $request")
        val service = RetrofitApi.getRetrofitService
        val call = service.emotion(request)

        isApiRequestInProgress = true
        call.enqueue(object : Callback<EmotionResponse> {
            override fun onResponse(
                call: Call<EmotionResponse>,
                response: Response<EmotionResponse>
            ) {
                isApiRequestInProgress = false
                if (response.isSuccessful) {
                    val emotionResponse = response.body()
                    if (emotionResponse != null) {
                        emotionDataList.clear()
                        val dataList = emotionResponse.data.data

                        // 마지막 문장을 제외하고 emotionDataList에 추가
                        if (dataList.isNotEmpty() && dataList.last().sentence.isEmpty()) {
                            emotionDataList.addAll(dataList.dropLast(1))
                        } else {
                            emotionDataList.addAll(dataList)
                        }

                        sentenceItems.clear()
                        for (i in emotionDataList.indices) {
                            sentenceItems.add(SentenceItem("대화 ${i + 1}", isSelected = i == 0))
                        }
                        adapter.notifyDataSetChanged()

                        Log.d("DataList", "$dataList")
                        dataList.forEach { data ->
                            if (data.sentence.isNotEmpty()) {
                                Log.d(
                                    "EmotionResponse",
                                    "Sentence: ${data.sentence}, Sentiment: ${data.sentiment}"
                                )
                            }
                        }

                        if (emotionDataList.isNotEmpty()) {
                            // 첫 번째 대화를 항상 선택된 상태로 설정
                            binding.tvSentence.text = emotionDataList[0].sentence
                            sendFirstSentenceToGPT(emotionDataList[0])
                            conversationHistoryMap[0] = mutableListOf() // 첫 번째 대화에 대해 초기화
                        }
                    }
                } else {
                    Log.e(
                        "EmotionAnalyzer",
                        "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}"
                    )
                    retryAnalyzeEmotion(sentence) // 실패 시 재시도
                }
            }

            override fun onFailure(call: Call<EmotionResponse>, t: Throwable) {
                isApiRequestInProgress = false
                Log.e("EmotionAnalyzer", "API 호출 실패", t)
                retryAnalyzeEmotion(sentence) // 실패 시 재시도
            }
        })
    }

    // API 요청 실패 시에 재시도 기능 추가함
    private fun retryAnalyzeEmotion(sentence: String) {
        Toast.makeText(this, "API 요청 실패. 다시 시도합니다...", Toast.LENGTH_SHORT).show()
        analyzeEmotion(sentence)
    }

    private fun sendMessage() {
        val message = binding.editTxt.text.toString().trim()
        if (message.isNotEmpty()) {
            val chatItem = ChatItem(message, isSentByMe = true)
            chatItems.add(chatItem)
            chatadapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
            binding.editTxt.text.clear()

            // 대화 내용 저장
            saveConversation(selectedConversation, chatItem)

            handleUserInput(message)
        } else {
            Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveConversation(position: Int, chatItem: ChatItem) {
        if (!conversationHistoryMap.containsKey(position)) {
            conversationHistoryMap[position] = mutableListOf()
        }
        conversationHistoryMap[position]?.add(chatItem)
    }

    private fun handleUserInput(userInput: String) {
        addToConversationHistory("User: $userInput")

        if (waitingForPositiveResponse) {
            sendGPTRequest(userInput, "TRANSFORM_CONFIRM", "분노")
            waitingForPositiveResponse = false
        } else {
            sendGPTRequest(userInput, determineReqType(1), determineReqSent(1))
        }
    }

    private fun sendGPTRequest(userInput: String, reqType: String, reqSent: String) {
        val request = GPTRequest(
            userId = 0,
            context = userInput,
            reqType = reqType,
            reqSent = reqSent
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.getGPTResponse(request)

        // Api 요청중이면 탭 이동이 불가능하도록 추가한 조건
        isApiRequestInProgress = true
        call.enqueue(object : Callback<GPTResponse> {
            override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                isApiRequestInProgress = false
                if (response.isSuccessful) {
                    val gptResponse = response.body()
                    gptResponse?.let {
                        val receivedMessage = it.data.response

                        receiveMessage("${receivedMessage}")

                        if (reqType == "TRANSFORM_CONFIRM") {
                            receiveMessage("\"${userInput}\"으로 긍정적인 마음을 가지는 걸로 하자!")
                        } else if (it.data.positive != null) {
                            val positiveMessages = it.data.positive
                            positiveMessages.forEachIndexed { index, message ->
                                Log.d("PositiveMessage", message)
                                receiveMessage("${index + 1}. $message")
                            }
                            receiveMessage("위의 세 문장을 참고해서 부정적인 문장을 긍정적인 문장으로 바꿔보자!")
                            waitingForPositiveResponse = true
                        }
                    }
                } else {
                    val errorMessage =
                        "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
                    Log.e("API Communication", errorMessage)
                    Toast.makeText(this@SentenceActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                    retrySendGPTRequest(userInput, reqType, reqSent) // 실패 시 재시도
                }
            }

            override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                isApiRequestInProgress = false
                Log.e("API Communication", "API 통신 실패", t)
                runOnUiThread {
                    Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                }
                retrySendGPTRequest(userInput, reqType, reqSent) // 실패 시 재시도
            }
        })
    }

    private fun retrySendGPTRequest(userInput: String, reqType: String, reqSent: String) {
        Toast.makeText(this, "API 요청 실패. 다시 시도합니다...", Toast.LENGTH_SHORT).show()
        sendGPTRequest(userInput, reqType, reqSent)
    }

    private fun receiveMessage(message: String) {
        runOnUiThread {
            val chatItem = ChatItem(message, isSentByMe = false)
            chatItems.add(chatItem)
            chatadapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
            addToConversationHistory("GPT: $message")
            saveConversation(selectedConversation, chatItem)
            val receivedMessage = Message("0", message)
            messageList.add(receivedMessage)
            Log.d("MessageList", "All Messages:")
            for (msg in messageList) {
                Log.d("MessageList", "${msg.sentBy}, ${msg.message}")
            }
        }
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

            isApiRequestInProgress = true
            call.enqueue(object : Callback<GPTResponse> {
                override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                    isApiRequestInProgress = false
                    if (response.isSuccessful) {
                        val gptResponse = response.body()
                        gptResponse?.let {
                            val receivedMessage = it.data.response

                            receiveMessage("${data.sentence}" + "라는 말을 했네!")
                            receiveMessage("${receivedMessage}")

                            if (it.data.positive != null) {
                                it.data.positive.forEachIndexed { index, message ->
                                    Log.d("PositiveMessage", message)
                                    receiveMessage("${index + 1}. $message")
                                }
                                receiveMessage("위의 세 문장을 참고해서 부정적인 문장을 긍정적인 문장으로 바꿔보자!")
                                waitingForPositiveResponse = true
                            }
                        }
                    } else {
                        val errorMessage =
                            "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
                        Log.e("API Communication_First", errorMessage)
                        Toast.makeText(this@SentenceActivity, "API 요청 실패", Toast.LENGTH_SHORT)
                            .show()
                        retrySendGPTRequest(data.sentence, reqType, reqSent) // 실패 시 재시도
                    }
                }

                override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                    isApiRequestInProgress = false
                    Log.e("API Communication", "API 통신 실패", t)
                    runOnUiThread {
                        Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                    retrySendGPTRequest(data.sentence, reqType, reqSent) // 실패 시 재시도
                }
            })
        }
    }

    private fun logAllConversations() {
        conversationHistoryMap.forEach { (position, chatItems) ->
            val conversationData = StringBuilder()
            conversationData.append("대화 $position - 시작\n")

            chatItems.forEach { chatItem ->
                val sender = if (chatItem.isSentByMe) "User" else "GPT"
                conversationData.append("$sender: ${chatItem.message}\n")
            }

            conversationData.append("대화 $position - 종료\n")

            // 로그로 출력
            Log.d("ConversationData_Tab_$position", conversationData.toString())
        }
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

    override fun onDestroy() {
        super.onDestroy()
        logAllConversations() // 액티비티 종료 시 대화 내용 로그로 출력
    }

    override fun onPause() {
        super.onPause()
        logAllConversations() // 액티비티 일시 중지 시 대화 내용 로그로 출력
    }
}
