package stufforg.capstone.frontend.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
@Serializable
data class UserOut(
    val id: Int,
    val username: String,
    val email: String,
    @SerialName("last_login") val lastLogin: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("delete_at") val deleteAt: String? = null,
)

@Immutable
@Serializable
data class AccessTokenOut(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserOut,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
data class UserCreate(
    val username: String,
    val email: String,
    val password: String,
    @SerialName("repeat_password") val repeatPassword: String,
)

@Serializable
data class UserLogin(
    val username: String? = null,
    val email: String? = null,
    val password: String,
    @SerialName("save_password") val savePassword: Boolean = true,
)

@Serializable
data class RefreshTokenIn(
    @SerialName("refresh_token") val refreshToken: String?,
)

@Serializable
data class GoogleToken(
    @SerialName("google_token") val googleToken: String,
)

@Immutable
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
    @SerialName("created_at") val createdAt: String = "",
    val items: List<ItemOut> = emptyList(),
) {
    val boxTag: String
        get() = "#${id.toString().padStart(2, '0')}"

    val hasUploadedImage: Boolean
        get() = !imageUrl.isNullOrBlank() && !imageUrl.endsWith("/default.png")

    val totalUnits: Int
        get() = items.sumOf { it.amount }
}

@Serializable
data class BoxesOut(
    val boxes: List<BoxOut>,
)

@Serializable
data class UserCreateBox(
    val name: String,
    val location: String? = null,
    val description: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("listOfItems") val listOfItems: List<ItemBase>? = null,
)

@Immutable
@Serializable
data class ItemBase(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val amount: Int = 1,
)

@Immutable
@Serializable
data class ItemOut(
    val id: Int,
    @SerialName("box_id") val boxId: Int,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val amount: Int = 1,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ListOfItems(
    val listOfItems: List<Int>,
)

@Serializable
data class ItemTransfer(
    @SerialName("item_id") val itemId: Int,
    @SerialName("target_box_id") val targetBoxId: Int,
)

@Immutable
@Serializable
data class ItemDetectionOut(
    val name: String,
    val confidence: Double,
    val quantity: Int,
)

@Immutable
@Serializable
data class DatabaseHealthOut(
    val status: String,
    @SerialName("users_in_db") val usersInDb: Int = 0,
)

@Immutable
data class ItemWithBox(
    val item: ItemOut,
    val boxTag: String,
    val boxName: String,
    val boxLocation: String?,
    val boxColorCode: String?,
)

private val DisplayDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

fun formatIsoDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return runCatching { OffsetDateTime.parse(raw).format(DisplayDateFormatter) }
        .recoverCatching { LocalDateTime.parse(raw).format(DisplayDateFormatter) }
        .recoverCatching { LocalDate.parse(raw.take(10)).format(DisplayDateFormatter) }
        .getOrDefault(raw.take(10))
}

