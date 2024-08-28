package com.example.dou

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitService {

    // 회원가입
    @POST("/auth/register")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    // 로그인
    @POST("/auth/login")
    fun login(@Body request: LoginRequset): Call<LoginResponse>

    // 사용자 정보 조회
    @GET("/user/{userId}")
    fun user(@Path("userId") memberId: Int?) : Call<UserResponse>

    // 감정 분석
    @POST("/sentiment")
    fun emotion(@Body request: EmotionRequest): Call<EmotionResponse>

    // 기록 등록(저장)API
//    @POST("/record/register")
//    fun recordSave(@Body request: EmotionRequest): Call<RecordResponse>

    // 기록 상세 조회
    @GET("/record/{identifyId}")
    fun recordCheck(@Path("identifyId") identifyId: Int?) : Call<RecordCheckResponse>

    // 날짜별 기록 조회
    @GET("record/date")
    fun recordDate(@Query("date") date: DateRequest): Call<DateResponse>

    // GPT 이용 Summary
    @POST("/gpt/summary")
    fun summary(@Body request: SummaryRequest): Call<SummaryResponse>

    @POST("/gpt")
    fun getGPTResponse(@Body request: GPTRequest): Call<GPTResponse>

    @POST("/room")
    fun roomAdd(@Body request: RoomAddRequest) : Call<RoomAddRespose>

    @PATCH("/room")
    fun roomPatch(@Body request: RoomSentPatchRequest) : Call<RoomAddRespose>

    @POST("/chat")
    fun chatPost(@Body request: List<ChatRequest>) : Call<ChatResponse>

    @GET("/room/test")
    fun getAllRooms() :Call<RoomListResponse>

    @GET("/room/date")
    fun getRoomDate(@Query("date") date: String): Call<RoomListResponse>
}