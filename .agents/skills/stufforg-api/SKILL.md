---
name: stufforg-api
description: >-
  Comprehensive guide, endpoint specs, Kotlin models, Retrofit interfaces, authentication patterns,
  and Jetpack Compose client architecture for Android applications communicating with the StuffOrg FastAPI backend.
  Use when developing Android Jetpack Compose screens, ViewModels, Retrofit network services,
  handling JWT token refresh, uploading multipart box images or YOLO detection requests, and debugging API integrations.
---

# StuffOrg API Integration Skill (Android Jetpack Compose & Kotlin)

This skill guides agents and developers building an Android client with **Kotlin & Jetpack Compose** to integrate with the StuffOrg FastAPI backend.

---

## 1. Architecture & Networking Setup

The FastAPI backend is mounted at `root_path="/api"`. All API routes are accessed under `/api`.

### Base URL for Android
- **Android Emulator**: `http://10.0.2.2:8000/api/` (Note: `10.0.2.2` bridges to the host machine's `localhost`).
- **Physical Device**: `http://<YOUR_HOST_LAN_IP>:8000/api/` (e.g. `http://192.168.1.50:8000/api/`).
- **Retrofit Requirement**: Retrofit base URLs **must end with a trailing slash `/`**.

### Cleartext HTTP Permission
Android blocks non-HTTPS traffic by default. For local backend testing, enable cleartext traffic:
1. Create `res/xml/network_security_config.xml` (see [network_security_config.xml](./references/network_security_config.xml)).
2. Add to `AndroidManifest.xml`:
   ```xml
   <application
       android:networkSecurityConfig="@xml/network_security_config"
       android:usesCleartextTraffic="true" ... >
   ```

---

## 2. Authentication & Token Lifecycle

StuffOrg issues two tokens:
1. `access_token`: Short-lived JWT used in header `Authorization: Bearer <access_token>`.
2. `refresh_token`: 30-day JWT returned in the response body specifically for mobile clients.

### Native Mobile Auth Rules
- **Token Persistence**: Save both `access_token` and `refresh_token` in **Jetpack DataStore** or **EncryptedSharedPreferences** (see [TokenManager](./references/token_manager_and_interceptor.kt)).
- **Auth Header Interceptor**: An OkHttp `Interceptor` injects `Authorization: Bearer <access_token>` on all requests except `/login`, `/users/`, and `/refresh`.
- **401 Token Refresh (OkHttp Authenticator)**:
  When a protected call fails with `401 Unauthorized`, an `okhttp3.Authenticator` intercepts it:
  - Reads the stored `refresh_token`.
  - Executes a synchronous `POST /api/refresh` with `{ "refresh_token": "..." }`.
  - Persists the new `access_token` and optional new `refresh_token`.
  - Re-executes the failed call with the new token.
  - Complete implementation: [token_manager_and_interceptor.kt](./references/token_manager_and_interceptor.kt).

---

## 3. High-Risk Integration Gotchas (Read Before Coding!)

1. **`POST /api/boxes/` Multipart Form Format**:
   - The field name is `"box"`.
   - Its value **must be a plain JSON string** of `UserCreateBox`, NOT a nested multipart structure!
   - In Retrofit:
     ```kotlin
     val boxJson = Json.encodeToString(userCreateBox)
     val boxPart = boxJson.toRequestBody("text/plain".toMediaType())
     val imagePart = MultipartHelpers.createImagePart(file)
     apiService.createBox(box = boxPart, image = imagePart)
     ```
2. **`GET /api/boxes/` Response Wrapping**:
   - The response is `BoxesOut` (`{ "boxes": [...] }`), **not** a bare `List<BoxOut>`.
   - Access the list via `response.body()?.boxes`.
3. **Endpoint Path Quirks**:
   - `GET /api/boxes/` and `POST /api/boxes/` require trailing slashes.
   - `GET /api/boxes/{box_id}` has **no** trailing slash.
   - Delete single box: `DELETE /api/deleteBoxById/{box_id}` (not `DELETE /api/boxes/{box_id}`).
   - Delete all boxes: `DELETE /api/deleteAllBoxes/` (has trailing slash).
4. **YOLO Detection Endpoint**:
   - `POST /api/item_detection` expects multipart field name **`"file"`**, NOT `"image"`.
   - Returns a `List<ItemDetectionOut>` containing `name`, `confidence`, and `quantity`.
   - Pre-fill box items or let users review detected labels before saving.
5. **Image Loading in Compose**:
   - When `box.imageUrl` is returned (Cloudflare R2 link), render it using **Coil**:
     ```kotlin
     AsyncImage(model = box.imageUrl, contentDescription = box.name)
     ```

---

## 4. Endpoints Cheat Sheet

| Endpoint | Method | Body / Multipart | Return Type | Auth |
| :--- | :--- | :--- | :--- | :--- |
| `users/` | `POST` | `UserCreate` | `AccessTokenOut` | None |
| `login` | `POST` | `UserLogin` | `AccessTokenOut` | None |
| `google_oidc_login` | `POST` | `GoogleToken` | `AccessTokenOut` | None |
| `apple_oidc_login` | `POST` | `AppleToken` | `AccessTokenOut` | None |
| `refresh` | `POST` | `RefreshTokenIn` | `AccessTokenOut` | None |
| `email-exists` | `POST` | `EmailForVerification` | `EmailForVerificationResponse` | None |
| `delete_account` | `DELETE` | None | `204 Unit` | Bearer |
| `boxes/` | `GET` | None | `BoxesOut` (`.boxes`) | Bearer |
| `boxes/` | `POST` | `@Part("box")`, `@Part image` | `BoxOut` | Bearer |
| `boxes/{box_id}` | `GET` | None | `BoxOut` | Bearer |
| `boxes/{box_id}` | `PUT` | `UserCreateBox` | `BoxOut` | Bearer |
| `deleteBoxById/{box_id}` | `DELETE`| None | `204 Unit` | Bearer |
| `deleteAllBoxes/` | `DELETE`| None | `204 Unit` | Bearer |
| `generate_qrcode/{box_id}` | `POST` | None | `200 Unit` | Bearer |
| `items/{box_id}` | `POST` | `ItemBase` | `ItemOut` | Bearer |
| `items/{item_id}` | `GET` | None | `ItemOut` | Bearer |
| `items/{item_id}` | `PUT` | `ItemBase` | `ItemOut` | Bearer |
| `items/{item_id}` | `DELETE`| None | `204 Unit` | Bearer |
| `boxes/{box_id}/items` | `DELETE`| None | `204 Unit` | Bearer |
| `items/moveone` | `PATCH` | `ItemTransfer` | `ItemOut` | Bearer |
| `items/movemultiple` | `PATCH` | `List<ItemTransfer>` | `200 Unit` | Bearer |
| `item_detection` | `POST` | `@Part file: MultipartBody.Part` | `List<ItemDetectionOut>` | Bearer |
| `test-db` | `GET` | None | `DatabaseHealthOut` | None |

---

## 5. Reference Code Files

- [**`models.kt`**](./references/models.kt): All `@Serializable` Kotlin data classes matching backend schemas.
- [**`api_service.kt`**](./references/api_service.kt): Complete Retrofit 2 interface for every endpoint.
- [**`token_manager_and_interceptor.kt`**](./references/token_manager_and_interceptor.kt): DataStore token manager, OkHttp AuthInterceptor, 401 Authenticator, and multipart upload helpers.
- [**`compose_patterns.kt`**](./references/compose_patterns.kt): Clean Compose ViewModel (`StateFlow`) and UI Screen with Coil image rendering.
- [**`network_security_config.xml`**](./references/network_security_config.xml): Android manifest config for cleartext local emulator communication.
- [**`endpoints.md`**](./references/endpoints.md): Markdown endpoint catalog with detailed JSON examples.
- [**`openapi.json`**](./references/openapi.json): Raw OpenAPI 3.1.0 spec pulled from the server.
