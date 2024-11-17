package com.example.dou

import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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
    fun emotion(@Body request: EmotionRequest): Single<EmotionResponse>

    // 기록 등록(저장)API
//    @POST("/record/register")
//    fun recordSave(@Body request: EmotionRequest): Call<RecordResponse>
    @PATCH("/record")
    fun recordPatch(@Body request: RecordPatchRequest) : Call<RecordPatchResponse>

    @POST("/record")
    fun recordPost(@Body request: RecordPostRequest) : Call<RecordPostResponse>

    @GET("/record/room")
    fun getRecordsByRoomId(
        @Query("roomId") roomId: Int,
        @Query("cursorId") cursorId: Int,
        @Query("limit") limit: Int,
    ): Call<RecordGetResponse>

    @GET("/record/{recordId}")
    fun getRecordsByRecordId(
        @Path("recordId") recordId: Int,
    ): Call<RecordIdResponse>

    // 기록 상세 조회
    @GET("/record/{identifyId}")
    fun recordCheck(@Path("identifyId") identifyId: Int?) : Call<RecordCheckResponse>

    // 날짜별 기록 조회
    @GET("record/date")
    fun recordDate(@Query("date") date: DateRequest): Call<DateResponse>

    @GET("/chat")
    fun getChat(
        @Query("recordId") recordId: Int,
        @Query("cursorId") cursorId: Int,
        @Query("limit") limit: Int
    ): Call<ChatGetResponse>

    // GPT 이용 Summary
    @POST("/gpt/summary")
    fun summary(@Body request: SummaryRequest): Call<SummaryResponse>

    @POST("/gpt")
    fun getGPTResponse(@Body request: GPTRequest): Single<GPTResponse>

    @POST("/room")
    fun roomAdd(@Body request: RoomAddRequest) : Call<RoomAddRespose>

    @PATCH("/room")
    fun roomPatch(@Body request: RoomSentPatchRequest) : Call<RoomAddRespose>

    @POST("/chat")
    fun chatPost(@Body request: List<ChatRequest>) : Call<ChatResponse>

    @GET("/room/user")
    fun getAllRooms(
        @Query("userId") userId: Int,
        @Query("cursorId") cursorId: Int,
        @Query("limit") limit: Int) :Call<RoomListResponse>

    @GET("/room/date")
    fun getRoomDate(
        @Query("date") date: String,
        @Query("cursorId") cursorId: Int,
        @Query("limit") limit: Int)
    : Call<RoomListResponse>

    @GET("/oauth/kakao")
    fun getKakao(): Call<ResponseBody>

    @GET("/oauth/kakao/callback")
    fun getKakaoToken(): Call<KaKaoLoginResponse>

    @POST("/oauth/kakao/refresh")
    fun postRefreshToken(
        @Header("Authorization") token: String,
        @Header("cookie") refreshTokenCookie: String
    ): Call<KaKaoRefreshResponse>

    @POST("/oauth/kakao/logout")
    fun kakaoLogout(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    @GET("user/access_token_info")
    fun getkakaoInfo(
        @Header("Authorization") token: String
    ):Call<KaKaoData>

    @GET
    fun getJsonResponse(@Url url: String): Call<ResponseBody>
}