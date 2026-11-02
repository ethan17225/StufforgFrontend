package stufforg.capstone.frontend.core.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import stufforg.capstone.frontend.core.model.AccessTokenOut
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.BoxesOut
import stufforg.capstone.frontend.core.model.DatabaseHealthOut
import stufforg.capstone.frontend.core.model.GoogleToken
import stufforg.capstone.frontend.core.model.ItemBase
import stufforg.capstone.frontend.core.model.ItemDetectionOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.ItemTransfer
import stufforg.capstone.frontend.core.model.ListOfItems
import stufforg.capstone.frontend.core.model.RefreshTokenIn
import stufforg.capstone.frontend.core.model.UserCreate
import stufforg.capstone.frontend.core.model.UserCreateBox
import stufforg.capstone.frontend.core.model.UserLogin

interface StuffOrgApiService {

    @POST("users/")
    suspend fun register(@Body body: UserCreate): Response<AccessTokenOut>

    @POST("login")
    suspend fun login(@Body body: UserLogin): Response<AccessTokenOut>

    @POST("google_oidc_login")
    suspend fun loginWithGoogle(@Body body: GoogleToken): Response<AccessTokenOut>

    @POST("refresh")
    suspend fun refreshToken(@Body body: RefreshTokenIn): Response<AccessTokenOut>

    @DELETE("delete_account")
    suspend fun deleteAccount(): Response<Unit>

    @GET("boxes/")
    suspend fun getBoxes(): Response<BoxesOut>

    @GET("boxes/{box_id}")
    suspend fun getBox(@Path("box_id") boxId: Int): Response<BoxOut>

    @Multipart
    @POST("boxes/")
    suspend fun createBox(
        @Part("box") box: RequestBody,
        @Part image: MultipartBody.Part? = null,
    ): Response<BoxOut>

    @PUT("boxes/{box_id}")
    suspend fun updateBox(
        @Path("box_id") boxId: Int,
        @Body body: UserCreateBox,
    ): Response<BoxOut>

    @DELETE("deleteBoxById/{box_id}")
    suspend fun deleteBox(@Path("box_id") boxId: Int): Response<Unit>

    @DELETE("deleteAllBoxes/")
    suspend fun deleteAllBoxes(): Response<Unit>

    @POST("generate_qrcode/{box_id}")
    suspend fun generateQrCode(@Path("box_id") boxId: Int): Response<BoxOut>

    @POST("items/{box_id}")
    suspend fun addItem(
        @Path("box_id") boxId: Int,
        @Body body: ItemBase,
    ): Response<ItemOut>

    @GET("items/{item_id}")
    suspend fun getItem(@Path("item_id") itemId: Int): Response<ItemOut>

    @PUT("items/{item_id}")
    suspend fun updateItem(
        @Path("item_id") itemId: Int,
        @Body body: ItemBase,
    ): Response<ItemOut>

    @DELETE("items/{item_id}")
    suspend fun deleteItem(@Path("item_id") itemId: Int): Response<Unit>

    @HTTP(method = "DELETE", path = "boxes/{box_id}/items", hasBody = true)
    suspend fun deleteItemsInBox(
        @Path("box_id") boxId: Int,
        @Body body: ListOfItems,
    ): Response<Unit>

    @PATCH("items/moveone")
    suspend fun moveItem(@Body body: ItemTransfer): Response<ItemOut>

    @PATCH("items/movemultiple")
    suspend fun moveMultipleItems(@Body body: List<ItemTransfer>): Response<Unit>

    @Multipart
    @POST("item_detection")
    suspend fun detectItems(@Part image: MultipartBody.Part): Response<List<ItemDetectionOut>>

    @GET("test-db")
    suspend fun checkHealth(): Response<DatabaseHealthOut>
}

