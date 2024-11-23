package com.example.dou

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import org.checkerframework.checker.index.qual.SearchIndexFor
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
//    @SerializedName("classes")
//    val classes: List<Double>,
//    @SerializedName("sentence")
//    val sentence: String,
//    @SerializedName("sentiment")
//    val sentiment: Int,
    @SerializedName("sentence")
    val sentence: String,
    @SerializedName("sentiment")
    val sentiment:String,
    @SerializedName("sentiment_idx")
    val sentiment_idx: Int
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
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("roomSent")
    val roomSent: Int,
    @SerializedName("roomDate")
    val roomDate: String,
    @SerializedName("updatedAt")
    val createdAt: String,
    @SerializedName("roomId")
    val roomId: Int
)

data class RoomSentPatchRequest(
    @SerializedName("roomId")
    val roomId : Int,
    @SerializedName("roomSent")
    val roomSent: Int
)

data class ChatRequest(
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("roomId")
    val roomId: Int,
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("isUser")
    val isUser: Int,
    @SerializedName("chatContext")
    val chatContext: String,
    @SerializedName("chatSent")
    val chatSent: Int
)

data class ChatResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<ChatResult>
)

data class ChatResult(
    @SerializedName("chatId")
    val chatId: Int,
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("createdAt")
    val createdAt: String
)

data class ChatGetResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<ChatGetData>,
    @SerializedName("cursorId")
    val cursorId: Int
)

data class ChatGetData(
    @SerializedName("chatId")
    val chatId: Int,
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("isUser")
    val isUser: Int,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("chatSent")
    val chatSent: Int,
    @SerializedName("chatContext")
    val chatContext: String,
)

data class RoomListResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<RoomListData>,
    @SerializedName("cursorId")
    val cursorId: Int
)

data class RoomListData(
    @SerializedName("roomId")
    val roomId: Int,
    @SerializedName("roomDate")
    val roomDate: String,
    @SerializedName("roomUserId")
    val roomUserId: Int,
    @SerializedName("roomSent")
    val roomSent: Int
)

data class RecordPatchRequest(
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("recordSent")
    val recordSent: Int,
    @SerializedName("recordSummary")
    val recordSummary: String,
)

data class RecordPatchResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: RecordPatchData
)

data class RecordPatchData(
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("recordSent")
    val recordSent: Int,
    @SerializedName("recordSummary")
    val recordSummary: String,
    @SerializedName("createdAt")
    val createdAt: String
)

data class RecordPostRequest(
    @SerializedName("roomId")
    val roomId: Int
)

data class RecordPostResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: RecordPostData
)

data class RecordPostData(
    @SerializedName("recordId")
    val recordId: Int,
    @SerializedName("roomId")
    val roomId: Int,
    @SerializedName("recordSent")
    val recordSent: Int,
    @SerializedName("recordSummary")
    val recordSummary: String,
    @SerializedName("createdAt")
    val createdAt: String
)

data class RecordGetResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<RecordGetData>, // 리스트로 변경
    @SerializedName("cursorId")
    val cursorId: Int,
)

data class RecordGetData(
    val recordId: Int,
    val roomId: Int,
    val createdAt: String,
    val recordSummary: String,
    val recordSent: Int,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(recordId)
        parcel.writeInt(roomId)
        parcel.writeInt(recordSent)
        parcel.writeString(recordSummary)
        parcel.writeString(createdAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecordGetData> {
        override fun createFromParcel(parcel: Parcel): RecordGetData {
            return RecordGetData(parcel)
        }

        override fun newArray(size: Int): Array<RecordGetData?> {
            return arrayOfNulls(size)
        }
    }
}

data class KaKaoData(
    val id: Long,
    val expires_in :Int,
    val app_id : Int
)

data class KaKaoLoginResponse(
    val status: Int,
    val code: String,
    val message: String,
    val data : KaKaoLoginData
)

data class KaKaoLoginData(
    val ok: Boolean,
    val eid_access_token: String,
    val userId: Int,
    val userNickname: String
)

data class KaKaoRefreshResponse(
    val status: Int,
    val code: String,
    val message: String,
    val data : KaKaoRefreshData
)

data class KaKaoRefreshData(
    val ok: Boolean,
    val eid_access_token: String,
)

data class RecordIdResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<RecordIdResult>
)

data class RecordIdResult(
    val recordId: Int,
    val roomId: Int,
    val createdAt: String,
    val recordSummary: String,
    val recordSent: Int,
)

data class PostUseDateResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: UseDateData
)

data class UseDateData(
    val useDate: Int
)

data class PostSentCountRequest(
    val sentCode: Int
)

data class PostSentCountData(
    val useSent: String,
    val sentCount: Int
)

data class PostSentCountResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: PostSentCountData
)