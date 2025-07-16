# Configuración del Proyecto iOS en Xcode

Este directorio contiene todos los archivos SwiftUI necesarios para la app TeaBoard iOS. Para configurar el proyecto en Xcode, sigue estos pasos:

## Paso 1: Crear Proyecto en Xcode

1. Abrir Xcode
2. File → New → Project
3. Seleccionar **iOS** → **App**
4. Configurar:
   - **Product Name**: TeaBoard
   - **Team**: Tu equipo de desarrollo
   - **Organization Identifier**: com.example.teaboard (o tu dominio)
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None
   - **Include Tests**: ✓ (opcional)
5. Guardar en: `/Users/gmoqa/Dev/none/iosApp/`

## Paso 2: Reemplazar Archivos Generados

Una vez creado el proyecto, reemplazar los archivos generados por Xcode con los archivos de este directorio:

```bash
# Desde el directorio iosApp/
cp TeaBoard/TeaBoardApp.swift TeaBoard.xcodeproj/../TeaBoard/
cp TeaBoard/Info.plist TeaBoard.xcodeproj/../TeaBoard/
cp -r TeaBoard/Views/ TeaBoard.xcodeproj/../TeaBoard/
```

**O** simplemente copiar manualmente los archivos a través de Xcode:
- Arrastrar la carpeta `Views/` al proyecto
- Reemplazar `TeaBoardApp.swift`
- Reemplazar `Info.plist`

## Paso 3: Agregar el Framework Compartido

### 3.1 Copiar el Framework

```bash
# Copiar framework a la carpeta del proyecto
mkdir -p TeaBoard.xcodeproj/../Frameworks
cp -r ../shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework \
     TeaBoard.xcodeproj/../Frameworks/
```

### 3.2 Configurar en Xcode

1. Seleccionar el **proyecto** en el navegador
2. Seleccionar el **target** TeaBoard
3. Ir a **General** tab
4. En la sección **Frameworks, Libraries, and Embedded Content**:
   - Click en **+**
   - Click en **Add Other...** → **Add Files...**
   - Navegar a `Frameworks/shared.framework`
   - Seleccionar y agregar
   - Asegurarse que esté marcado como **Embed & Sign**

### 3.3 Configurar Build Phases (Importante)

Para que el framework se actualice automáticamente cuando cambies código Kotlin:

1. Seleccionar el **target** TeaBoard
2. Ir a **Build Phases** tab
3. Click en **+** → **New Run Script Phase**
4. Nombrar: "Build Kotlin Framework"
5. Agregar este script:

```bash
cd "$SRCROOT/../../shared"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Copiar framework actualizado
rm -rf "$SRCROOT/../Frameworks/shared.framework"
cp -r build/bin/iosSimulatorArm64/debugFramework/shared.framework \
     "$SRCROOT/../Frameworks/"
```

6. **Mover esta fase ANTES de "Compile Sources"** (arrastrando)

## Paso 4: Configurar Build Settings

1. Seleccionar el **target** TeaBoard
2. Ir a **Build Settings** tab
3. Buscar **Framework Search Paths**
4. Agregar: `$(PROJECT_DIR)/Frameworks`

## Paso 5: Configurar Permisos

El archivo `Info.plist` ya incluye los permisos necesarios:
- ✅ Cámara (`NSCameraUsageDescription`)
- ✅ Galería de fotos (`NSPhotoLibraryUsageDescription`)
- ✅ Micrófono (`NSMicrophoneUsageDescription`)

## Paso 6: Compilar y Ejecutar

1. Seleccionar un simulador (iPhone 15 Pro recomendado)
2. Click en **Run** (⌘R)

La app debería compilar y ejecutarse mostrando el grid de 6 botones.

---

## Troubleshooting

### Error: "Module 'shared' not found"

**Solución**: Verificar que:
1. El framework está en `Frameworks/shared.framework`
2. Framework Search Paths incluye `$(PROJECT_DIR)/Frameworks`
3. El framework está marcado como **Embed & Sign**

### Error: "Building for iOS Simulator, but linking in dylib built for iOS"

**Solución**: Estás usando el framework incorrecto. Usa:
- Simulador Apple Silicon: `iosSimulatorArm64/debugFramework/shared.framework`
- Simulador Intel: `iosX64/debugFramework/shared.framework`
- Device físico: `iosArm64/debugFramework/shared.framework`

### Error: Permisos de cámara/micrófono

**Solución**:
1. Verificar que `Info.plist` tiene las keys de permisos
2. Resetear permisos del simulador: Device → Erase All Content and Settings

### Framework no se actualiza

**Solución**:
1. Verificar que el script "Build Kotlin Framework" se ejecute
2. Limpiar build folder: Product → Clean Build Folder (⇧⌘K)
3. Recompilar

---

## Estructura del Proyecto

```
TeaBoard/
├── TeaBoardApp.swift          # App principal + AppState
├── Views/
│   ├── MainView.swift         # Pantalla principal con grid
│   ├── ConfigureButtonView.swift  # Configurar botón
│   └── SettingsView.swift     # Configuración y sync
├── Info.plist                 # Configuración y permisos
└── Frameworks/
    └── shared.framework       # Framework KMP compilado
```

---

## Arquitectura

### Flujo de Datos

```
SwiftUI Views
     ↓
  AppState (ObservableObject)
     ↓
shared.framework (Kotlin)
     ↓
┌────────────┬──────────────┐
│ iOS API   │ Shared Logic │
└────────────┴──────────────┘
```

### Servicios Disponibles (desde shared.framework)

- `IOSLocalStorageImpl` - Almacenamiento local (JSON + archivos)
- `IOSDriveStorageImpl` - Google Drive (stub, requiere implementación)
- `IOSAudioPlayer` - Reproducción de audio (AVAudioPlayer)
- `IOSAudioRecorder` - Grabación de audio (AVAudioRecorder)
- `FileProvider` - Manejo de archivos (NSFileManager)
- `PreferencesProvider` - UserDefaults wrapper
- `StorageService` - Delegador con fallback local/drive
- `ButtonConfig` - Modelo de datos compartido

---

## Próximos Pasos

### Funcionalidades Pendientes

1. **Google Drive iOS SDK**
   - Integrar GoogleSignIn SDK
   - Implementar `IOSDriveStorageImpl` completo
   - OAuth flow para iOS

2. **Audio Trimming**
   - Implementar `trimSilence()` en `IOSAudioRecorder`
   - Usar AVAudioEngine para procesamiento

3. **Mejoras UI**
   - Animaciones en transiciones
   - Feedback háptico en botones
   - Dark mode support

4. **Testing**
   - Unit tests para ViewModels
   - UI tests para flujos principales
   - Tests de integración con framework

---

## Comandos Útiles

```bash
# Recompilar framework (desde raíz del proyecto)
cd ../shared
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Copiar framework actualizado
cp -r build/bin/iosSimulatorArm64/debugFramework/shared.framework \
     ../iosApp/Frameworks/

# Limpiar build de Xcode
xcodebuild clean -project TeaBoard.xcodeproj -scheme TeaBoard

# Compilar desde terminal
xcodebuild -project TeaBoard.xcodeproj \
           -scheme TeaBoard \
           -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

---

## Soporte

Para más información sobre Kotlin Multiplatform y SwiftUI:
- [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui/)
- [Integrating Kotlin in iOS Apps](https://kotlinlang.org/docs/native-objc-interop.html)
