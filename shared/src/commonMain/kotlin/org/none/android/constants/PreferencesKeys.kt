package org.none.android.constants

/**
 * Claves centralizadas para SharedPreferences (Android) / UserDefaults (iOS).
 *
 * Usar estas constantes en lugar de strings hardcodeados previene:
 * - Typos y errores de escritura
 * - Inconsistencias entre plataformas
 * - Cambios accidentales de claves
 * - Dificultad para encontrar todas las referencias
 */
object PreferencesKeys {
    /**
     * Sincronización con Google Drive
     */
    object Sync {
        /** Indica si la sincronización con Drive está habilitada */
        const val SYNC_ENABLED = "sync_enabled"

        /** Indica si el usuario ha iniciado sesión con Google */
        const val IS_LOGGED_IN = "is_logged_in"

        /** Email de la cuenta de Google del usuario */
        const val USER_EMAIL = "user_email"

        /** Nombre del usuario de Google */
        const val USER_NAME = "user_name"

        /** ID de la carpeta de TeaBoard en Google Drive */
        const val TEABOARD_FOLDER_ID = "teaboard_folder_id"

        /** ID del archivo de configuración en Google Drive */
        const val CONFIG_FILE_ID = "config_file_id"
    }

    /**
     * Configuración de la aplicación
     */
    object App {
        /** Indica si es la primera vez que se ejecuta la app */
        const val FIRST_LAUNCH = "first_launch"

        /** Versión de la app cuando se ejecutó por última vez */
        const val LAST_APP_VERSION = "last_app_version"

        /** Timestamp de la última sincronización exitosa */
        const val LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    }

    /**
     * Preferencias de UI
     */
    object UI {
        /** Indica si el modo de edición está activo por defecto */
        const val DEFAULT_EDIT_MODE = "default_edit_mode"

        /** Última orientación utilizada */
        const val LAST_ORIENTATION = "last_orientation"
    }

    /**
     * Configuración de audio
     */
    object Audio {
        /** Umbral de silencio para trim (0-32767) */
        const val SILENCE_THRESHOLD = "silence_threshold"

        /** Margen de samples para trim */
        const val TRIM_MARGIN_SAMPLES = "trim_margin_samples"

        /** Indica si el trim de silencio está habilitado */
        const val TRIM_ENABLED = "trim_enabled"
    }

    /**
     * Valores por defecto para las preferencias
     */
    object Defaults {
        const val SYNC_ENABLED = false
        const val IS_LOGGED_IN = false
        const val USER_EMAIL = ""
        const val USER_NAME = ""
        const val TEABOARD_FOLDER_ID = ""
        const val CONFIG_FILE_ID = ""
        const val FIRST_LAUNCH = true
        const val LAST_APP_VERSION = ""
        const val LAST_SYNC_TIMESTAMP = 0L
        const val DEFAULT_EDIT_MODE = false
        const val LAST_ORIENTATION = 0
        const val SILENCE_THRESHOLD = 1000
        const val TRIM_MARGIN_SAMPLES = 1
        const val TRIM_ENABLED = true
    }
}
