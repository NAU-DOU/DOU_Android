package com.example.dou

import com.google.gson.annotations.SerializedName
import java.io.Serial

data class SignupRequest(
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userNickname") val nickname: String,
    @SerializedName("password") val password: String,
)

data class SignupResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: SignupResult,
)

data class SignupResult(
    @SerializedName("userId")
    val userId : Int,
    @SerializedName("token")
    val token: String,
)

data class LoginRequset(
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("password") val password: String,
)

data class LoginResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: LoginResult,
)

data class LoginResult(
    @SerializedName("userId")
    val userId : Int,
    @SerializedName("userNickname")
    val userNickname : String,
    @SerializedName("token")
    val token: String,
)

data class UserResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: UserResult,
)

data class UserResult(
    @SerializedName("userId")
    val userId : Int,
    @SerializedName("userEmail")
    val userEmail: String,
    @SerializedName("userNickname")
    val userNickname: String,
    @SerializedName("userStatus")
    val userStatus : Int,
)

data class EmotionRequest(
    @SerializedName("userId")
    val userId : Int,
    @SerializedName("sentense")
    val sentence: String,
)

data class EmotionResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: EmotionData,
)

data class EmotionData(
    @SerializedName("data")
    val data: List<EmotionResult>
)

data class EmotionResult(
    @SerializedName("classes")
    val classes: List<Double>,
    @SerializedName("sentence")
    val sentence: String,
    @SerializedName("sentiment")
    val sentiment: Int,
)

data class RecordResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: RecordResult,
)

data class RecordResult(
    @SerializedName("recordId")
    val recordId: Int
)

// 기록 상세 조회
data class RecordCheckResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: Any // 다양한 유형의 데이터를 포함할 수 있도록 Any 형식으로 정의
)

// 날짜별 기록 조회
data class DateRequest(
    @SerializedName("date")
    val date: String,
)

data class DateResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: Any // 다양한 유형의 데이터를 포함할 수 있도록 Any 형식으로 정의
)

// Gpt 이용 내용 요약
data class SummaryRequest(
    @SerializedName("userId")
    val userId : Int,
    @SerializedName("context")
    val context: String,
)

data class SummaryResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: SummaryData,
)

data class SummaryData(
    @SerializedName("summary")
    val summary: String
)

// GPT와의 대화
data class GPTRequest(
    val userId: Int,
    val context: String,
    val reqType: String,
    val reqSent: String
)

data class GPTResponse(
    val status: Int,
    val code: String,
    val message: String,
    val data: GPTData
)

data class GPTData(
    val response: String,
    val positive: List<String>
)


data class ClovaSpeechResponse(
    val segments: List<Segment>
)

data class Segment(
    val text: String
)

data class RoomAddRequest(
    @SerializedName("roomUserId")
    val roomUserId : Int,
    @SerializedName("roomSent")
    val roomSent: Int
)

data class RoomAddRespose(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: RoomAddResult
)

data class RoomAddResult(
    @SerializedName("user_id")
    val user_id: Int,
    @SerializedName("room_sent")
    val room_sent: Int,
    @SerializedName("room_date")
    val room_date: String,
    @SerializedName("created_at")
    val created_at: String,
    @SerializedName("room_id")
    val room_id: Int
)

data class RoomSentPatchRequest(
    @SerializedName("roomId")
    val roomId : Int,
    @SerializedName("roomSent")
    val roomSent: Int
)
