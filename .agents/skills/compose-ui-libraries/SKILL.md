---
name: compose-ui-libraries
description: Select, configure, and implement modern Jetpack Compose and Compose Multiplatform UI libraries. Use when building Android or KMP UI screens, configuring libs.versions.toml dependencies, or implementing design systems, custom bottom sheets, frosted glass blur, async image loading, skeleton shimmers, charts, calendars, onboarding tooltips, zoomable images, drag-and-drop lists, or rich text editors.
---
# Compose UI Libraries

Guide the selection, Gradle configuration, and idiomatic implementation of vetted Jetpack Compose and Compose Multiplatform UI libraries so the agent uses battle-tested ecosystem primitives instead of building fragile custom components from scratch.

## When to Use

- Designing or scaffolding a new Jetpack Compose or Compose Multiplatform UI feature
- Selecting a third-party library for a specific UI capability (charts, blur, calendars, sheets, image loading, drag-and-drop, tooltips, animations)
- Generating or updating `gradle/libs.versions.toml` and module `build.gradle.kts` files for UI dependencies
- Replacing brittle custom canvas or gesture code with accessible, production-ready Compose primitives

## Steps

### 1. Map the UI Requirement to the Target Library

Before writing custom modifiers or layouts, select the standard library for the task from this decision matrix:

| UI Task / Feature | Recommended Library | Maven Coordinates (`group:artifact`) |
| :--- | :--- | :--- |
| Standard Material 3 & Dynamic Color | Official Material 3 | `androidx.compose.material3:material3` |
| Responsive / Foldable / Tablet Layouts | Material 3 Adaptive | `androidx.compose.material3.adaptive:adaptive`, `adaptive-layout`, `adaptive-navigation`, `androidx.compose.material3:material3-adaptive-navigation-suite` |
| Custom Design System / Headless Primitives / Multi-Detent Bottom Sheets | Compose Unstyled | `com.composables:core` (or modular `composeunstyled-*` artifacts) |
| Frosted Glass / Background Blur | Haze | `dev.chrisbanes.haze:haze`, `dev.chrisbanes.haze:haze-materials` |
| Coroutine Image Loading | Coil 3 | `io.coil-kt.coil3:coil-compose`, `io.coil-kt.coil3:coil-network-okhttp` (or `coil-network-ktor3`) |
| Image Loading Animations / Placeholders | Landscapist | `com.github.skydoves:landscapist-coil3`, `landscapist-placeholder`, `landscapist-animation` |
| Skeleton Loading Shimmer | Compose Shimmer | `com.valentinilk.shimmer:compose-shimmer` |
| Pinch-to-Zoom & Panning Large Images | Telephoto | `me.saket.telephoto:zoomable-image-coil3` |
| Vector / Lottie Animations | Compottie (KMP) or Lottie | `io.github.alexzhirkevich:compottie` or `com.airbnb.android:lottie-compose` |
| Line, Bar, and Cartesian Charts | Vico | `com.patrykandpatrick.vico:compose`, `com.patrykandpatrick.vico:compose-m3` |
| Customizable Week/Month Calendars | Kizitonwose Calendar | `com.kizitonwose.calendar:compose` |
| Drag-and-Drop LazyList / Grid Reordering | Reorderable | `sh.calvin.reorderable:reorderable` |
| Onboarding Tooltips & Popups | Balloon | `com.github.skydoves:balloon-compose` |
| WYSIWYG Rich Text & Markdown | Compose Rich Editor | `com.mohamedrejeb.richeditor:richeditor-compose` |
| Capture Composable to Bitmap | Capturable | `dev.shreyaspatil:capturable` |

### 2. Configure Gradle Version Catalog (`libs.versions.toml`)

Group related UI libraries into logical `[bundles]` in `gradle/libs.versions.toml` so feature modules can declare clean, single-line dependencies:

1. Define `compose-bom` for official AndroidX Compose artifacts and omit individual version numbers on BOM-managed libraries.
2. Bundle `adaptive`, `adaptive-layout`, `adaptive-navigation`, and `material3-adaptive-navigation-suite` under a `compose-adaptive` bundle.
3. Bundle `coil-compose` and a network fetcher (`coil-network-okhttp` for Android or `coil-network-ktor3` for KMP) under a `coil` bundle.
4. Bundle `haze` and `haze-materials` under a `haze` bundle.

### 3. Apply Library-Specific Implementation Patterns

Follow these architectural patterns when generating Compose UI code with the selected libraries:

- **Adaptive Navigation (`NavigationSuiteScaffold`)**: Wrap top-level destinations in `NavigationSuiteScaffold` instead of hardcoding `NavigationBar`. It automatically switches between a bottom bar on compact phones, a `NavigationRail` on medium width/foldables, and a permanent drawer on expanded screens.
- **Headless Primitives (`compose-unstyled`)**: When the design requires non-Material aesthetics or a bottom sheet with custom snap detents (e.g., peek, half-expanded, full-screen), use Compose Unstyled's `ModalBottomSheet` and `rememberModalBottomSheetState` instead of fighting Material 3's `ModalBottomSheet` constraints.
- **Background Blur (`haze`)**: Create a single `val hazeState = rememberHazeState()`. Apply `Modifier.hazeSource(state = hazeState)` to the scrolling background content container, and apply `Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin())` to the floating top bar, bottom bar, or card overlay.
- **Skeleton Loading (`compose-shimmer`)**: Reuse your actual item composable layout during loading states by applying `Modifier.shimmer()` to the root container and background placeholders to child boxes, rather than maintaining a separate skeleton composable hierarchy.
- **Zoomable Media (`telephoto`)**: Inside `HorizontalPager` or image galleries, replace `AsyncImage` with `ZoomableAsyncImage` so pinch-to-zoom, double-tap zoom, and pan gestures automatically coordinate with pager swipe gestures.
- **Drag-and-Drop Lists (`Reorderable`)**: Always provide stable keys in `LazyColumn` (`items(items, key = { it.id })`) and bind `rememberReorderableLazyListState` so item animations and scroll-at-edge behavior work reliably.

### 4. Verify Stability and Accessibility

1. Ensure interactive custom primitives (especially when using headless components or custom modifiers) include semantic roles and `contentDescription` for TalkBack support.
2. Wrap external library state holders using their provided `remember*State()` functions so state survives recomposition and configuration changes where supported.

## Gotchas

- **Coil 3 Package & Network Fetcher Split**: Coil 3 uses the package `coil3` and Maven group `io.coil-kt.coil3` (not `io.coil-kt`). Unlike Coil 2, `coil-compose` does not bundle networking by default. You must include `coil-network-okhttp` or `coil-network-ktor3` in dependencies, or remote URLs will silently fail to load.
- **Haze 1.x+ API Naming**: Modern Haze versions use `Modifier.hazeSource(hazeState)` on the background content and `Modifier.hazeEffect(hazeState)` on the blurred overlay. Avoid deprecated `Modifier.haze()` / `Modifier.hazeChild()` calls from early 0.x releases.
- **Material 3 vs. Headless Mix-ups**: Do not stack Material 3 surface elevation tinting on top of `hazeEffect` overlays or `compose-unstyled` primitives, as opaque Material surface colors will hide the underlying blur or custom styling. Use `Color.Transparent` on the scaffold/app bar container when applying `hazeEffect`.
- **Lazy Layout Keys with Reorderable**: Using `sh.calvin.reorderable` without providing a unique, immutable `key` lambda on every `items(...)` block in `LazyColumn` or `LazyVerticalGrid` will cause visual glitches and state loss during drag operations.