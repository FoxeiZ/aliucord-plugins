package com.uploader.exceptions

/**
 * Exception thrown when a file operation fails because the file doesn't exist.
 */
open class FileNotFoundException(cause: Throwable? = null) : Exception("File not found", cause)

/**
 * Exception thrown when a Catbox file operation fails because the file doesn't exist.
 */
class NoSuchCatboxFileException(cause: Exception) : FileNotFoundException(cause)

/**
 * Exception thrown when a Catbox album operation fails because the album doesn't exist or can't be modified.
 */
class NoSuchCatboxAlbumException(
    albumShort: String,
    cause: Exception
) : Exception("Album with short '$albumShort' not found or cannot be modified", cause)
