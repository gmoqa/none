# NONE - Comunicación Aumentativa para Autismo

**Proyecto de doble propósito:**
- 📱 **Aplicación Android** nativa en Kotlin - Herramienta AAC para personas autistas
- 🌐 **Sitio Web** multiidioma - Landing page informativa en [noneapp.org](https://noneapp.org)

---

## 📱 Aplicación Android

Aplicación nativa desarrollada en Kotlin, diseñada como herramienta de comunicación aumentativa y alternativa (CAA) para personas autistas. Permite crear botones personalizados con imágenes y sonidos que se pueden almacenar localmente o sincronizar con Google Drive.

## Características

✅ **Almacenamiento flexible** - Funciona sin internet (modo local) o con sincronización en Google Drive
✅ **Login opcional con Google** - Solo necesario si deseas sincronización en la nube
✅ **Pantalla completa** - Interfaz sin distracciones
✅ **Grilla de 6 botones personalizables** - Cada botón puede tener:
  - Imagen personalizada (captura de cámara o galería)
  - Audio grabado personalizado
  - Etiqueta descriptiva
✅ **Grabación de audio fluida** - Graba sonidos directamente en la app
✅ **Sincronización opcional** - Sincroniza tus configuraciones entre dispositivos vía Google Drive
✅ **Modo edición/uso** - Alterna fácilmente entre configurar y usar los botones
✅ **Interfaz accesible** - Botones grandes con imágenes visuales

## Propósito Terapéutico

Esta aplicación está diseñada específicamente para ayudar a personas con trastorno del espectro autista (TEA) a comunicarse de manera efectiva mediante:

1. **Comunicación visual y auditiva**: Asociación de imágenes con sonidos para facilitar la expresión de necesidades
2. **Personalización completa**: Cada usuario puede crear botones con sus propias imágenes y grabaciones
3. **Funciona sin internet**: No requiere conexión permanente, ideal para uso en cualquier lugar
4. **Sincronización opcional**: Los datos pueden sincronizarse en Google Drive para uso en múltiples dispositivos
5. **Simplicidad de uso**: Interfaz clara y directa, sin elementos confusos

## Requisitos previos

1. **Android Studio** (versión Arctic Fox o superior)
2. **JDK 11 o superior**
3. **Cuenta de Google** (solo si deseas usar sincronización con Drive)
4. **Dispositivo o emulador Android** con API 21 o superior (Android 5.0+)

## Configuración (Opcional - Solo para sincronización con Google Drive)

Si deseas usar la app **sin sincronización en la nube**, puedes omitir esta sección completamente. La app funcionará en modo local.

Para habilitar la sincronización con Google Drive, sigue la guía detallada en [`GOOGLE_DRIVE_SETUP.md`](GOOGLE_DRIVE_SETUP.md).

**Resumen rápido:**
1. Crear proyecto en Google Cloud Console
2. Habilitar Google Drive API
3. Configurar OAuth Consent Screen
4. Crear credenciales OAuth 2.0 para Android con tu SHA-1 fingerprint:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

**Nota**: Una vez configurado, los usuarios podrán activar/desactivar la sincronización desde el menú de Ajustes dentro de la app.

## Instalación y ejecución

1. Clona o descarga este proyecto
2. **(Opcional)** Si deseas sincronización, completa la configuración de Google Drive API
3. Abre el proyecto en Android Studio
4. Sincroniza Gradle (File → Sync Project with Gradle Files)
5. Conecta un dispositivo o inicia un emulador
6. Ejecuta la aplicación (Run → Run 'app')

**Nota**: La app funcionará inmediatamente en modo local. La sincronización con Drive se puede activar desde Ajustes una vez configurado el OAuth.

## Cómo usar la aplicación

### Primera vez

1. Al abrir la app, verás la pantalla principal con 6 botones
2. La app está en modo local por defecto (no requiere login)
3. **(Opcional)** Para habilitar sincronización con Drive:
   - Toca "Ajustes" en la esquina superior derecha
   - Activa el switch "Habilitar sincronización"
   - Inicia sesión con tu cuenta de Google
   - Acepta los permisos de Drive

### Configurar botones

1. Una vez dentro, verás la grilla de 6 botones
2. Toca el botón **"Editar"** en la esquina superior izquierda
3. Toca cualquier botón que desees configurar
4. En la pantalla de configuración:
   - **Imagen**: Toca "Tomar Foto" o "Seleccionar Imagen"
   - **Audio**: Toca "🎤 Grabar", habla, luego "⏹️ Detener"
   - **Etiqueta**: Escribe un nombre descriptivo (ej: "Agua", "Comida", "Baño")
   - Puedes reproducir el audio con "▶️ Reproducir Audio" para verificarlo
5. Toca **"Guardar"**
6. Los archivos se subirán automáticamente a Firebase

### Usar los botones

1. Toca el botón **"Usar"** para salir del modo edición
2. Toca cualquier botón configurado
3. Se reproducirá el sonido asociado
4. Mantén presionado cualquier botón para editarlo rápidamente

### Desactivar sincronización

1. Toca el botón **"Ajustes"** en la esquina superior derecha
2. Desactiva el switch "Habilitar sincronización"
3. Opcionalmente, cierra sesión de Google
4. Los datos locales permanecerán en el dispositivo

## Estructura del proyecto

```
tea-board/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/teaboard/
│   │   │   ├── models/
│   │   │   │   └── ButtonConfig.kt          # Modelo de datos
│   │   │   ├── services/
│   │   │   │   ├── AudioRecorderService.kt  # Grabación de audio
│   │   │   │   ├── AudioPlayerService.kt    # Reproducción de audio
│   │   │   │   ├── StorageService.kt        # Delegador local/Drive
│   │   │   │   ├── LocalStorageService.kt   # Almacenamiento local
│   │   │   │   └── DriveStorageService.kt   # Google Drive sync
│   │   │   ├── MainActivity.kt              # Pantalla principal
│   │   │   ├── SettingsActivity.kt          # Configuración y sync
│   │   │   └── ConfigureButtonActivity.kt   # Configuración de botones
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_settings.xml
│   │   │   │   └── dialog_configure_button.xml
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── strings_google.xml       # ⚠️ Configurar Web Client ID
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── file_paths.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── README.md
└── GOOGLE_DRIVE_SETUP.md              # Guía de configuración de Drive
```

## Permisos necesarios

La aplicación solicita los siguientes permisos:

- **INTERNET**: Para sincronizar con Firebase
- **RECORD_AUDIO**: Para grabar sonidos personalizados
- **CAMERA**: Para tomar fotos de los botones
- **READ_MEDIA_IMAGES**: Para seleccionar imágenes de la galería (Android 13+)
- **READ_EXTERNAL_STORAGE**: Para seleccionar imágenes (Android 12 y anteriores)

Todos los permisos se solicitan en tiempo de ejecución cuando son necesarios.

## Casos de uso terapéutico

### Ejemplos de configuración de botones:

1. **Necesidades básicas**:
   - Botón "Agua" - Foto de un vaso de agua + audio diciendo "Tengo sed"
   - Botón "Comida" - Foto de un plato + audio diciendo "Tengo hambre"
   - Botón "Baño" - Foto de un baño + audio diciendo "Necesito ir al baño"

2. **Emociones**:
   - Botón "Feliz" - Cara sonriente + audio con risa
   - Botón "Triste" - Cara triste + audio de llanto suave
   - Botón "Enojado" - Cara enojada + audio expresando frustración

3. **Actividades**:
   - Botón "Jugar" - Foto de juguetes + audio diciendo "Quiero jugar"
   - Botón "Dormir" - Foto de cama + audio diciendo "Tengo sueño"

## Tecnologías utilizadas

- **Kotlin** - Lenguaje de programación
- **Android SDK 34** - API mínima 21 (Android 5.0+, 99% de dispositivos)
- **Google Sign-In** - Autenticación con Google (opcional)
- **Google Drive API** - Sincronización en la nube (opcional)
- **Almacenamiento Local** - JSON + archivos internos (modo offline)
- **Material Design 3** - Diseño de interfaz moderno
- **Glide** - Carga de imágenes optimizada
- **Coroutines** - Programación asíncrona
- **MediaRecorder/MediaPlayer** - Grabación y reproducción de audio
- **ProGuard** - Optimización y reducción de tamaño del APK

## Optimizaciones implementadas

✅ **Compatibilidad extendida**: Soporta desde Android 5.0 (99% de dispositivos)
✅ **Modo offline completo**: Funciona sin internet, sincronización es opcional
✅ **Almacenamiento dual**: Sistema local con fallback automático si Drive falla
✅ **Cache inteligente**: Los datos de Drive se cachean localmente para uso offline
✅ **Feedback háptico**: Vibración al presionar botones para mejor experiencia sensorial
✅ **Pantalla siempre encendida**: No se apaga durante el uso
✅ **APK optimizado**: Minificación y reducción de recursos en builds de release
✅ **Dependencias actualizadas**: Últimas versiones de Google Play Services y AndroidX

## Solución de problemas

### Los botones no guardan la configuración
- En modo local: Los datos se guardan en el almacenamiento interno del dispositivo automáticamente
- En modo Drive: Verifica que hayas iniciado sesión y tengas conexión a Internet
- Revisa los permisos de cámara y micrófono

### No se reproduce el audio
- Verifica que hayas concedido permiso de RECORD_AUDIO
- Asegúrate de haber grabado y guardado el audio antes
- Revisa que el archivo de audio se haya guardado correctamente

### Error al tomar fotos
- Verifica que hayas concedido permiso de CAMERA
- Asegúrate de que el dispositivo tenga cámara

### Error al activar sincronización con Drive
- Verifica que hayas configurado OAuth 2.0 en Google Cloud Console
- Asegúrate de que el SHA-1 fingerprint sea correcto
- Revisa que Google Drive API esté habilitada en tu proyecto
- Espera 5-10 minutos para que los cambios se propaguen

### La sincronización no funciona
- Verifica tu conexión a Internet
- Asegúrate de haber iniciado sesión correctamente
- Revisa los permisos de Google Drive en la configuración de la app

## Mejoras futuras posibles

- [ ] Agregar más botones (grilla configurable)
- [ ] Modo oscuro/claro
- [ ] Exportar/importar configuraciones
- [ ] Categorías de botones
- [ ] Soporte para texto-a-voz (TTS)
- [ ] Modo kiosco para bloquear salida de la app
- [ ] Estadísticas de uso
- [ ] Compartir configuraciones entre usuarios/terapeutas

## Contribuciones

Este es un proyecto de código abierto diseñado para ayudar a la comunidad de personas con autismo y sus familias. Las contribuciones son bienvenidas.

## 📁 Estructura del Proyecto

```
/
├── 📱 app/                    # Aplicación Android
│   ├── src/main/
│   │   ├── java/              # Código fuente Kotlin
│   │   ├── res/               # Recursos (layouts, strings, etc.)
│   │   └── assets/            # Recursos de la app (sonidos, etc.)
│   └── build.gradle
│
├── 🌐 docs/                   # Landing page (GitHub Pages)
│   ├── index.html             # 🇪🇸 Versión en español
│   ├── en/                    # 🇬🇧 Versión en inglés
│   ├── pt/                    # 🇧🇷 Versión en portugués
│   ├── fr/                    # 🇫🇷 Versión en francés
│   ├── de/                    # 🇩🇪 Versión en alemán
│   ├── styles.css             # Estilos compartidos
│   ├── script.js              # JavaScript compartido
│   ├── assets/                # Imágenes de la landing page
│   └── CNAME                  # Configuración dominio noneapp.org
│
├── 📚 documentation/          # Documentación del proyecto
│   ├── README_LANDING.md              # Guía rápida de la landing
│   ├── LANDING_PAGE_SETUP.md          # Configuración GitHub Pages
│   ├── MULTILANGUAGE_SETUP.md         # Sistema multiidioma
│   ├── DOMINIO_NONEAPP_SETUP.md       # Configuración DNS
│   └── ACTUALIZACIONES_LICENCIA.md    # Cambios de licencia
│
├── README.md                  # Este archivo
├── CLAUDE.md                  # Instrucciones para Claude Code
├── build.gradle               # Configuración Gradle
└── gradlew                    # Gradle wrapper
```

## 🌐 Sitio Web

Landing page multiidioma disponible en:
- 🇪🇸 Español: [noneapp.org](https://noneapp.org)
- 🇬🇧 Inglés: [noneapp.org/en](https://noneapp.org/en)
- 🇧🇷 Portugués: [noneapp.org/pt](https://noneapp.org/pt)
- 🇫🇷 Francés: [noneapp.org/fr](https://noneapp.org/fr)
- 🇩🇪 Alemán: [noneapp.org/de](https://noneapp.org/de)

**Documentación del sitio web**: Ver carpeta [`documentation/`](documentation/)

## Licencia

Este proyecto está disponible bajo la licencia **GNU GPL v3**.

**Lo que esto significa:**
- ✅ Gratis para usar siempre
- ✅ Gratis para modificar y mejorar
- ✅ Debe permanecer open source si se distribuye
- ❌ No puede convertirse en software comercial de código cerrado

**Copyright © 2023-2025 Guillermo Quinteros**

Nombrado en honor a Salvador "None" Quinteros.

## Contacto y Soporte

- **Email**: gu.quinteros@gmail.com
- **GitHub**: [@gmoqa](https://github.com/gmoqa)
- **Sitio web**: [noneapp.org](https://noneapp.org)

Para reportar problemas o solicitar ayuda:
- Consulta [`documentation/`](documentation/) para guías específicas
- Consulta `GOOGLE_DRIVE_SETUP.md` para configuración de sincronización
- Crea un issue en el repositorio del proyecto
- La app funciona sin configuración adicional en modo local

## 💝 Apoyo

NONE siempre será gratuito. Si te ayuda a ti o a tus seres queridos, considera donar para apoyar los costos de terapia de None.

---

**Nota importante**: Esta aplicación es una herramienta de asistencia y no reemplaza el tratamiento profesional. Siempre consulta con terapeutas y especialistas en autismo para un plan de comunicación completo.
