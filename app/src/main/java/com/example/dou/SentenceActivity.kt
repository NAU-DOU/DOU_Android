package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivitySentenceBinding
import com.google.gson.Gson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import model.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/*
TODO 1) roomid 만든 후에 record Position 별로 summary와 같은 내용 전달해야 함
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

    private val recordIdMap = mutableMapOf<Int, Int>() // 각 대화 항목에 대한 recordId를 저장

    private val compositeDisposable = CompositeDisposable()


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
        val roomId = intent.getIntExtra("roomId",-1)
        Log.d("ChatActivity", "Received sentences: $sentences")
        Log.d("SentenceActivity", "Received roomId: $roomId")

        if (sentences != null && originalSentences != null) {
            analyzeEmotion(sentences,roomId)

            // 전체 대화 내용 요약을 위해서 summary 시도하기
            summaryChat(originalSentences,roomId)
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

    // room>record>chat인데 room에서는 지금 summary를 안하고 있어서 잠시 주석처리
    // summary 만들고 나서 summary를 바탕으로 다시 감정 분석하고, 해당 내용을 roomId에 넣어줘야됨..

    // Swagger에서 gpt summary부분 이용하기
    private fun summaryChat(context: String, roomId: Int){
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }

        // SummaryRequest 생성
        val request = SummaryRequest(userId = userId, context = context)
        Log.d("SummaryRequest", "Request: $request")

        // Retrofit 서비스 인터페이스 호출
        val service = RetrofitApi.getRetrofitService
        val call = service.summary(request)

        // 비동기적으로 API 요청 실행
        call.enqueue(object : Callback<SummaryResponse> {
            override fun onResponse(call: Call<SummaryResponse>, response: Response<SummaryResponse>) {
                if (response.isSuccessful) {
                    // API 요청이 성공한 경우
                    val summaryResponse = response.body()
                    val roomSummaryResult = summaryResponse?.data?.summary

                    /* TODO
                    *   1) API 요청 후 받은 요약 내용을 가지고 감정 분석 실시 2) 감정 분석 후 sentiment를 가지고 다시 해당 room의 sentiment를 조절해야 함*/
                    if (roomSummaryResult != null) {
                        analyzeEmotion(roomSummaryResult, roomId = roomId)
                    }

                    Log.d("SummaryResponse", "Response: $summaryResponse")
                } else {
                    // 응답이 성공적이지 않은 경우 (예: 서버 에러)
                    Log.e("SummaryResponse", "Failed with response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SummaryResponse>, t: Throwable) {
                // 네트워크 요청이 실패한 경우 (예: 네트워크 오류)
                Log.e("SummaryRequest", "Request failed", t)
            }
        })
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

    private fun analyzeEmotion(sentence: String, roomId: Int) {
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }

        val request = EmotionRequest(userId = userId, sentence = sentence)
        Log.d("EmotionRequest", "Request: $request")

        val disposable = RetrofitApi.getRetrofitService.emotion(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                emotionDataList.clear()
                val dataList = response.data.data
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
                    Log.d("EmotionResponse", "Sentence: ${data.sentence}, Sentiment: ${data.sentiment}")
                }

                if (emotionDataList.isNotEmpty()) {
                    binding.tvSentence.text = emotionDataList[0].sentence
                    sendFirstSentenceToGPT(emotionDataList[0])
                    conversationHistoryMap[0] = mutableListOf()
                }

                // RecordPost를 각 emotionDataList에 대해 실행
                emotionDataList.forEachIndexed { index, emotionResult ->
                    postRecordForEmotion(roomId, emotionResult, index) // index를 position으로 전달
                }

                val finalSentiment = emotionDataList.lastOrNull()?.sentiment_idx ?: 0
                roomPatchSentiment(roomId, finalSentiment)
            }, { error ->
                Log.e("EmotionAnalyzer", "API 호출 실패", error)
            })

        compositeDisposable.add(disposable)
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
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }

        val request = GPTRequest(userId = userId, context = userInput, reqType = reqType, reqSent = reqSent)
        Log.d("GPTRequest", "Request: $request")

        val disposable = RetrofitApi.getRetrofitService.getGPTResponse(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val receivedMessage = response.data.response
                receiveMessage(receivedMessage)

                if (reqType == "TRANSFORM_CONFIRM") {
                    receiveMessage("\"$userInput\"으로 긍정적인 마음을 가지는 걸로 하자!")
                } else if (response.data.positive != null) {
                    response.data.positive.forEachIndexed { index, message ->
                        Log.d("PositiveMessage", message)
                        receiveMessage("${index + 1}. $message")
                    }
                    receiveMessage("위의 세 문장을 참고해서 부정적인 문장을 긍정적인 문장으로 바꿔보자!")
                    waitingForPositiveResponse = true
                }
            }, { error ->
                Log.e("API Communication", "API 요청 실패", error)
                Toast.makeText(this, "API 요청 실패", Toast.LENGTH_SHORT).show()
                retrySendGPTRequest(userInput, reqType, reqSent)
            })

        compositeDisposable.add(disposable)
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
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }


        if (data.sentence.isNotEmpty()) {
            val reqType = determineReqType(data.sentiment_idx)
            val reqSent = determineReqSent(data.sentiment_idx)

            val request = GPTRequest(
                userId = userId,
                context = data.sentence,
                reqType = reqType,
                reqSent = reqSent
            )

            Log.d("GPT First Request", "$request")

            val service = RetrofitApi.getRetrofitService

            isApiRequestInProgress = true
            val disposable = service.getGPTResponse(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    isApiRequestInProgress = false
                    response.data?.let {
                        val receivedMessage = it.response

                        receiveMessage("${data.sentence}라는 말을 했네!")
                        receiveMessage(receivedMessage)

                        it.positive?.forEachIndexed { index, message ->
                            Log.d("PositiveMessage", message)
                            receiveMessage("${index + 1}. $message")
                        }
                        if (it.positive != null) {
                            receiveMessage("위의 세 문장을 참고해서 부정적인 문장을 긍정적인 문장으로 바꿔보자!")
                            waitingForPositiveResponse = true
                        }
                    }
                }, { error ->
                    isApiRequestInProgress = false
                    Log.e("API Communication_First", "API 요청 실패", error)
                    Toast.makeText(this@SentenceActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                    retrySendGPTRequest(data.sentence, reqType, reqSent) // 실패 시 재시도
                })

            compositeDisposable.add(disposable)
        }
    }
    private fun logAllConversations() {
        val roomId = intent.getIntExtra("roomId", -1)
        val chatRequestList = mutableListOf<ChatRequest>()

        conversationHistoryMap.forEach { (position, chatItems) ->
            chatItems.forEachIndexed { index, chatItem ->
                // 감정 분석 결과에서 sentiment 값을 가져오기
                val sentiment = if (index < emotionDataList.size) {
                    emotionDataList[index].sentiment_idx
                } else {
                    1 // 기본값으로 1을 설정 (원하는 기본값으로 설정 가능)
                }

                val userId = getUserId()
                if (userId != -1) {
                    Log.d("UserData", "User ID: $userId")
                } else {
                    Log.d("UserData", "No User ID found in SharedPreferences")
                }

                // position에 대응하는 recordId를 가져옴
                val recordId = recordIdMap[position] ?: (position + 1) // recordIdMap에서 가져오거나 기본값으로 position + 1 사용

                // 각 메시지를 ChatRequest 객체로 변환
                val chatRequest = ChatRequest(
                    userId = userId, // 실제 사용자의 ID로 변경 필요
                    roomId = roomId, // Intent에서 가져온 roomId 사용
                    recordId = recordId, // position이 아닌 생성된 recordId 사용
                    isUser = if (chatItem.isSentByMe) 1 else 0,
                    chatContext = chatItem.message,
                    chatSent = sentiment // 감정 분석 결과에 따라 chatSent 설정
                )

                // 리스트에 추가
                chatRequestList.add(chatRequest)
            }
        }

        // 전체 대화 내용을 한 번의 API 요청으로 서버에 전송
        sendChatRequests(chatRequestList)

        // JSON 형식으로 변환하여 로그 출력
        val gson = Gson()
        val json = gson.toJson(chatRequestList)
        Log.d("ChatRequest", json)
    }


    private fun sendChatRequests(chatRequestList: List<ChatRequest>) {
        val service = RetrofitApi.getRetrofitService
        val call = service.chatPost(chatRequestList)

        call.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    val chatResponses = response.body()
                    Log.d("ChatPost", "채팅 저장 성공: $chatResponses")
                    // 성공 처리 로직 추가
                } else {
                    Log.e("ChatPost", "채팅 저장 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    // 실패 처리 로직 추가
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e("ChatPost", "채팅 저장 요청 실패", t)
                // 네트워크 오류 등 요청 실패 시 처리 로직 추가
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

    override fun onDestroy() {
        super.onDestroy()
        //logAllConversations() // 액티비티 종료 시 대화 내용 로그로 출력

        // logAllConversations 후에 roomPatchSentiment 및 recordPatch 실행
        logAllConversationsAndPatch()
    }

    override fun onPause() {
        super.onPause()
        //logAllConversations() // 액티비티 일시 중지 시 대화 내용 로그로 출력

        // logAllConversations 후에 roomPatchSentiment 및 recordPatch 실행
        //logAllConversationsAndPatch()
    }
    // room을 생성하고자 할 때 사용하는 코드
    // TODO: room 생성은했는데, 이제 patch가 안되는 문제가 발생함요
    private fun roomPatchSentiment(roomId: Int, roomSent: Int){
        val request = RoomSentPatchRequest(
            roomId = roomId,
            roomSent = roomSent
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.roomPatch(request)

        call.enqueue(object: Callback<RoomAddRespose>{
            override fun onResponse(
                call: Call<RoomAddRespose>,
                response: Response<RoomAddRespose>
            ) {
                if (response.isSuccessful) {
                    // API 요청이 성공한 경우
                    val patchResponse = response.body()
                    Log.d("RoomPatch", "Patch 성공: $patchResponse")
                    // 성공 처리 로직 추가
                } else {
                    // 응답이 성공적이지 않은 경우 (예: 서버 에러)
                    Log.e("RoomPatch", "Patch 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    // 실패 처리 로직 추가
                }
            }

            override fun onFailure(call: Call<RoomAddRespose>, t: Throwable) {
                // 네트워크 요청이 실패한 경우 (예: 네트워크 오류)
                Log.e("RoomPatch", "Patch 요청 실패", t)
            }
        })
    }

    private fun logAllConversationsAndPatch() {
        logAllConversations()

        val roomId = intent.getIntExtra("roomId", -1)
        val finalSentiment = emotionDataList.lastOrNull()?.sentiment_idx ?: 0

        // RoomSentiment를 Patch
        roomPatchSentiment(roomId, finalSentiment) {
            // RoomSentiment Patch가 성공적으로 완료된 후 recordPatch 실행
            emotionDataList.forEachIndexed { index, emotionResult ->
                patchRecord(index + 1, emotionResult.sentiment_idx, emotionResult.sentence)
            }
        }
    }

    private fun roomPatchSentiment(roomId: Int, roomSent: Int, onSuccess: () -> Unit) {
        val request = RoomSentPatchRequest(
            roomId = roomId,
            roomSent = roomSent
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.roomPatch(request)

        call.enqueue(object : Callback<RoomAddRespose> {
            override fun onResponse(
                call: Call<RoomAddRespose>,
                response: Response<RoomAddRespose>
            ) {
                if (response.isSuccessful) {
                    val patchResponse = response.body()
                    Log.d("RoomPatch", "Patch 성공: $patchResponse")
                    onSuccess() // 성공 시, recordPatch를 호출
                } else {
                    Log.e("RoomPatch", "Patch 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RoomAddRespose>, t: Throwable) {
                Log.e("RoomPatch", "Patch 요청 실패", t)
            }
        })
    }

    private fun patchRecord(recordId: Int, sentiment: Int, sentence: String) {
        val patchRequest = RecordPatchRequest(
            recordId = recordId,
            recordSent = sentiment,
            recordSummary = sentence
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.recordPatch(patchRequest)

        call.enqueue(object : Callback<RecordPatchResponse> {
            override fun onResponse(
                call: Call<RecordPatchResponse>,
                response: Response<RecordPatchResponse>
            ) {
                if (response.isSuccessful) {
                    val patchResponse = response.body()
                    Log.d("RecordPatch", "Patch 성공: ${patchResponse?.data}")
                } else {
                    Log.e("RecordPatch", "Patch 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RecordPatchResponse>, t: Throwable) {
                Log.e("RecordPatch", "Patch 호출 실패", t)
            }
        })
    }

    private fun getUserId(): Int {
        val sharedPref = getSharedPreferences("userData", Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_ID", -1) // 기본값 -1
    }

    private fun postRecordForEmotion(roomId: Int, emotionResult: EmotionResult, position: Int) {
        val request = RecordPostRequest(
            roomId = roomId
        )

        val service = RetrofitApi.getRetrofitService
        val call = service.recordPost(request)

        call.enqueue(object : Callback<RecordPostResponse> {
            override fun onResponse(call: Call<RecordPostResponse>, response: Response<RecordPostResponse>) {
                if (response.isSuccessful) {
                    val recordResponse = response.body()
                    recordResponse?.let {
                        Log.d("RecordPost", "Record 생성 성공: ${it.data}")

                        // 생성된 recordId를 recordIdMap에 저장
                        recordIdMap[position] = it.data.recordId

                        // 생성된 recordId로 recordPatch를 통해 sentiment와 summary를 설정
                        patchRecord(it.data.recordId, emotionResult.sentiment_idx, emotionResult.sentence)
                    }
                } else {
                    Log.e("RecordPost", "Record 생성 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RecordPostResponse>, t: Throwable) {
                Log.e("RecordPost", "Record 생성 실패", t)
            }
        })
    }
}
