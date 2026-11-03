package stufforg.capstone.frontend.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.DatabaseHealthOut
import stufforg.capstone.frontend.core.model.GoogleToken
import stufforg.capstone.frontend.core.model.ItemBase
import stufforg.capstone.frontend.core.model.ItemDetectionOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.ItemTransfer
import stufforg.capstone.frontend.core.model.ListOfItems
import stufforg.capstone.frontend.core.model.UserCreate
import stufforg.capstone.frontend.core.model.UserCreateBox
import stufforg.capstone.frontend.core.model.UserLogin
import stufforg.capstone.frontend.core.model.UserOut
import stufforg.capstone.frontend.core.network.MultipartHelpers
import stufforg.capstone.frontend.core.network.SessionState
import stufforg.capstone.frontend.core.network.SessionStore
import stufforg.capstone.frontend.core.network.StuffOrgApiService
import java.io.File

class StuffOrgRepository(
    private val api: StuffOrgApiService,
    private val sessionStore: SessionStore,
    private val json: Json,
) {
    val sessionFlow: Flow<SessionState> = sessionStore.sessionFlow

    private val _boxes = MutableStateFlow<List<BoxOut>>(emptyList())
    val boxes: StateFlow<List<BoxOut>> = _boxes.asStateFlow()

    suspend fun completeOnboarding() {
        sessionStore.completeOnboarding()
    }

    suspend fun login(
        identifier: String,
        password: String,
        savePassword: Boolean = true,
    ): Result<UserOut> = withContext(Dispatchers.IO) {
        runApiCall {
            val trimmed = identifier.trim()
            val body = if ("@" in trimmed) {
                UserLogin(email = trimmed, password = password, savePassword = savePassword)
            } else {
                UserLogin(username = trimmed, password = password, savePassword = savePassword)
            }
            api.login(body)
        }.mapCatching { tokenOut ->
            sessionStore.saveAuth(tokenOut)
            tokenOut.user
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        repeatPassword: String,
    ): Result<UserOut> = withContext(Dispatchers.IO) {
        runApiCall {
            api.register(
                UserCreate(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    repeatPassword = repeatPassword,
                ),
            )
        }.mapCatching { tokenOut ->
            sessionStore.saveAuth(tokenOut)
            tokenOut.user
        }
    }

    suspend fun loginWithGoogle(googleIdToken: String): Result<UserOut> =
        withContext(Dispatchers.IO) {
            runApiCall {
                api.loginWithGoogle(GoogleToken(googleToken = googleIdToken))
            }.mapCatching { tokenOut ->
                sessionStore.saveAuth(tokenOut)
                tokenOut.user
            }
        }

    suspend fun logout() {
        _boxes.value = emptyList()
        sessionStore.clearAuth()
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        runUnitCall { api.deleteAccount() }.onSuccess {
            _boxes.value = emptyList()
            sessionStore.clearAuth()
        }
    }

    suspend fun refreshBoxes(): Result<List<BoxOut>> = withContext(Dispatchers.IO) {
        runApiCall { api.getBoxes() }.map { out ->
            val sorted = out.boxes.sortedByDescending { it.id }
            _boxes.value = sorted
            sorted
        }
    }

    suspend fun fetchBox(boxId: Int): Result<BoxOut> = withContext(Dispatchers.IO) {
        runApiCall { api.getBox(boxId) }.onSuccess { fetched ->
            upsertBoxInMemory(fetched)
        }
    }

    suspend fun createBox(
        name: String,
        location: String?,
        description: String?,
        colorCode: String?,
        items: List<ItemBase>,
        imageFile: File?,
        autoGenerateCloudQr: Boolean,
    ): Result<BoxOut> = withContext(Dispatchers.IO) {
        val payload = UserCreateBox(
            name = name.trim(),
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            colorCode = colorCode,
            listOfItems = items.filter { it.name.isNotBlank() },
        )
        val boxPart = MultipartHelpers.createBoxRequestBody(payload, json)
        val imagePart = imageFile?.let { MultipartHelpers.createImagePart(it, partName = "image") }

        runApiCall { api.createBox(box = boxPart, image = imagePart) }.map { created ->
            val finalBox = if (autoGenerateCloudQr) {
                runApiCall { api.generateQrCode(created.id) }.getOrDefault(created)
            } else {
                created
            }
            upsertBoxInMemory(finalBox)
            finalBox
        }
    }

    suspend fun updateBox(
        boxId: Int,
        name: String,
        location: String?,
        description: String?,
        colorCode: String?,
    ): Result<BoxOut> = withContext(Dispatchers.IO) {
        // listOfItems = null ensures backend preserves all existing items in the box
        val payload = UserCreateBox(
            name = name.trim(),
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            colorCode = colorCode,
            listOfItems = null,
        )
        runApiCall { api.updateBox(boxId, payload) }.onSuccess { updated ->
            upsertBoxInMemory(updated)
        }
    }

    suspend fun updateBoxImage(boxId: Int, imageFile: File): Result<BoxOut> =
        withContext(Dispatchers.IO) {
            val part = MultipartHelpers.createImagePart(imageFile, partName = "image")
            runApiCall { api.updateBoxImage(boxId, part) }.onSuccess { updated ->
                upsertBoxInMemory(updated)
            }
        }

    suspend fun deleteBoxImage(boxId: Int): Result<BoxOut> = withContext(Dispatchers.IO) {
        runApiCall { api.deleteBoxImage(boxId) }.onSuccess { updated ->
            upsertBoxInMemory(updated)
        }
    }

    suspend fun deleteBox(boxId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runUnitCall { api.deleteBox(boxId) }.onSuccess {
            _boxes.update { list -> list.filterNot { it.id == boxId } }
        }
    }

    suspend fun deleteAllBoxes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = api.deleteAllBoxes()
            if (resp.isSuccessful || resp.code() == 404) {
                _boxes.value = emptyList()
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(resp)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error"))
        }
    }

    suspend fun generateCloudQrCode(boxId: Int): Result<BoxOut> = withContext(Dispatchers.IO) {
        runApiCall { api.generateQrCode(boxId) }.onSuccess { updated ->
            upsertBoxInMemory(updated)
        }
    }

    suspend fun addItem(boxId: Int, item: ItemBase): Result<ItemOut> = withContext(Dispatchers.IO) {
        val cleaned = item.copy(
            name = item.name.trim(),
            description = item.description?.trim()?.takeIf { it.isNotEmpty() },
            category = item.category?.trim()?.takeIf { it.isNotEmpty() },
            amount = item.amount.coerceAtLeast(1),
        )
        runApiCall { api.addItem(boxId, cleaned) }.onSuccess { created ->
            _boxes.update { list ->
                list.map { box ->
                    if (box.id == boxId) box.copy(items = box.items + created) else box
                }
            }
        }
    }

    suspend fun fetchItem(itemId: Int): Result<ItemOut> = withContext(Dispatchers.IO) {
        runApiCall { api.getItem(itemId) }.onSuccess { fetched ->
            replaceItemInMemory(fetched)
        }
    }

    suspend fun updateItem(itemId: Int, item: ItemBase): Result<ItemOut> =
        withContext(Dispatchers.IO) {
            val cleaned = item.copy(
                name = item.name.trim(),
                description = item.description?.trim()?.takeIf { it.isNotEmpty() },
                category = item.category?.trim()?.takeIf { it.isNotEmpty() },
                amount = item.amount.coerceAtLeast(1),
            )
            runApiCall { api.updateItem(itemId, cleaned) }.onSuccess { updated ->
                replaceItemInMemory(updated)
            }
        }

    suspend fun updateItemQuantity(item: ItemOut, newAmount: Int): Result<ItemOut> {
        val clamped = newAmount.coerceAtLeast(1)
        val optimistic = item.copy(amount = clamped)
        replaceItemInMemory(optimistic)
        return updateItem(
            itemId = item.id,
            item = ItemBase(
                name = item.name,
                description = item.description,
                category = item.category,
                amount = clamped,
            ),
        ).onFailure {
            replaceItemInMemory(item)
        }
    }

    suspend fun deleteItem(itemId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runUnitCall { api.deleteItem(itemId) }.onSuccess {
            _boxes.update { list ->
                list.map { box ->
                    box.copy(items = box.items.filterNot { it.id == itemId })
                }
            }
        }
    }

    suspend fun deleteItemsInBox(boxId: Int, itemIds: List<Int>): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (itemIds.isEmpty()) return@withContext Result.success(Unit)
            runUnitCall {
                api.deleteItemsInBox(boxId, ListOfItems(listOfItems = itemIds))
            }.onSuccess {
                val idSet = itemIds.toSet()
                _boxes.update { list ->
                    list.map { box ->
                        if (box.id == boxId) {
                            box.copy(items = box.items.filterNot { it.id in idSet })
                        } else {
                            box
                        }
                    }
                }
            }
        }

    suspend fun moveItem(itemId: Int, targetBoxId: Int): Result<ItemOut> =
        withContext(Dispatchers.IO) {
            runApiCall {
                api.moveItem(ItemTransfer(itemId = itemId, targetBoxId = targetBoxId))
            }.onSuccess { moved ->
                _boxes.update { list ->
                    list.map { box ->
                        val without = box.items.filterNot { it.id == itemId }
                        if (box.id == targetBoxId) {
                            box.copy(items = without + moved)
                        } else {
                            box.copy(items = without)
                        }
                    }
                }
            }
        }

    suspend fun moveMultipleItems(itemIds: List<Int>, targetBoxId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (itemIds.isEmpty()) return@withContext Result.success(Unit)
            val transfers = itemIds.map { ItemTransfer(itemId = it, targetBoxId = targetBoxId) }
            runUnitCall { api.moveMultipleItems(transfers) }.onSuccess {
                refreshBoxes()
            }
        }

    suspend fun detectItems(imageFile: File): Result<List<ItemDetectionOut>> =
        withContext(Dispatchers.IO) {
            try {
                var resp = api.detectItems(MultipartHelpers.createImagePart(imageFile, "image"))
                if (resp.code() == 422) {
                    resp = api.detectItems(MultipartHelpers.createImagePart(imageFile, "file"))
                }
                if (resp.isSuccessful) {
                    val detected = resp.body().orEmpty().filter {
                        it.quantity > 0 && !it.name.equals("No items detected", ignoreCase = true)
                    }
                    Result.success(detected)
                } else {
                    Result.failure(Exception(parseErrorMessage(resp)))
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "AI detection request failed"))
            }
        }

    suspend fun checkHealth(): Result<DatabaseHealthOut> = withContext(Dispatchers.IO) {
        runApiCall { api.checkHealth() }
    }

    private fun upsertBoxInMemory(box: BoxOut) {
        _boxes.update { current ->
            val exists = current.any { it.id == box.id }
            if (exists) {
                current.map { if (it.id == box.id) box else it }
            } else {
                listOf(box) + current
            }
        }
    }

    private fun replaceItemInMemory(updated: ItemOut) {
        _boxes.update { list ->
            list.map { box ->
                if (box.id == updated.boxId || box.items.any { it.id == updated.id }) {
                    box.copy(
                        items = box.items.map { if (it.id == updated.id) updated else it },
                    )
                } else {
                    box
                }
            }
        }
    }

    private inline fun <T> runApiCall(block: () -> Response<T>): Result<T> = try {
        val resp = block()
        val body = resp.body()
        if (resp.isSuccessful && body != null) {
            Result.success(body)
        } else {
            Result.failure(Exception(parseErrorMessage(resp)))
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.localizedMessage ?: "Network error"))
    }

    private inline fun runUnitCall(block: () -> Response<Unit>): Result<Unit> = try {
        val resp = block()
        if (resp.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(parseErrorMessage(resp)))
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.localizedMessage ?: "Network error"))
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        if (!raw.isNullOrBlank()) {
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject
                when (val detail = root["detail"]) {
                    is JsonPrimitive -> return detail.content
                    is JsonArray -> {
                        val first = detail.firstOrNull()
                        if (first is JsonObject) {
                            first["msg"]?.jsonPrimitive?.content?.let { return it }
                        }
                    }
                    else -> Unit
                }
            }
        }
        return "Request failed (${response.code()})"
    }
}

