package org.none.android.constants

/**
 * Constantes relacionadas con la configuración y visualización de botones.
 *
 * Estas constantes son compartidas entre Android e iOS para garantizar
 * consistencia en el comportamiento de la aplicación.
 */
object ButtonConstants {
    /**
     * Número máximo de botones configurables en la aplicación.
     */
    const val MAX_BUTTONS = 6

    /**
     * Colores de los botones en formato ARGB (Android) / equivalente para iOS.
     *
     * Orden:
     * - Botón 1: Rosa suave (#FFCCCC)
     * - Botón 2: Azul suave (#CCE5FF)
     * - Botón 3: Amarillo suave (#FFF3CC)
     * - Botón 4: Verde suave (#D9FFD9)
     * - Botón 5: Púrpura suave (#F2D9FF)
     * - Botón 6: Naranja suave (#FFE6CC)
     */
    object Colors {
        // Android: Formato 0xAARRGGBB
        const val BUTTON_1_ANDROID = 0xFFFFCCCC.toInt()  // Rosa suave
        const val BUTTON_2_ANDROID = 0xFFCCE5FF.toInt()  // Azul suave
        const val BUTTON_3_ANDROID = 0xFFFFF3CC.toInt()  // Amarillo suave
        const val BUTTON_4_ANDROID = 0xFFD9FFD9.toInt()  // Verde suave
        const val BUTTON_5_ANDROID = 0xFFF2D9FF.toInt()  // Púrpura suave
        const val BUTTON_6_ANDROID = 0xFFFFE6CC.toInt()  // Naranja suave

        val ANDROID_COLORS = listOf(
            BUTTON_1_ANDROID,
            BUTTON_2_ANDROID,
            BUTTON_3_ANDROID,
            BUTTON_4_ANDROID,
            BUTTON_5_ANDROID,
            BUTTON_6_ANDROID
        )

        // iOS: Formato RGB normalizado (0.0 - 1.0)
        object iOS {
            // Botón 1: Rosa suave (RGB: 255, 204, 204)
            const val BUTTON_1_RED = 1.0
            const val BUTTON_1_GREEN = 0.8
            const val BUTTON_1_BLUE = 0.8

            // Botón 2: Azul suave (RGB: 204, 229, 255)
            const val BUTTON_2_RED = 0.8
            const val BUTTON_2_GREEN = 0.9
            const val BUTTON_2_BLUE = 1.0

            // Botón 3: Amarillo suave (RGB: 255, 243, 204)
            const val BUTTON_3_RED = 1.0
            const val BUTTON_3_GREEN = 0.95
            const val BUTTON_3_BLUE = 0.8

            // Botón 4: Verde suave (RGB: 217, 255, 217)
            const val BUTTON_4_RED = 0.85
            const val BUTTON_4_GREEN = 1.0
            const val BUTTON_4_BLUE = 0.85

            // Botón 5: Púrpura suave (RGB: 242, 217, 255)
            const val BUTTON_5_RED = 0.95
            const val BUTTON_5_GREEN = 0.85
            const val BUTTON_5_BLUE = 1.0

            // Botón 6: Naranja suave (RGB: 255, 230, 204)
            const val BUTTON_6_RED = 1.0
            const val BUTTON_6_GREEN = 0.9
            const val BUTTON_6_BLUE = 0.8
        }
    }

    /**
     * Rango de IDs válidos para botones (1-6).
     */
    object IdRange {
        const val MIN_BUTTON_ID = 1
        const val MAX_BUTTON_ID = MAX_BUTTONS

        fun isValidButtonId(id: Int): Boolean = id in MIN_BUTTON_ID..MAX_BUTTON_ID
    }

    /**
     * Obtiene el color Android correspondiente a un ID de botón.
     *
     * @param buttonId ID del botón (1-6)
     * @return Color en formato ARGB o gris por defecto si el ID es inválido
     */
    fun getAndroidColor(buttonId: Int): Int {
        return if (buttonId in 1..MAX_BUTTONS) {
            Colors.ANDROID_COLORS[buttonId - 1]
        } else {
            0xFFCCCCCC.toInt()  // Gris por defecto
        }
    }
}
