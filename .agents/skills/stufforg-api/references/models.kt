package com.stufforg.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// User & Auth Models
// ==========================================

@Serializable
data class UserOut(
    val id: Int,
    val username: String,
    val email: String,
    @SerialName("last_login") val lastLogin: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("delete_at") val deleteAt: String? = null
)

@Serializable
data class AccessTokenOut(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserOut,
    /**
     * Stored in Android EncryptedSharedPreferences or Jetpack DataStore.
     * Returned specifically for mobile clients unable to read httpOnly cookies.
     */
    @SerialName("refresh_token") val refreshToken: String? = null
)

@Serializable
data class UserCreate(
    val username: String,
    val email: String,
    val password: String,
    @SerialName("repeat_password") val repeatPassword: String
)

@Serializable
data class UserLogin(
    val username: String? = null,
    val email: String? = null,
    val password: String,
    @SerialName("save_password") val savePassword: Boolean = true
)

@Serializable
data class RefreshTokenIn(
    @SerialName("refresh_token") val refreshToken: String?
)

@Serializable
data class GoogleToken(
    @SerialName("google_token") val googleToken: String
)

@Serializable
data class AppleToken(
    @SerialName("apple_token") val appleToken: String
)

@Serializable
data class EmailForVerification(
    val email: String
)

@Serializable
data class EmailForVerificationResponse(
    val email: String,
    val message: String
)

// ==========================================
// Box Models
// ==========================================

@Serializable
data class ImageMetadata(
    val url: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class BoxOut(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    val name: String,
    val location: String? = null,
    val description: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("qr_code_data") val qrCodeData: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    val items: List<ItemOut> = emptyList()
)

@Serializable
data class BoxesOut(
    val boxes: List<BoxOut>
)

@Serializable
data class UserCreateBox(
    val name: String,
    val location: String? = null,
    val description: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("listOfItems") val listOfItems: List<ItemBase>? = emptyList(),
    val image: ImageMetadata? = null
)

// ==========================================
// Item Models
// ==========================================

@Serializable
data class ItemBase(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val amount: Int = 1
)

@Serializable
data class ItemOut(
    val id: Int,
    @SerialName("box_id") val boxId: Int,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val amount: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ItemTransfer(
    @SerialName("item_id") val itemId: Int,
    @SerialName("target_box_id") val targetBoxId: Int
)

@Serializable
data class ItemDetectionOut(
    val name: String,
    val confidence: Double,
    val quantity: Int
)

@Serializable
data class DatabaseHealthOut(
    val status: String,
    @SerialName("users_in_db") val usersInDb: Int
)

