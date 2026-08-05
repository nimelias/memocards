# MemoCards

App móvil tipo Anki: mazos, tarjetas y repaso con repetición espaciada (SM-2).

## Stack (nativo)

- Kotlin + Jetpack Compose + Material 3
- Room (SQLite local)
- Navigation Compose
- Builds Android solo vía GitHub Actions (sin SDK en el portátil)

## Desarrollo local

Editar código Kotlin en `app/src/main/java/`. **No instalar Android SDK ni ejecutar Gradle en el portátil** (laptop de pocos recursos).

El código Expo/React Native anterior está en `legacy-expo/` (solo referencia; no hace falta `npm install` salvo consultar el código legacy).

## Compilación — solo CI remoto

> **Política:** las APKs se generan **exclusivamente en GitHub Actions**. No compilar en local (`./gradlew`, `expo run:android`, etc.).

| Qué | Dónde |
|-----|-------|
| Workflow | `.github/workflows/android-preview.yml` |
| Comando CI | `./gradlew assembleRelease` |
| Disparadores | push a `main` o *Run workflow* manual |
| Artifact | `memocards-release-apk` (retención 14 días) |
| Descarga | Actions → último run → Artifacts |

Tras push, esperar el workflow verde y descargar el APK desde Artifacts.

## Funciones portadas

- Lista / creación de mazos
- Detalle de mazo (stats, ajustes de estudio, reset)
- Crear tarjetas (frente/reverso)
- Repaso SM-2 (Otra vez / Difícil / Bien / Fácil)
- Temas claro / oscuro / arena + escala de fuente
- **Sincronización con estudIA** (puerto 30004): importar barajas, enviar estadísticas de repaso, sync automático
