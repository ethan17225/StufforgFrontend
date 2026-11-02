# Dropped Prototype Features (`DROPPED_FEATURES.md`)

Per project scope, **StuffOrg Frontend** preserves the **Lumina Pastel** visual design and user workflows from the Stitch prototype (`projects/16898046296254646034`) while implementing **only features backed by the StuffOrg FastAPI backend** (or native Android OS capabilities agreed upon in scope).

Below is the complete list of Stitch prototype widgets and decorative concepts that were dropped because the backend API / database schema (`UserOut`, `BoxOut`, `ItemOut`, `ItemDetectionOut`) does not store or expose them.

---

## 1. Global / Cross-Cutting Prototype Concepts

| Dropped Prototype Concept | Stitch Screens | Reason Dropped |
|---|---|---|
| **100% Offline-First / Local SQLite Guest Vault** ("Skip Account • Use Local Guest Vault", "Argon2id local key", "Zero Cloud Upload") | Onboarding 1–3, Sign In, Create Account, Settings | Every StuffOrg inventory endpoint (`/api/boxes/`, `/api/items/{box_id}`, `/api/item_detection`) requires a valid FastAPI JWT `Bearer` token (`HTTPBearer`) and persists to the server's PostgreSQL database + Cloudflare R2 storage. |
| **Bell / Push Notification Center** (`notifications` top-bar icon) | Dashboard, Create Box, Box Details, Settings | Backend exposes no notifications table, push registration endpoint, or alert feed. |
| **"StuffOrg Pro" Subscription Tier Badges** ("Pro Member • Unlimited Boxes", "Pro Organizer") | Dashboard, Settings | `UserOut` contains only `id`, `username`, `email`, `last_login`, `created_at`, and `delete_at` — no billing or subscription tier fields exist. |

---

## 2. Onboarding (`Onboarding 1–3`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **"Tour sample storage vault instead" / "Load Sample 'Studio & Tech' Vault"** | No guest/demo mode or sample-seed API endpoint exists without creating real server boxes under an authenticated user account. |
| **"What space do you want to organize first?" preset space configurator** (`Home & Garage`, `Studio & Gear`, `Moving & Units`, `Office & Tech`) | Backend has no user-level workspace/zone table (`location` is a free-text string per box). |
| **Runtime `CAMERA` permission prompt on Step 2 ("Enable Camera & Continue")** | Intentionally omitted (YAGNI / Android privacy best practice): the app uses Android's system `ActivityResultContracts.TakePicture()`, `PickVisualMedia()`, and Google Play Services Code Scanner (`GmsBarcodeScanning`), which require **zero runtime `CAMERA` permission**. |

---

## 3. Sign In & Create Account (`Sign In`, `Create Account`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Continue with Apple** | Explicitly excluded per scope decision (`POST /api/google_oidc_login` is the selected Google OIDC social auth endpoint). |
| **Full Name (Optional) field on Sign In** | `POST /api/login` (`UserLogin`) accepts only `username` or `email`, `password`, and `save_password`. `UserOut` has no `full_name` column. |
| **Forgot Password? flow** | Backend has no password-reset token, email dispatch, or `/forgot-password` endpoint. |
| **Vault Passkey (Face ID / Touch ID biometric unlock)** | Backend uses standard JWT access + refresh tokens (`/api/login`, `/api/refresh`), not WebAuthn/Passkeys. |
| **Generate Paper Recovery Key (PDF)** | Backend manages standard server-side password hashing rather than client-custodial encryption keys. |
| **Terms of Service / Privacy Manifesto / Open Source Specs / Help links** | No hosted legal/help URLs or backend endpoints exist for these links. |

---

## 4. Dashboard (`Dashboard`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Box Capacity Ring (`82% Box Cap`)** | `BoxOut` has no `max_capacity` or slot-limit field (`id`, `user_id`, `name`, `location`, `description`, `color_code`, `qr_code_data`, `image_url`, `created_at`, `items`). |
| **Voice Search Microphone (`mic` icon inside search bar)** | Client-side search already filters boxes, items, categories, and locations instantaneously as you type. |
| **Dedicated "Storage Zones" card carousel (`Garage Zone A`, `Creative Studio`)** | Backend has no separate `Zone` entity with icons or capacities; replaced by live dynamic location filter pills derived directly from `BoxOut.location`. |

---

## 5. Create Box & AI Scan (`Create Box & AI Scan`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Bounding-box overlay & inference latency badge (`4 Bounding Boxes`, `Processed in 340ms`)** | `POST /api/item_detection` returns `List<ItemDetectionOut>` containing only `name`, `confidence`, and `quantity` — no bounding-box coordinates (`x`, `y`, `w`, `h`) or server timing metadata are returned. |
| **Save as Draft** | Backend has no draft box status (`BoxOut` records are immediately live upon `POST /api/boxes/`). |

