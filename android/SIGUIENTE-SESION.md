# Spicetify Mobile — Notas para la siguiente sesión

> Estado al **5 de noviembre de 2026**. Leer esto antes de tocar el proyecto.
> Repo: https://github.com/FabianReyes02/cliaaaa.git · rama `main` · todo commiteado y pusheado hasta `d18d4c5`.

## ✅ Lo que ya está hecho y verificado

- **Port nativo de Spicetify a Android** (Kotlin + Jetpack Compose, `android/`).
- APK debug incluido en la raíz del repo: `SpicetifyMobile.apk` (minSdk 26, instala en Android 8+).
- Compila con `./gradlew assembleDebug` y `./gradlew lintDebug` pasa sin errores.
- Pantallas: Inicio (reproductor completo con ticker demo), Letras Plus (LRCLIB sincronizado con auto-scroll), Temas (presets + acento + portada dinámica + AMOLED), Ajustes, Buscar, Novedades, Reddit.
- Portadas generadas deterministas en modo demo (degradado + iniciales).
- Persistencia con DataStore (tema, acento, AMOLED, Client ID, tokens).
- **Migración feb-2026 de la Web API aplicada** (`d18d4c5`): endpoints genéricos `/me/library`,
  búsqueda con `limit=10`, scopes reducidos, email/product eliminados de `GET /me`.
- Screenshots actualizados en `android/docs/screenshots/` y READMEs (raíz + `android/README.md`) al día.

## ⏭️ Lo que falta (en orden de prioridad)

### 1. Conectar la cuenta real de Spotify (tarea del usuario)
La app nunca se probó con la API real. Requisitos:
- Cuenta con **Premium** — desde feb-2026, el **dueño de la app debe tener Premium activo** o la app deja de funcionar.
- Crear la app en <https://developer.spotify.com/dashboard>:
  - Redirect URI **exacto**: `spicetify-mobile://callback`
  - Marcar **Web API**
  - Agregar el mail de la cuenta en la pestaña **Users and Access** (máx. 5 usuarios)
- Copiar el **Client ID** y pegarlo en la app: **Ajustes → Cuenta de Spotify → Conectar con Spotify**.

### 2. Probar el flujo OAuth end-to-end (primera prueba con red real)
- Abrir Spotify en el celular o PC y **darle play una vez** (dispositivo activo obligatorio).
- Verificar en **Inicio**: título/portada reales, progreso avanzando, ▶/⏸, siguiente/anterior, seek, shuffle, repeat, corazón (guardar en biblioteca).
- **Letras**: buscar la canción sonando (LRCLIB) y confirmar el resaltado sincronizado.
- **Buscar**: resultados reales. **Novedades**: álbumes del año (aproximación, ver trampa #1).
- Errores esperados y su causa:
  - `403` → falta Premium (dueño de la app o cuenta que controla).
  - `404` dispositivo → no hay dispositivo activo; dar play en Spotify.
  - "User not authorized" → el mail no está en Users and Access.

### 3. (Opcional) Capturas con cuenta conectada
Reemplazar/agregar screenshots en `android/docs/screenshots/` con la app mostrando datos reales.

### 4. (Opcional) Release empaquetada
- Configurar un keystore propio (**no commitearlo**), `isMinifyEnabled = true`, verificar en emulador y publicar el `.apk` de release.
- Migrar los tokens de DataStore a `EncryptedSharedPreferences` o Android Keystore.

## 🧰 Entorno (no perder tiempo re-descubriéndolo)

- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (platforms 26–36, build-tools 34/35/36). JDK 21 en PATH.
- `services.gradle.org` **no** responde en esta red; `downloads.gradle.org` sí (el wrapper ya usa la URL correcta).
- Build: `cd android && ./gradlew assembleDebug`. `android/local.properties` existe y está ignorado.
- Emulador: `Medium_Phone_API_35` · `adb devices` → `emulator-5554` · `emulator -avd Medium_Phone_API_35` o Android Studio.
- `adb`: `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe` (Git Bash: anteponer `MSYS_NO_PATHCONV=1` para rutas `/sdcard/...`).
- Captura de pantalla: `adb exec-out screencap -p > archivo.png`
- Coordenadas táctiles (2400 px de alto): Inicio `[130,2290]` · Letras `[400,2280]` · Temas `[675,2280]` · Ajustes `[950,2290]` · botón play `[540,850]` · quick action Buscar `[205,1335]` · tarjeta Novedades `[790,1780]`.
- UI dump para textos: `adb shell uiautomator dump /sdcard/window.xml` y `adb shell cat /sdcard/window.xml`.
- Git: remote `origin` → `github.com/FabianReyes02/cliaaaa`. Ignorados y **no deben entrar al repo**: `android/local.properties`, `android/build/`, `android/.gradle/`, `android/.kotlin/`.
- El `.gitignore` raíz anclaba `/spicetify` (antes el patrón sin anclar ignoraba todo el paquete `com/spicetify/mobile/` con las fuentes). **No revertir ese cambio.**

## 🪤 Trampas conocidas

1. **Spotify cambió su API en feb-2026** (afectarían a cualquier app nueva):
   `GET /browse/new-releases` fue **eliminado** → la pantalla Novedades muestra álbumes del año con `q=year:2026&type=album`.
   Endpoints de biblioteca reemplazados por `PUT/DELETE /me/library` y `GET /me/library/contains` con **URIs** (`spotify:track:…`).
   `GET /search` limita `limit` a **10**.
   `GET /me` ya no devuelve `email`, `product`, `country`, `followers` → se eliminaron los scopes `user-read-private` y `user-read-email`.
   Requisito Premium del dueño de la app; máximo 5 usuarios por app.
2. **Lint falla** si `windowLightNavigationBar` está en `themes.xml` (requiere API 27 y minSdk=26). Ya eliminado.
3. **`mipmap-anydpi-v26/`** es la carpeta correcta para el ícono adaptativo (renombrarla hace que AAPT "no encuentre" los recursos).
4. **Texto suelto sin color → negro ilegible.** Por eso `MainActivity` envuelve todo en `Surface(color = background)`. No quitarlo.
5. El seek bar con punto verde al final es el **stop indicator** estándar de Material 3 — no es un bug.

## 📝 Commits relevantes (recientes → viejos)

- `d18d4c5` feat(android): migrate Spotify integration to the February 2026 Web API
- `8555c50` docs(android): add prebuilt SpicetifyMobile.apk for direct download
- `0bd4c14` feat(android): port Spicetify to a native Android app (Kotlin + Compose)

## 📋 Checklist de evaluación — estado

Todos los ítems del "Checklist para la evaluación" en `android/README.md` están marcados,
**menos** la prueba con cuenta real de Spotify (ver secciones 1 y 2). La documentación es honesta
sobre el alcance: es un **port del concepto**, no una inyección en la APK de Spotify (imposible en Android sin root).
