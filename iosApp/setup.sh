#!/bin/bash

# Script de configuración automática para TeaBoard iOS
# Este script ayuda a configurar el proyecto después de crearlo en Xcode

set -e  # Exit on error

echo "🚀 Configurando TeaBoard iOS..."

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar que estamos en el directorio correcto
if [ ! -f "README_XCODE_SETUP.md" ]; then
    echo -e "${RED}❌ Error: Ejecuta este script desde el directorio iosApp/${NC}"
    exit 1
fi

echo -e "${BLUE}📁 Verificando estructura...${NC}"

# Verificar que existe el proyecto Xcode
if [ ! -d "TeaBoard.xcodeproj" ]; then
    echo -e "${RED}❌ No se encontró TeaBoard.xcodeproj${NC}"
    echo "Por favor, crea primero el proyecto en Xcode siguiendo el paso 1 del README"
    exit 1
fi

echo -e "${GREEN}✅ Proyecto Xcode encontrado${NC}"

# Crear directorio de Frameworks
echo -e "${BLUE}📦 Creando directorio de Frameworks...${NC}"
mkdir -p Frameworks

# Compilar framework iOS
echo -e "${BLUE}🔨 Compilando framework Kotlin Multiplatform...${NC}"
cd ../shared
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# Detectar arquitectura del Mac
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    echo -e "${BLUE}Detectado Mac Apple Silicon (M1/M2/M3)${NC}"
    FRAMEWORK_TARGET="iosSimulatorArm64"
    ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
else
    echo -e "${BLUE}Detectado Mac Intel${NC}"
    FRAMEWORK_TARGET="iosX64"
    ./gradlew :shared:linkDebugFrameworkIosX64
fi

# Copiar framework
echo -e "${BLUE}📋 Copiando framework al proyecto iOS...${NC}"
cd ../iosApp
rm -rf Frameworks/shared.framework
cp -r ../shared/build/bin/${FRAMEWORK_TARGET}/debugFramework/shared.framework Frameworks/

echo -e "${GREEN}✅ Framework copiado exitosamente${NC}"

# Verificar que los archivos Swift existen
echo -e "${BLUE}🔍 Verificando archivos Swift...${NC}"

REQUIRED_FILES=(
    "TeaBoard/TeaBoardApp.swift"
    "TeaBoard/Views/MainView.swift"
    "TeaBoard/Views/ConfigureButtonView.swift"
    "TeaBoard/Views/SettingsView.swift"
    "TeaBoard/Info.plist"
)

ALL_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo -e "${RED}❌ Falta: $file${NC}"
        ALL_EXIST=false
    else
        echo -e "${GREEN}✅ $file${NC}"
    fi
done

if [ "$ALL_EXIST" = false ]; then
    echo -e "${RED}⚠️  Algunos archivos faltan. Cópialos manualmente desde el directorio TeaBoard/${NC}"
fi

echo ""
echo -e "${GREEN}🎉 ¡Configuración completada!${NC}"
echo ""
echo -e "${BLUE}Próximos pasos:${NC}"
echo "1. Abrir TeaBoard.xcodeproj en Xcode"
echo "2. Ir a target TeaBoard → General → Frameworks, Libraries, and Embedded Content"
echo "3. Agregar Frameworks/shared.framework (Embed & Sign)"
echo "4. Ir a Build Settings → Framework Search Paths → Agregar: \$(PROJECT_DIR)/Frameworks"
echo "5. Compilar y ejecutar (⌘R)"
echo ""
echo "📖 Para más detalles, consulta README_XCODE_SETUP.md"
