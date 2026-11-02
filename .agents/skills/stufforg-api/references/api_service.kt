package com.stufforg.data.network

import com.stufforg.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit Interface for StuffOrg API
 * Note: Base URL MUST end with a slash: "http://10.0.2.2:8000/api/"
 */
interface StuffOrgApiService {

    // ==========================================
    // Authentication & User Management
    // ==========================================

    @POST("users/")
    suspend fun register(
        @Body body: UserCreate
    ): Response<AccessTokenOut>

    @POST("login")
    suspend fun login(
        @Body body: UserLogin
    ): Response<AccessTokenOut>

    @POST("google_oidc_login")
    suspend fun loginWithGoogle(
        @Body body: GoogleToken
    ): Response<AccessTokenOut>

    @POST("apple_oidc_login")
    suspend fun loginWithApple(
        @Body body: AppleToken
    ): Response<AccessTokenOut>

    @POST("refresh")
    suspend fun refreshToken(
        @Body body: RefreshTokenIn
    ): Response<AccessTokenOut>

    @POST("email-exists")
    suspend fun checkEmailExists(
        @Body body: EmailForVerification
    ): Response<EmailForVerificationResponse>

    @DELETE("delete_account")
    suspend fun deleteAccount(): Response<Unit>

    // ==========================================
    // Boxes
    // ==========================================

    @GET("boxes/")
    suspend fun getBoxes(): Response<BoxesOut>

    @GET("boxes/{box_id}")
    suspend fun getBox(
        @Path("box_id") boxId: Int
    ): Response<BoxOut>

    /**
     * Create Box with optional image
     * IMPORTANT: The 'box' part MUST be sent as text/plain or application/json string
     * representing the serialized UserCreateBox JSON.
     * Example:
     *   val boxJson = Json.encodeToString(userCreateBox)
     *   val boxBody = boxJson.toRequestBody("text/plain".toMediaType())
     */
    @Multipart
    @POST("boxes/")
    suspend fun createBox(
        @Part("box") box: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<BoxOut>

    @PUT("boxes/{box_id}")
    suspend fun updateBox(
        @Path("box_id") boxId: Int,
        @Body body: UserCreateBox
    ): Response<BoxOut>

    @DELETE("deleteBoxById/{box_id}")
    suspend fun deleteBox(
        @Path("box_id") boxId: Int
    ): Response<Unit>

    @DELETE("deleteAllBoxes/")
    suspend fun deleteAllBoxes(): Response<Unit>

    @POST("generate_qrcode/{box_id}")
    suspend fun generateQrCode(
        @Path("box_id") boxId: Int
    ): Response<Unit>

    // ==========================================
    // Items
    // ==========================================

    @POST("items/{box_id}")
    suspend fun addItem(
        @Path("box_id") boxId: Int,
        @Body body: ItemBase
    ): Response<ItemOut>

    @GET("items/{item_id}")
    suspend fun getItem(
        @Path("item_id") itemId: Int
    ): Response<ItemOut>

    @PUT("items/{item_id}")
    suspend fun updateItem(
        @Path("item_id") itemId: Int,
        @Body body: ItemBase
    ): Response<ItemOut>

    @DELETE("items/{item_id}")
    suspend fun deleteItem(
        @Path("item_id") itemId: Int
    ): Response<Unit>

    @DELETE("boxes/{box_id}/items")
    suspend fun deleteAllItemsInBox(
        @Path("box_id") boxId: Int
    ): Response<Unit>

    @PATCH("items/moveone")
    suspend fun moveItem(
        @Body body: ItemTransfer
    ): Response<ItemOut>

    @PATCH("items/movemultiple")
    suspend fun moveMultipleItems(
        @Body body: List<ItemTransfer>
    ): Response<Unit>

    // ==========================================
    // YOLO Object Detection
    // ==========================================

    /**
     * Upload an image to detect items using YOLO
     * The image must be passed as MultipartBody.Part with field name "file"
     */
    @Multipart
    @POST("item_detection")
    suspend fun detectItems(
        @Part file: MultipartBody.Part
    ): Response<List<ItemDetectionOut>>

    // ==========================================
    // Health
    // ==========================================

    @GET("test-db")
    suspend fun checkHealth(): Response<DatabaseHealthOut>
}

