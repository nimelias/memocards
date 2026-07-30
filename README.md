# MemoCards

App móvil tipo Anki: mazos, tarjetas y repaso con repetición espaciada (SM-2).

## Stack (nativo)

- Kotlin + Jetpack Compose + Material 3
- Room (SQLite local)
- Navigation Compose
- Builds Android solo vía GitHub Actions (sin SDK en el portátil)

## Desarrollo

No hace falta Android SDK local. El código fuente está en `app/src/main/java/`.

El código Expo/React Native anterior está en `legacy-expo/` (referencia durante la migración).

## Build (GitHub Actions)

- Workflow: `.github/workflows/android-preview.yml`
- Artifact: `memocards-release-apk`
- Push a `main` o *Run workflow*

## Funciones portadas

- Lista / creación de mazos
- Detalle de mazo (stats, ajustes de estudio, reset)
- Crear tarjetas (frente/reverso)
- Repaso SM-2 (Otra vez / Difícil / Bien / Fácil)
- Temas claro / oscuro / arena + escala de fuente
