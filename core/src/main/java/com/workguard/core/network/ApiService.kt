package com.workguard.core.network

import com.squareup.moshi.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login/employee")
    suspend fun loginEmployee(
        @Body request: LoginRequest,
        @Header("User-Agent") userAgent: String?
    ): LoginResponse

    @POST("employee/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): ApiEnvelope<DeviceRegisterResponse>

    @POST("employee/devices/login")
    suspend fun loginDevice(
        @Body request: DeviceRegisterRequest
    ): ApiEnvelope<DeviceRegisterResponse>

    @POST("employee/devices/logout")
    suspend fun logoutDevice(
        @Body request: DeviceRegisterRequest
    ): ApiEnvelope<DeviceRegisterResponse>

    @POST("tracking/ping")
    suspend fun trackingPing(
        @Body request: TrackingPingRequest
    ): ApiEnvelope<TrackingPingResponse>

    @GET("employee/news")
    suspend fun getNewsList(
        @Query("q") q: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("category_slug") categorySlug: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: String? = null
    ): ApiEnvelope<NewsListResponse>

    @GET("employee/news/featured")
    suspend fun getNewsFeatured(): ApiEnvelope<NewsItem>

    @GET("employee/news/categories")
    suspend fun getNewsCategories(): ApiEnvelope<List<NewsCategory>>

    @GET("employee/news/{id}")
    suspend fun getNewsDetail(
        @Path("id") id: Int
    ): ApiEnvelope<NewsDetail>

    @GET("employee/monitoring/latest")
    suspend fun getMonitoringLatest(): ApiEnvelope<MonitoringPoint>

    @GET("employee/monitoring/history")
    suspend fun getMonitoringHistory(
        @Query("task_id") taskId: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("days") days: Int? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<MonitoringPoint>>

    @GET("employee/monitoring/violations")
    suspend fun getMonitoringViolations(
        @Query("type") type: String? = null,
        @Query("resolved") resolved: Boolean? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<MonitoringViolation>>

    @GET("auth/profile")
    suspend fun getProfile(): ApiEnvelope<ProfileResponse>

    @GET("employee/profile")
    suspend fun getEmployeeProfile(): ApiEnvelope<EmployeeProfile>

    @GET("auth/me")
    suspend fun getMe(): ApiEnvelope<TokenContextResponse>

    @GET("public/settings")
    suspend fun getPublicSettings(
        @Query("company_code") companyCode: String?
    ): ApiEnvelope<CompanySettingsResponse>

    @GET("employee/home")
    suspend fun getEmployeeHome(): ApiEnvelope<EmployeeHomeResponse>

    @Multipart
    @POST("face-sessions/verify")
    suspend fun verifyFace(
        @Part photo: MultipartBody.Part,
        @Part("context") context: RequestBody,
        @Part("camera_source") cameraSource: RequestBody,
        @Part("camera_facing") cameraFacing: RequestBody,
        @Part("device_model") deviceModel: RequestBody? = null,
        @Part("device_manufacturer") deviceManufacturer: RequestBody? = null,
        @Part("app_version") appVersion: RequestBody? = null,
        @Part("app_version_code") appVersionCode: RequestBody? = null,
        @Part("battery_level") batteryLevel: RequestBody? = null,
        @Part("is_mock_location") isMockLocation: RequestBody? = null
    ): ApiEnvelope<FaceVerifyResponse>

    @POST("employee/attendance")
    suspend fun submitAttendance(
        @Body request: AttendanceRequest
    ): ApiEnvelope<AttendanceResponse>

    @POST("employee/attendance/check-in")
    suspend fun checkInAttendance(
        @Body request: AttendanceRequest
    ): ApiEnvelope<AttendanceResponse>

    @POST("employee/attendance/check-out")
    suspend fun checkOutAttendance(
        @Body request: AttendanceRequest
    ): ApiEnvelope<AttendanceResponse>

    @POST("employee/push/register")
    suspend fun registerPushToken(
        @Body request: PushTokenRequest
    ): ApiEnvelope<PushTokenResponse>

    @GET("employee/attendance/today")
    suspend fun getAttendanceToday(
        @Query("date") date: String? = null
    ): ApiEnvelope<AttendanceTodayResponse>

    @GET("employee/attendance/history")
    suspend fun getAttendanceHistory(
        @Query("month") month: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<AttendanceHistoryItem>>

    @GET("employee/attendance/rules")
    suspend fun getAttendanceRules(
        @Query("date") date: String? = null
    ): ApiEnvelope<AttendanceRulesResponse>

    @POST("tasks")
    suspend fun createTask(
        @Body request: TaskCreateRequest
    ): ApiEnvelope<TaskResponse>

    @PATCH("tasks/{taskId}/complete")
    suspend fun completeTask(
        @Path("taskId") taskId: String,
        @Body request: TaskCompleteRequest
    ): ApiEnvelope<TaskResponse>

    @Multipart
    @POST("tasks/{taskId}/media")
    suspend fun uploadTaskMedia(
        @Path("taskId") taskId: String,
        @Part photo: MultipartBody.Part,
        @Part("face_session_id") faceSessionId: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("accuracy") accuracy: RequestBody? = null,
        @Part("camera_source") cameraSource: RequestBody,
        @Part("camera_facing") cameraFacing: RequestBody,
        @Part("is_mock_location") isMockLocation: RequestBody? = null
    ): ApiEnvelope<TaskMediaResponse>

    @POST("patrol/session/start")
    suspend fun startPatrolSession(
        @Body request: PatrolSessionStartRequest
    ): ApiEnvelope<PatrolSessionResponse>

    @GET("patrol/points")
    suspend fun getPatrolPoints(): ApiEnvelope<List<PatrolPointResponse>>

    @POST("patrol/scan")
    suspend fun scanPatrolPoint(
        @Body request: PatrolScanRequest
    ): ApiEnvelope<PatrolScanResponse>

    @GET("employee/chat/threads")
    suspend fun getChatThreads(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): ApiEnvelope<ChatThreadListResponse>

    @GET("employee/chat/threads/{threadId}")
    suspend fun getChatThreadDetail(
        @Path("threadId") threadId: String
    ): ApiEnvelope<ChatThreadDetail>

    @GET("employee/chat/threads/{threadId}/messages")
    suspend fun getChatMessages(
        @Path("threadId") threadId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): ApiEnvelope<ChatMessageListResponse>

    @POST("employee/chat/threads/{threadId}/messages")
    suspend fun sendChatMessage(
        @Path("threadId") threadId: String,
        @Body request: ChatMessageRequest
    ): ApiEnvelope<ChatMessageItem>

    @Multipart
    @POST("employee/chat/attachments")
    suspend fun uploadChatAttachment(
        @Part file: MultipartBody.Part
    ): ApiEnvelope<ChatAttachmentUploadResponse>

    @POST("employee/chat/threads/{threadId}/read")
    suspend fun markChatRead(
        @Path("threadId") threadId: String,
        @Body request: ChatReadRequest
    ): ApiEnvelope<Any>

    @POST("employee/chat/threads/{threadId}/typing")
    suspend fun setChatTyping(
        @Path("threadId") threadId: String,
        @Body request: ChatTypingRequest
    ): ApiEnvelope<Any>

    @POST("employee/chat/threads/{threadId}/calls")
    suspend fun startChatCall(
        @Path("threadId") threadId: String,
        @Body request: ChatCallStartRequest
    ): ApiEnvelope<ChatCallResponse>

    @POST("employee/chat/calls/{callId}/join")
    suspend fun joinChatCall(
        @Path("callId") callId: String
    ): ApiEnvelope<ChatCallResponse>

    @POST("employee/chat/calls/{callId}/end")
    suspend fun endChatCall(
        @Path("callId") callId: String
    ): ApiEnvelope<ChatCallResponse>

    @Multipart
    @POST("employee/face-templates")
    suspend fun createFaceTemplate(
        @Part photo: MultipartBody.Part,
        @Part("slot") slot: RequestBody,
        @Part("camera_source") cameraSource: RequestBody,
        @Part("camera_facing") cameraFacing: RequestBody,
        @Part("notes") notes: RequestBody? = null
    ): ApiEnvelope<FaceTemplate>

    @GET("employee/face-templates/status")
    suspend fun getFaceTemplateStatus(): ApiEnvelope<FaceTemplateStatus>
}

data class ApiEnvelope<T>(
    val success: Boolean? = null,
    val data: T? = null,
    val message: String? = null
)

data class LoginRequest(
    @Json(name = "employee_code")
    val employeeCode: String,
    @Json(name = "password")
    val password: String,
    @Json(name = "company_code")
    val companyCode: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val data: LoginResponseData? = null,
    val message: String? = null
)

data class LoginResponseData(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "expires_in")
    val expiresIn: String,
    val user: LoginUser
)

data class LoginUser(
    val id: Int,
    val username: String,
    val role: String,
    @Json(name = "employee_id")
    val employeeId: Int? = null,
    @Json(name = "company_id")
    val companyId: Int? = null
)

data class DeviceRegisterRequest(
    @Json(name = "device_id")
    val deviceId: String,
    @Json(name = "device_model")
    val deviceModel: String? = null,
    @Json(name = "os_version")
    val osVersion: String? = null,
    @Json(name = "app_version")
    val appVersion: String? = null,
    @Json(name = "app_version_code")
    val appVersionCode: Long? = null
)

data class DeviceRegisterResponse(
    val id: Int? = null,
    @Json(name = "device_id")
    val deviceId: String? = null
)

data class ProfileResponse(
    val username: String? = null,
    val role: String? = null,
    @Json(name = "full_name")
    val fullName: String? = null,
    @Json(name = "employee_photo_url")
    val employeePhotoUrl: String? = null,
    @Json(name = "photo_url")
    val photoUrl: String? = null
)

data class TokenContextResponse(
    @Json(name = "employee_id")
    val employeeId: Int? = null,
    @Json(name = "company_id")
    val companyId: Int? = null
)

data class CompanySettingsResponse(
    @Json(name = "company_name")
    val companyName: String? = null,
    val name: String? = null,
    @Json(name = "logo_url")
    val logoUrl: String? = null,
    val logo: String? = null
)

data class TrackingPingRequest(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "accuracy")
    val accuracy: Float? = null,
    @Json(name = "is_mock_location")
    val isMockLocation: Boolean? = null,
    @Json(name = "battery_level")
    val batteryLevel: Int? = null,
    @Json(name = "is_charging")
    val isCharging: Boolean? = null
)

data class TrackingPingResponse(
    val ok: Boolean? = null,
    @Json(name = "task_id")
    val taskId: Int? = null
)

data class MonitoringPoint(
    val id: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "accuracy_meters")
    val accuracyMeters: Float? = null,
    @Json(name = "accuracy")
    val accuracy: Float? = null,
    @Json(name = "task_id")
    val taskId: Int? = null,
    val provider: String? = null,
    @Json(name = "is_mocked")
    val isMocked: Boolean? = null,
    @Json(name = "recorded_at")
    val recordedAt: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

data class MonitoringViolation(
    val id: Int? = null,
    val type: String? = null,
    val message: String? = null,
    val resolved: Boolean? = null,
    @Json(name = "task_id")
    val taskId: Int? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "resolved_at")
    val resolvedAt: String? = null
)
