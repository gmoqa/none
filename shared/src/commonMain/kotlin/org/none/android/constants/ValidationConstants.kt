package org.none.android.constants

/**
 * Constantes para validación de datos de entrada.
 *
 * Centraliza todos los límites y reglas de validación para garantizar
 * consistencia entre Android e iOS.
 */
object ValidationConstants {
    /**
     * Validación de labels (etiquetas de botones)
     */
    object Label {
        /** Longitud mínima del label */
        const val MIN_LENGTH = 1

        /** Longitud máxima del label */
        const val MAX_LENGTH = 50

        /** Caracteres permitidos (regex) */
        const val ALLOWED_CHARS_REGEX = "^[a-zA-Z0-9\\s\\-_.áéíóúÁÉÍÓÚñÑ]+$"

        /** Label por defecto cuando no hay ninguno configurado */
        const val DEFAULT_PREFIX = "Botón"

        /**
         * Genera un label por defecto para un botón
         */
        fun getDefaultLabel(buttonId: Int): String = "$DEFAULT_PREFIX $buttonId"
    }

    /**
     * Validación de rutas de archivos
     */
    object FilePath {
        /** Longitud máxima de una ruta de archivo */
        const val MAX_PATH_LENGTH = 500

        /** Extensiones de imagen permitidas */
        val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        /** Extensiones de audio permitidas */
        val ALLOWED_AUDIO_EXTENSIONS = setOf("m4a", "mp3", "wav", "aac")

        /**
         * Valida que un path tenga una extensión permitida para imágenes
         */
        fun isValidImageExtension(path: String): Boolean {
            val extension = path.substringAfterLast('.', "").lowercase()
            return extension in ALLOWED_IMAGE_EXTENSIONS
        }

        /**
         * Valida que un path tenga una extensión permitida para audio
         */
        fun isValidAudioExtension(path: String): Boolean {
            val extension = path.substringAfterLast('.', "").lowercase()
            return extension in ALLOWED_AUDIO_EXTENSIONS
        }
    }

    /**
     * Validación de archivos de imagen
     */
    object Image {
        /** Tamaño máximo de imagen en bytes (5 MB) */
        const val MAX_SIZE_BYTES = 5 * 1024 * 1024

        /** Ancho mínimo en píxeles */
        const val MIN_WIDTH = 100

        /** Alto mínimo en píxeles */
        const val MIN_HEIGHT = 100

        /** Ancho máximo en píxeles */
        const val MAX_WIDTH = 4096

        /** Alto máximo en píxeles */
        const val MAX_HEIGHT = 4096
    }

    /**
     * Validación de archivos de audio
     */
    object Audio {
        /** Tamaño máximo de audio en bytes (10 MB) */
        const val MAX_SIZE_BYTES = 10 * 1024 * 1024

        /** Duración mínima en milisegundos (0.1 segundos) */
        const val MIN_DURATION_MS = 100L

        /** Duración máxima en milisegundos (30 segundos) */
        const val MAX_DURATION_MS = 30_000L

        /** Sample rate mínimo */
        const val MIN_SAMPLE_RATE = 8000

        /** Sample rate recomendado */
        const val RECOMMENDED_SAMPLE_RATE = 44100
    }

    /**
     * Validación de configuración de Google Drive
     */
    object Drive {
        /** Longitud mínima de un folder ID de Google Drive */
        const val MIN_FOLDER_ID_LENGTH = 20

        /** Longitud mínima de un file ID de Google Drive */
        const val MIN_FILE_ID_LENGTH = 20

        /** Regex para validar formato de email */
        const val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"

        /**
         * Valida que un email tenga formato válido
         */
        fun isValidEmail(email: String): Boolean {
            return email.matches(Regex(EMAIL_REGEX))
        }
    }

    /**
     * Mensajes de error de validación
     */
    object ErrorMessages {
        const val LABEL_EMPTY = "El label no puede estar vacío"
        const val LABEL_TOO_SHORT = "El label debe tener al menos ${Label.MIN_LENGTH} carácter"
        const val LABEL_TOO_LONG = "El label no puede tener más de ${Label.MAX_LENGTH} caracteres"
        const val LABEL_INVALID_CHARS = "El label contiene caracteres no permitidos"

        const val IMAGE_PATH_EMPTY = "Debe seleccionar una imagen"
        const val IMAGE_EXTENSION_INVALID = "Formato de imagen no soportado"
        const val IMAGE_TOO_LARGE = "La imagen supera el tamaño máximo de ${Image.MAX_SIZE_BYTES / 1024 / 1024} MB"
        const val IMAGE_TOO_SMALL = "La imagen es demasiado pequeña (mínimo ${Image.MIN_WIDTH}x${Image.MIN_HEIGHT})"

        const val AUDIO_PATH_EMPTY = "Debe grabar o seleccionar un audio"
        const val AUDIO_EXTENSION_INVALID = "Formato de audio no soportado"
        const val AUDIO_TOO_LARGE = "El audio supera el tamaño máximo de ${Audio.MAX_SIZE_BYTES / 1024 / 1024} MB"
        const val AUDIO_TOO_SHORT = "El audio es demasiado corto (mínimo ${Audio.MIN_DURATION_MS / 1000.0}s)"
        const val AUDIO_TOO_LONG = "El audio es demasiado largo (máximo ${Audio.MAX_DURATION_MS / 1000}s)"

        const val EMAIL_INVALID = "El email no tiene un formato válido"
        const val DRIVE_ID_INVALID = "El ID de Drive no es válido"
    }
}
