package stufforg.capstone.frontend

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import stufforg.capstone.frontend.core.data.StuffOrgRepository
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
import stufforg.capstone.frontend.core.model.UserOut
import stufforg.capstone.frontend.core.network.NetworkFactory
import stufforg.capstone.frontend.core.network.SessionStore
import stufforg.capstone.frontend.core.network.StuffOrgApiService
import stufforg.capstone.frontend.core.util.QrLabelHelper

class StuffOrgRepositoryAndQrTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = NetworkFactory.createJson()

    @Test
    fun parseBoxId_handlesRawTaggedAndUrlFormats() {
        assertEquals(4, QrLabelHelper.parseBoxId("4"))
        assertEquals(4, QrLabelHelper.parseBoxId(" #04 "))
        assertEquals(4, QrLabelHelper.parseBoxId("B-04"))
        assertEquals(4, QrLabelHelper.parseBoxId("SO-BOX-04-AUD"))
        assertEquals(42, QrLabelHelper.parseBoxId("STUFFORG:BOX:42"))
        assertEquals(12, QrLabelHelper.parseBoxId("https://stufforg.app/boxes/12"))
        assertEquals(7, QrLabelHelper.parseBoxId("https://stufforg.app/qr/7"))

        assertNull(QrLabelHelper.parseBoxId(""))
        assertNull(QrLabelHelper.parseBoxId("   "))
        assertNull(QrLabelHelper.parseBoxId("0"))
        assertNull(QrLabelHelper.parseBoxId("no-digits-here"))
    }

    @Test
    fun login_routesEmailVsUsernameAndPersistsSession() = runTest {
        val fakeApi = FakeStuffOrgApiService()
        val sessionStore = SessionStore(dataStore = null, json = json)
        val repository = StuffOrgRepository(fakeApi, sessionStore, json)

        val emailResult = repository.login("alex@example.com", "Secret123!")
        assertTrue(emailResult.isSuccess)
        assertEquals("alex@example.com", fakeApi.lastLogin?.email)
        assertNull(fakeApi.lastLogin?.username)
        assertTrue(repository.sessionFlow.first().isAuthenticated)

        repository.logout()
        assertFalse(repository.sessionFlow.first().isAuthenticated)

        val userResult = repository.login("alex_morgan", "Secret123!")
        assertTrue(userResult.isSuccess)
        assertEquals("alex_morgan", fakeApi.lastLogin?.username)
        assertNull(fakeApi.lastLogin?.email)
    }

    @Test
    fun updateBox_sendsNullListOfItemsToPreserveExistingItems() = runTest {
        val fakeApi = FakeStuffOrgApiService()
        val repository = StuffOrgRepository(fakeApi, SessionStore(null, json), json)
        repository.refreshBoxes()

        val result = repository.updateBox(
            boxId = 1,
            name = "Updated Studio Cables",
            location = "Shelf C",
            description = "Balanced cables",
            colorCode = "#5B4AE4",
        )

        assertTrue(result.isSuccess)
        assertNull(fakeApi.lastUpdatedBoxPayload?.listOfItems)
        assertEquals("Updated Studio Cables", repository.boxes.value.first { it.id == 1 }.name)
    }

    @Test
    fun updateItemQuantity_rollsBackOnServerErrorAndParsesFastApiDetail() = runTest {
        val fakeApi = FakeStuffOrgApiService()
        val repository = StuffOrgRepository(fakeApi, SessionStore(null, json), json)
        repository.refreshBoxes()

        val targetItem = repository.boxes.value.first { it.id == 1 }.items.first()
        assertEquals(4, targetItem.amount)

        fakeApi.failNextUpdateItem = true
        val failed = repository.updateItemQuantity(targetItem, 9)

        assertTrue(failed.isFailure)
        assertEquals("Database write conflict", failed.exceptionOrNull()?.message)
        // Rolled back to original quantity (4)
        val afterRollback = repository.boxes.value.first { it.id == 1 }.items.first()
        assertEquals(4, afterRollback.amount)
    }

    @Test
    fun moveItem_transfersItemBetweenBoxesInState() = runTest {
        val fakeApi = FakeStuffOrgApiService()
        val repository = StuffOrgRepository(fakeApi, SessionStore(null, json), json)
        repository.refreshBoxes()

        val moved = repository.moveItem(itemId = 101, targetBoxId = 2)
        assertTrue(moved.isSuccess)

        val box1Items = repository.boxes.value.first { it.id == 1 }.items
        val box2Items = repository.boxes.value.first { it.id == 2 }.items
        assertTrue(box1Items.none { it.id == 101 })
        assertTrue(box2Items.any { it.id == 101 && it.boxId == 2 })
    }

    @Test
    fun detectItems_fallsBackToFilePartOn422AndFiltersSentinelEmptyResult() = runTest {
        val fakeApi = FakeStuffOrgApiService(rejectImagePartWith422 = true)
        val repository = StuffOrgRepository(fakeApi, SessionStore(null, json), json)
        val photo = tempFolder.newFile("scan.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val result = repository.detectItems(photo)
        assertTrue(result.isSuccess)
        assertEquals(listOf("image", "file"), fakeApi.receivedDetectionPartNames)

        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("XLR Cable", items.single().name)
        assertEquals(3, items.single().quantity)
    }

    @Test
    fun updateAndDeleteBoxImage_updatesInMemoryCache() = runTest {
        val fakeApi = FakeStuffOrgApiService()
        val repository = StuffOrgRepository(fakeApi, SessionStore(dataStore = null, json = json), json)
        repository.refreshBoxes()

        val photo = tempFolder.newFile("cover.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val uploaded = repository.updateBoxImage(1, photo).getOrThrow()
        assertTrue(uploaded.hasUploadedImage)
        assertTrue(repository.boxes.value.first { it.id == 1 }.hasUploadedImage)

        val cleared = repository.deleteBoxImage(1).getOrThrow()
        assertFalse(cleared.hasUploadedImage)
        assertFalse(repository.boxes.value.first { it.id == 1 }.hasUploadedImage)
    }
}

private class FakeStuffOrgApiService(
    private val rejectImagePartWith422: Boolean = false,
) : StuffOrgApiService {

    var lastLogin: UserLogin? = null
    var lastUpdatedBoxPayload: UserCreateBox? = null
    var failNextUpdateItem: Boolean = false
    val receivedDetectionPartNames = mutableListOf<String>()

    private val sampleUser = UserOut(
        id = 1,
        username = "alex_morgan",
        email = "alex@example.com",
        createdAt = "2026-01-15T10:00:00Z",
    )

    private val initialItem = ItemOut(
        id = 101,
        boxId = 1,
        name = "Mogami XLR Cable",
        category = "Cables",
        amount = 4,
    )

    private var boxesState = listOf(
        BoxOut(
            id = 1,
            userId = 1,
            name = "Audio & Studio Cables",
            location = "Studio Shelf B",
            items = listOf(initialItem),
        ),
        BoxOut(
            id = 2,
            userId = 1,
            name = "Spares & Adapters",
            location = "Closet",
            items = emptyList(),
        ),
    )

    override suspend fun register(body: UserCreate): Response<AccessTokenOut> =
        Response.success(
            AccessTokenOut(
                accessToken = "jwt-access",
                refreshToken = "jwt-refresh",
                user = sampleUser.copy(username = body.username, email = body.email),
            ),
        )

    override suspend fun login(body: UserLogin): Response<AccessTokenOut> {
        lastLogin = body
        return Response.success(
            AccessTokenOut(
                accessToken = "jwt-access",
                refreshToken = "jwt-refresh",
                user = sampleUser,
            ),
        )
    }

    override suspend fun loginWithGoogle(body: GoogleToken): Response<AccessTokenOut> =
        Response.success(
            AccessTokenOut(
                accessToken = "jwt-google",
                refreshToken = "jwt-refresh",
                user = sampleUser,
            ),
        )

    override suspend fun refreshToken(body: RefreshTokenIn): Response<AccessTokenOut> =
        Response.success(
            AccessTokenOut(
                accessToken = "jwt-refreshed",
                refreshToken = body.refreshToken,
                user = sampleUser,
            ),
        )

    override suspend fun deleteAccount(): Response<Unit> = Response.success(Unit)

    override suspend fun getBoxes(): Response<BoxesOut> =
        Response.success(BoxesOut(boxes = boxesState))

    override suspend fun getBox(boxId: Int): Response<BoxOut> =
        Response.success(boxesState.first { it.id == boxId })

    override suspend fun createBox(box: RequestBody, image: MultipartBody.Part?): Response<BoxOut> =
        Response.success(boxesState.first())

    override suspend fun updateBox(boxId: Int, body: UserCreateBox): Response<BoxOut> {
        lastUpdatedBoxPayload = body
        val existing = boxesState.first { it.id == boxId }
        val updated = existing.copy(
            name = body.name,
            location = body.location,
            description = body.description,
            colorCode = body.colorCode,
        )
        boxesState = boxesState.map { if (it.id == boxId) updated else it }
        return Response.success(updated)
    }

    override suspend fun updateBoxImage(boxId: Int, image: MultipartBody.Part): Response<BoxOut> {
        val updated = boxesState.first { it.id == boxId }.copy(imageUrl = "https://r2.dev/box_$boxId.jpg")
        boxesState = boxesState.map { if (it.id == boxId) updated else it }
        return Response.success(updated)
    }

    override suspend fun deleteBoxImage(boxId: Int): Response<BoxOut> {
        val updated = boxesState.first { it.id == boxId }.copy(imageUrl = "https://r2.dev/default.png")
        boxesState = boxesState.map { if (it.id == boxId) updated else it }
        return Response.success(updated)
    }

    override suspend fun deleteBox(boxId: Int): Response<Unit> = Response.success(Unit)

    override suspend fun deleteAllBoxes(): Response<Unit> = Response.success(Unit)

    override suspend fun generateQrCode(boxId: Int): Response<BoxOut> =
        Response.success(boxesState.first { it.id == boxId }.copy(qrCodeData = "https://r2.dev/$boxId.png"))

    override suspend fun addItem(boxId: Int, body: ItemBase): Response<ItemOut> =
        Response.success(
            ItemOut(
                id = 202,
                boxId = boxId,
                name = body.name,
                description = body.description,
                category = body.category,
                amount = body.amount,
            ),
        )

    override suspend fun getItem(itemId: Int): Response<ItemOut> =
        Response.success(initialItem)

    override suspend fun updateItem(itemId: Int, body: ItemBase): Response<ItemOut> {
        if (failNextUpdateItem) {
            failNextUpdateItem = false
            return Response.error(
                500,
                """{"detail":"Database write conflict"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        return Response.success(
            initialItem.copy(
                id = itemId,
                name = body.name,
                description = body.description,
                category = body.category,
                amount = body.amount,
            ),
        )
    }

    override suspend fun deleteItem(itemId: Int): Response<Unit> = Response.success(Unit)

    override suspend fun deleteItemsInBox(boxId: Int, body: ListOfItems): Response<Unit> =
        Response.success(Unit)

    override suspend fun moveItem(body: ItemTransfer): Response<ItemOut> =
        Response.success(initialItem.copy(id = body.itemId, boxId = body.targetBoxId))

    override suspend fun moveMultipleItems(body: List<ItemTransfer>): Response<Unit> =
        Response.success(Unit)

    override suspend fun detectItems(image: MultipartBody.Part): Response<List<ItemDetectionOut>> {
        val disposition = image.headers?.get("Content-Disposition").orEmpty()
        val partName = Regex("""name="([^"]+)"""").find(disposition)?.groupValues?.get(1).orEmpty()
        receivedDetectionPartNames += partName
        if (rejectImagePartWith422 && partName == "image") {
            return Response.error(
                422,
                """{"detail":[{"msg":"Field required"}]}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        return Response.success(
            listOf(
                ItemDetectionOut(name = "No items detected", confidence = 0.0, quantity = 0),
                ItemDetectionOut(name = "XLR Cable", confidence = 0.96, quantity = 3),
            ),
        )
    }

    override suspend fun checkHealth(): Response<DatabaseHealthOut> =
        Response.success(DatabaseHealthOut(status = "Connected", usersInDb = 1))
}

