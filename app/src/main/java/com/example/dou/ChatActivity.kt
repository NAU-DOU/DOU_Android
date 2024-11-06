package com.example.dou

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivityChatBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import model.Message
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import io.reactivex.rxjava3.schedulers.Schedulers

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    private val messageList = ArrayList<Message>()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    private val emotionDataList = mutableListOf<EmotionResult>()

    //private var disposable: Disposable? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(chatItems)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)
        binding.chatRecycler.adapter = adapter

        // Intent에서 sentences 값을 가져옵니다.
        val sentences = intent.getStringExtra("sentences")
        val originalSentences = intent.getStringExtra("originalSentences")
        Log.d("ChatActivity", "Received sentences: $sentences")

        // Null 체크 후 analyzeEmotion 함수 호출
        if (sentences != null && originalSentences != null) {
            // gpt로부터 받은 내용을 가지고 요약한걸 제일 먼저 화면에 보여주기
            summaryEmotion(originalSentences)

            // 그리고 다음으로는 감정 인식을 통해서 제일 먼저 받은 내용을 사용자에게 보내기
            analyzeEmotion(sentences)

        } else {
            Log.d("ChatActivity", "Sentences is null")
        }

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }
    }

    private fun summaryEmotion(context: String) {
        val request = SummaryRequest(userId = 0, context = context)
        Log.d("SummaryRequest", "Request: $request")
        val service = RetrofitApi.getRetrofitService
        val call = service.summary(request)

        call.enqueue(object : Callback<SummaryResponse> {
            override fun onResponse(
                call: Call<SummaryResponse>,
                response: Response<SummaryResponse>
            ) {
                if (response.isSuccessful) {
                    val summaryResponse = response.body()
                    if (summaryResponse != null) {

                        val response = summaryResponse.data
                        Log.d("SummaryResponse", "Summary: ${response.summary}")

                        receiveMessage("\'${response.summary}\'" + "라는 대화를 했네")
                    }
                } else {
                    Log.e("SummaryAPI", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SummaryResponse>, t: Throwable) {
                Log.e("SummaryAPI", "API 호출 실패", t)
            }
        })
    }

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

//    private fun analyzeEmotion(sentence: String) {
//        val request = EmotionRequest(userId = 0, sentence = sentence)
//        Log.d("EmotionRequest", "Request: $request")
//        val service = RetrofitApi.getRetrofitService
//        val call = service.emotion(request)
//
//        call.enqueue(object : Callback<EmotionResponse> {
//            override fun onResponse(
//                call: Call<EmotionResponse>,
//                response: Response<EmotionResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val emotionResponse = response.body()
//                    if (emotionResponse != null) {
//                        emotionDataList.clear() // 기존 데이터를 지우고
//                        emotionDataList.addAll(emotionResponse.data.data) // 새로운 데이터를 저장
//                        val emoSentences = mutableListOf<String>()
//                        val sentiments = mutableListOf<Int>()
//                        val dataList = emotionResponse.data.data
//                        Log.d("DataList", "$dataList")
//                        dataList.forEach { data ->
//                            if (data.sentence.isNotEmpty()) {
//                                emoSentences.add(data.sentence)
//                                sentiments.add(data.sentiment)
//                            }
//                        }
//
//                        // 각 문장과 감정을 로그에 출력
//                        for (i in emoSentences.indices) {
//                            Log.d("EmotionResponse", "Sentence: ${emoSentences[i]}, Sentiment: ${sentiments[i]}")
//                        }
//                        if (dataList.isNotEmpty()) {
//                            sendFirstSentenceToGPT(dataList[0])
//                        }
//                    }
//                } else {
//                    Log.e("EmotionAnalyzer", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(call: Call<EmotionResponse>, t: Throwable) {
//                Log.e("EmotionAnalyzer", "API 호출 실패", t)
//            }
//        })
//    }

    private fun analyzeEmotion(sentence: String) {
        val request = EmotionRequest(userId = 0, sentence = sentence)
        Log.d("EmotionRequest", "Request: $request")
        val service = RetrofitApi.getRetrofitService

        val disposable = service.emotion(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                emotionDataList.clear()
                emotionDataList.addAll(response.data.data)
                if (emotionDataList.isNotEmpty()) {
                    sendFirstSentenceToGPT(emotionDataList[0])
                }
            }, { error ->
                Log.e("EmotionAnalyzer", "API 호출 실패", error)
                Toast.makeText(this, "API 호출 실패", Toast.LENGTH_SHORT).show()
            })

        // 구독을 CompositeDisposable에 추가하여 관리
        compositeDisposable.add(disposable)
    }


//    private fun sendFirstSentenceToGPT(data: EmotionResult) {
//        if (data.sentence.isNotEmpty()) {
//            val reqType = determineReqType(data.sentiment)
//            val reqSent = determineReqSent(data.sentiment)
//
//            val request = GPTRequest(
//                userId = 0,
//                context = data.sentence,
//                reqType = reqType,
//                reqSent = reqSent
//            )
//
//            Log.d("GPT First Request", "$request")
//
//            val service = RetrofitApi.getRetrofitService
//            val call = service.getGPTResponse(request)
//
//            call.enqueue(object : Callback<GPTResponse> {
//                override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
//                    if (response.isSuccessful) {
//                        val gptResponse = response.body()
//                        gptResponse?.let {
//                            val receivedMessage = it.data.response
//                            receiveMessage("${data.sentence}"+"라는 말을 했네!\n"+"${receivedMessage}")
//                        }
//                    } else {
//                        val errorMessage = "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
//                        Log.e("API Communication_First", errorMessage)
//                        Toast.makeText(this@ChatActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
//                    Log.e("API Communication", "API 통신 실패", t)
//                    runOnUiThread {
//                        Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            })
//        }

    private fun sendFirstSentenceToGPT(data: EmotionResult) {
        if (data.sentence.isNotEmpty()) {
            val reqType = determineReqType(data.sentiment_idx)
            val reqSent = determineReqSent(data.sentiment_idx)

            val request = GPTRequest(
                userId = 0,
                context = data.sentence,
                reqType = reqType,
                reqSent = reqSent
            )
            Log.d("GPT First Request", "$request")
            val service = RetrofitApi.getRetrofitService

            val disposable = service.getGPTResponse(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    response.data?.let {
                        receiveMessage("${data.sentence}라는 말을 했네!\n${it.response}")
                    }
                }, { error ->
                    Log.e("API Communication_First", "API 요청 실패", error)
                    Toast.makeText(this, "API 요청 실패", Toast.LENGTH_SHORT).show()
                })

            compositeDisposable.add(disposable)
        }
    }

    private fun sendStoredEmotionData() {
        if (emotionDataList.isNotEmpty()) {
            // 원하는 인덱스를 지정하여 데이터를 전송
            val index = 1 // 예시로 두 번째 데이터를 전송
            if (index < emotionDataList.size) {
                val data = emotionDataList[index]
                val message = "Sentiment: ${determineReqSent(data.sentiment_idx)}, Sentence: ${data.sentence}"
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

        // 구독 객체 생성 및 관리
        val disposable = service.getGPTResponse(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                response.data?.let {
                    val receivedMessage = it.response
                    receiveMessage("${receivedMessage}\n다음 문장으로 넘어가고 싶으면 '다음'이라고 적어줘!")
                }
            }, { error ->
                Log.e("API Communication", "API 요청 실패", error)
                Toast.makeText(this, "API 요청 실패", Toast.LENGTH_SHORT).show()
            })

        // CompositeDisposable에 구독 추가
        compositeDisposable.add(disposable)
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
        compositeDisposable.clear()
    }
}
