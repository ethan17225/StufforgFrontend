---
name: kotlin-project-architect
description: Guide and generate best-practice Android Jetpack Compose and Compose Multiplatform (KMP) project structures, multi-module architectures, UDF state hoisting patterns, and Gradle convention plugins. Use when bootstrapping, organizing, modularizing, or refactoring Jetpack Compose codebases.
---
# Kotlin Project Architect (Jetpack Compose Edition)

Guide engineers and AI agents in structuring, modularizing, and packaging Android Jetpack Compose and Compose Multiplatform (KMP) projects following Google's Now in Android (NiA) architecture, Unidirectional Data Flow (UDF), and Compose compiler stability best practices.

## When to Use

* Bootstrapping a new Android Jetpack Compose or Compose Multiplatform (KMP) project.
* Designing multi-module Gradle architectures (`:app`, `:feature:*`, `:core:*`) and convention plugins (`build-logic`).
* Structuring feature packages with Unidirectional Data Flow (Screen, ViewModel, UiState, and Navigation routes).
* Enforcing Compose compiler stability boundaries between domain/UI modules and data/infrastructure layers.

## Workflow

### 1. Build Logic and Compose Compiler Configuration

* Enforce Gradle Kotlin DSL (`settings.gradle.kts` and `build.gradle.kts`) and centralize all dependencies in `gradle/libs.versions.toml`.
* Configure the **Kotlin Compose Compiler Gradle plugin** (`org.jetbrains.kotlin.plugin.compose`) via reusable convention plugins under `build-logic/convention`:
  * `AndroidApplicationComposeConventionPlugin` for `:app`.
  * `AndroidLibraryComposeConventionPlugin` for `:core:designsystem`, `:core:ui`, and `:feature:*` modules.
  * `AndroidFeatureConventionPlugin` to automatically wire Compose runtime, `lifecycle-runtime-compose`, `hilt-navigation-compose` (or Koin), and type-safe Navigation (`kotlinx-serialization`).
* Configure a shared `stability_config.conf` in the root project and register it in the Compose compiler extension so external domain/stdlib types (`java.time.*`, `kotlinx.collections.immutable.*`) are treated as stable across modules without polluting pure Kotlin modules with Compose annotations.

### 2. Multi-Module Hierarchy for Jetpack Compose

Organize the project into strict, decoupled module tiers so the Compose Compiler only runs where UI exists and feature modules never depend on each other:

* **`:app` (Entry Point)**: Application class, root `MainActivity`, top-level `NavigationSuiteScaffold` / `NavHost` graph assembly, and root DI wiring.
* **`:feature:<name>` (UI & Presentation)**: Self-contained feature slices (e.g., `:feature:home`, `:feature:inventory`, `:feature:settings`).
  * Depends **only** on `:core:ui`, `:core:designsystem`, `:core:domain`, and `:core:model`.
  * **Never** depends on another `:feature:*` module or directly on `:core:database` / `:core:network`.
* **`:core:designsystem` (Pure Compose Primitives)**: Theme (`MaterialTheme`, ColorScheme, Typography), icons, tokens, and stateless foundational wrappers (Buttons, TopAppBars, Sheets, Haze blur wrappers). Zero business or domain model dependencies.
* **`:core:ui` (Composite Shared UI)**: Reusable, domain-aware Composables shared across multiple features (e.g., `ItemCard`, `EmptyStateView`, `UserAvatar`) that combine `:core:designsystem` + `:core:model`.
* **`:core:model` (Pure Kotlin Models)**: Lightweight, immutable data classes representing domain entities. Keep free of Room/Retrofit annotations so `:feature:*` modules receive inherently `@Stable` types.
* **`:core:domain` (Use Cases)**: Optional layer for reusable complex business logic combining multiple repositories.
* **`:core:data` / `:core:database` / `:core:network` (Infrastructure)**: Repository implementations, Room DAOs/Entities, and Ktor/Retrofit data sources. Conceal all implementation classes with the `internal` modifier. Do **not** enable the Compose compiler in these modules.

### 3. Feature Package Structure & UDF File Colocation

Inside a `:feature:<name>` module (or a feature package in a single-module project), organize files around Unidirectional Data Flow (UDF) and type-safe Navigation:

```text
com.example.app.feature.inventory/
├── navigation/
│   └── InventoryNavigation.kt       // @Serializable route objects, NavGraphBuilder.inventoryScreen()
├── InventoryScreen.kt               // Stateful Route composable + Stateless Screen composable + @Preview
├── InventoryViewModel.kt            // StateFlow<InventoryUiState>, event handling
├── InventoryUiState.kt              // @Immutable / sealed interface UiState & single-file UI intents/events
└── components/                      // Feature-private internal sub-composables
    ├── InventoryHeader.kt
    └── InventoryItemRow.kt