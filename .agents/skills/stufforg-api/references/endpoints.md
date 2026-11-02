# StuffOrg API Endpoints Reference

Base URL: `http://localhost:8000/api` (All routes are prefixed with `/api`)

---

## 1. Authentication & Users

### `POST /users/`
Register a new user.
- **Auth**: None
- **Body (`application/json`)**:
  ```json
  {
    "username": "johndoe",
    "email": "johndoe@example.com",
    "password": "securepassword123",
    "repeat_password": "securepassword123"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "eyJhbGciOi...",
    "token_type": "bearer",
    "user": {
      "id": 1,
      "username": "johndoe",
      "email": "johndoe@example.com",
      "created_at": "2026-11-01T12:00:00"
    }
  }
  ```

### `POST /login`
Log in with credentials.
- **Auth**: None
- **Body (`application/json`)**:
  ```json
  {
    "username": "johndoe",
    "email": "johndoe@example.com",
    "password": "securepassword123",
    "save_password": true
  }
  ```
  *(Note: Either `username` or `email` can be provided along with `password`)*
- **Response `200 OK`**:
  ```json
  {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "eyJhbGciOi...",
    "token_type": "bearer",
    "user": { "id": 1, "username": "johndoe", "email": "johndoe@example.com" }
  }
  ```

### `POST /google_oidc_login`
Login/Register with Google OAuth ID Token.
- **Auth**: None
- **Body**: `{"google_token": "<google_id_token>"}`
- **Response `200 OK`**: `AccessTokenOut`

### `POST /apple_oidc_login`
Login/Register with Apple OAuth Identity Token.
- **Auth**: None
- **Body**: `{"apple_token": "<apple_identity_token>"}`
- **Response `200 OK`**: `AccessTokenOut`

### `POST /refresh`
Exchange a refresh token for new access and refresh tokens.
- **Auth**: None
- **Body (`application/json`)**:
  ```json
  {
    "refresh_token": "<refresh_token_string>"
  }
  ```
  *(Also accepts `refresh_token` httpOnly cookie for web browsers)*
- **Response `200 OK`**: `AccessTokenOut`

### `GET /email-exists`
Check if an email is registered in the database.
- **Auth**: None
- **Body (`application/json`)**: `{"email": "user@example.com"}`
- **Response `200 OK`**: `{"email": "user@example.com", "message": "User exists in database"}`
- **Response `404 Not Found`**: `{"detail": "Email not found on the database"}`

### `DELETE /delete_account`
Mark the authenticated user's account for deletion (scheduled 14-day grace period).
- **Auth**: Bearer Token
- **Response `204 No Content`**

---

## 2. Boxes

### `GET /boxes/`
Retrieve all boxes belonging to the authenticated user.
- **Auth**: Bearer Token
- **Response `200 OK`**:
  ```json
  {
    "boxes": [
      {
        "id": 1,
        "user_id": 1,
        "name": "Holiday Decorations",
        "location": "Attic",
        "description": "Christmas ornaments",
        "color_code": "#FF5733",
        "image_url": "https://...",
        "qr_code_data": "1",
        "created_at": "2026-11-01T12:00:00",
        "items": []
      }
    ]
  }
  ```
  *(Notice: The response is an object with a `"boxes"` key containing the list)*

### `POST /boxes/`
Create a new box with optional image upload.
- **Auth**: Bearer Token
- **Content-Type**: `multipart/form-data`
- **Form Fields**:
  - `box`: (string, required) **JSON string** of `UserCreateBox`:
    ```json
    {
      "name": "Kitchen Tools",
      "location": "Pantry",
      "description": "Baking gear",
      "color_code": "#4CAF50",
      "listOfItems": [
        {"name": "Whisk", "amount": 2, "category": "Utensil"}
      ]
    }
    ```
  - `image`: (file, optional) Binary image file.
- **Response `201 Created`**: Returns the created `BoxOut` object.

### `GET /boxes/{box_id}`
Get details for a specific box.
- **Auth**: Bearer Token
- **Response `200 OK`**: `BoxOut`
- **Response `404 Not Found`**: If box doesn't exist or belongs to another user.

### `PUT /boxes/{box_id}`
Update a box.
- **Auth**: Bearer Token
- **Body (`application/json`)**: `UserCreateBox`
- **Response `200 OK`**: `BoxOut`

### `DELETE /deleteBoxById/{box_id}`
Delete a box by ID.
- **Auth**: Bearer Token
- **Response `204 No Content`**

### `DELETE /deleteAllBoxes/`
Delete all boxes for authenticated user.
- **Auth**: Bearer Token
- **Response `204 No Content`**

### `POST /generate_qrcode/{box_id}`
Generate and upload QR code for the box.
- **Auth**: Bearer Token
- **Response `200 OK`**

---

## 3. Items

### `POST /items/{box_id}`
Add a new item to a box.
- **Auth**: Bearer Token
- **Body (`application/json`)**:
  ```json
  {
    "name": "Electric Drill",
    "description": "DeWalt 20V Cordless",
    "category": "Tools",
    "amount": 1
  }
  ```
- **Response `201 Created`**: `ItemOut`

### `GET /items/{item_id}`
Get item details.
- **Auth**: Bearer Token
- **Response `200 OK`**: `ItemOut`

### `PUT /items/{item_id}`
Update an existing item.
- **Auth**: Bearer Token
- **Body (`application/json`)**: `ItemBase`
- **Response `200 OK`**: `ItemOut`

### `DELETE /items/{item_id}`
Delete an item.
- **Auth**: Bearer Token
- **Response `204 No Content`**

### `DELETE /boxes/{box_id}/items`
Delete all items inside a specific box.
- **Auth**: Bearer Token
- **Response `204 No Content`**

### `PATCH /items/moveone`
Move an item from one box to another.
- **Auth**: Bearer Token
- **Body (`application/json`)**:
  ```json
  {
    "item_id": 15,
    "target_box_id": 3
  }
  ```
- **Response `200 OK`**: `ItemOut`

### `PATCH /items/movemultiple`
Move multiple items to a target box.
- **Auth**: Bearer Token
- **Body (`application/json`)**: Array of transfers:
  ```json
  [
    { "item_id": 15, "target_box_id": 3 },
    { "item_id": 16, "target_box_id": 3 }
  ]
  ```
- **Response `200 OK`**

---

## 4. YOLO Object Detection

### `POST /item_detection`
Upload a photo to automatically detect items with YOLO.
- **Auth**: Bearer Token
- **Content-Type**: `multipart/form-data`
- **Form Field**:
  - `file`: (file, required) Image file (`image/jpeg`, `image/png`, etc.)
- **Response `200 OK`**:
  ```json
  [
    {
      "name": "bottle",
      "confidence": 0.89,
      "quantity": 2
    },
    {
      "name": "book",
      "confidence": 0.94,
      "quantity": 1
    }
  ]
  ```
- **Frontend Usage Pattern**:
  Use this endpoint to allow users to take a photo of a box's contents, inspect the detected items list, and prefill `listOfItems` when calling `POST /boxes/` or `POST /items/{box_id}`.

---

## 5. Health

### `GET /test-db`
- **Auth**: None
- **Response `200 OK`**: `{"status": "Connected", "users_in_db": 1}`

