# MemoCards — AGENTS

App nativa Android (Kotlin + Jetpack Compose).

## Reglas

- Sin Android SDK en el portátil: NO usar `./gradlew` ni instalar SDK local salvo CI.
- Builds: GitHub Actions (`assembleRelease`).
- Consultar MCP Dev Ideas (`projectId: 3`) al inicio.
- Commits/push solo si el usuario lo pide.

## Estructura

- `app/src/main/java/com/zatiki/memocards/` — código Kotlin
- `domain/` — modelos + SM-2
- `data/` — Room + repositorio
- `ui/` — Compose screens + temas
- `legacy-expo/` — app Expo anterior (referencia)