---

## 6. Box Details & Contents (`Box Details & Contents`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Container Type badge (`Clear Plastic Tote`)** | `BoxOut` / `UserCreateBox` has no container-material or bin-type column. |
| **Structured per-item spec tags (`25ft`, `Studio Grade`, `Gold Plated` chips)** | `ItemOut` / `ItemBase` only persists free-text `description` alongside `name`, `category`, and `amount` (no structured spec tags). |

---

## 7. QR Scanner & Instant Lookup (`QR Scanner`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Custom live camera HUD controls (`60 FPS` badge, `Flash Off`, `Gallery` import, `Chime On` toggle, `Decoded in 18ms`)** | QR scanning uses **Google Code Scanner** (`GmsBarcodeScanning`), which provides its own secure system viewfinder UI (including built-in torch control) with zero camera permission, paired with manual Box ID lookup and quick-tap chips in-app. |

---

## 8. Print QR Labels (`Print QR Labels`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Multi-template sheet layouts (`Avery 8-up Letter` matrix with `Starting at Sticker Position #1`, `Thermal Roll 4×6"`, `PDF Vector Export`)** | Kept single-sticker 2×2" rendering via ZXing + Android's native `androidx.print.PrintHelper` (which delegates paper size, copies, and printer selection—including Save as PDF—to the Android system print spooler) and PNG share via `FileProvider`. |
| **Hardcoded printer status card (`Canon TS8320 Series • AirPrint Connected`)** | Android handles printer discovery and status natively inside the system print dialog opened by `PrintHelper`. |

---

## 9. Add Item (`Add Item`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Target Box Capacity bar (`Capacity: 14/20 Items • 6 slots remaining`)** | `BoxOut` has no capacity limit column. |
| **Per-Item Photo Persistence** | `POST /api/items/{box_id}` accepts JSON `ItemBase` (`name`, `description`, `category`, `amount`) only — `ItemOut` has no `image_url` column. *(Note: taking/picking an item snapshot is still supported in-app to run `/api/item_detection` YOLO auto-fill for name & quantity, with a clear UI note that item photos aren't persisted on the server.)* |
| **Scan UPC / EAN Barcode product lookup** | Backend has no UPC/EAN product catalog lookup endpoint. |
| **Unit Type selector (`Pieces`, `Cables`, `Sets`)** | `ItemBase.amount` is a plain `Int` without a `unit` field. |
| **Condition selector (`Brand New`, `Good / Gently Used`, `Studio Backup`)** | `ItemBase` / `ItemOut` has no `condition` column. |
| **Sub-slot / Pocket field** | `ItemBase` / `ItemOut` has no `sub_slot` column. |
| **Estimated Value & Date Acquired (`$45.00`, `Jan 2024`)** | `ItemBase` / `ItemOut` has no price, currency, or purchase-date columns. |

---

## 10. Item Detail & Edit (`Item Detail & Edit`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Item Hero Photo & Condition / Grade badges (`Studio Grade`, `Working Condition`)** | `ItemOut` has no `image_url`, `grade`, or `condition` fields (`id`, `box_id`, `name`, `description`, `category`, `amount`, `created_at`). |
| **Barcode / Serial (`UPC 8492049102`), Structured Length & Specs, Estimated Replacement Value (`$65.00 each / $260.00 total`), Last Inspected by Organizer** | None of these structured metadata columns exist on `ItemOut` (free-text notes are supported via `ItemOut.description`). |
| **Activity History audit log (`Alex decreased quantity by 1`, `Scanned in via AI Vision`)** | Backend does not store an item-level mutation/audit history table; only `created_at` is returned and displayed. |

---

## 11. Profile & Storage Settings (`Profile & Storage Settings`)

| Dropped Widget / Action | Reason Dropped |
|---|---|
| **Edit Profile button (`username` / `email` / avatar update)** | Backend `users.py` exposes registration, login, Google OIDC login, refresh, and `DELETE /api/delete_account` (schedule deletion), with no `PUT`/`PATCH` profile-update or avatar upload endpoint. |
| **Storage Capacity meter (`82% • 2.1 GB Media Cached`)** | Backend exposes no storage quota or R2 media byte-usage endpoint. |
| **Export Inventory as CSV / Excel** | Backend exposes no CSV/XLSX export endpoint. |
| **Manage Locations & Zones screen** | Backend has no standalone locations/zones CRUD endpoint (`location` is an attribute on each box, editable directly in Box Detail). |
| **App Preferences toggles (`AI Auto-Tagging`, `Scan Sound & Haptics`, `Theme & Appearance` switcher)** | Per scope, Lumina Pastel Light Mode is fixed, Google Code Scanner manages scan feedback natively, and AI detection is triggered explicitly when capturing/uploading a photo. |
