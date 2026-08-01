# MemoCards — AGENTS

App nativa Android (Kotlin + Jetpack Compose).

## Reglas

- **Compilación solo en CI remoto** (GitHub Actions). El portátil es de pocos recursos: NO ejecutar `./gradlew`, NO instalar Android SDK, NO `npm install` / `expo run:android`.
- Para obtener APK: push a `main` (o *Run workflow*) → descargar artifact `memocards-release-apk`.
- No generar artefactos locales: `node_modules/`, `.gradle/`, `build/`, `.expo/` están en `.gitignore` y no deben recrearse salvo necesidad explícita.
- Consultar MCP Dev Ideas (`projectId: 3`) al inicio.
- Commits/push solo si el usuario lo pide.

## Estructura

- `app/src/main/java/com/zatiki/memocards/` — código Kotlin
- `domain/` — modelos + SM-2
- `data/` — Room + repositorio
- `ui/` — Compose screens + temas
- `legacy-expo/` — app Expo anterior (referencia)
