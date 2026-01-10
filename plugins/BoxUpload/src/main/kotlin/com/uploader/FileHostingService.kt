package com.uploader

import com.uploader.exceptions.FileNotFoundException
import java.io.File

/**
 * Abstract base class for all file hosting services.
 * Defines the common interface that all file hosting services must implement.
 */
abstract class FileHostingService {

    /**
     * Uploads the given content to the hosting service and returns the URL pointing to the uploaded file.
     *
     * @param file the file to upload
     * @return the URL pointing to the uploaded file
     * @throws IllegalArgumentException if name is blank
     */
    abstract

    fun upload(file: File): String

    /**
     * Uploads content from the given URL to the hosting service and returns the URL pointing to the uploaded file.
     *
     * @param url the URL to upload content from
     * @return the URL pointing to the uploaded file
     */
    abstract fun upload(url: String): String

    /**
     * Deletes the given files from the hosting service.
     *
     * @param files a set of file identifiers to delete
     * @throws FileNotFoundException if any of the files don't exist
     */
    abstract fun delete(files: Set<String>)

    /**
     * Gets the maximum file size allowed by this hosting service in bytes.
     * Returns null if there's no specific limit or the limit is unknown.
     */
    abstract fun getMaxFileSize(): Float?

    /**
     * Gets the name of this hosting service.
     */
    abstract fun getServiceName(): String

    /**
     * Validates if the file name is acceptable for this service.
     * Default implementation checks if the name is not blank.
     */
    open fun validateFileName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    /**
     * Gets the supported file extensions for this service.
     * Returns null if all extensions are supported.
     */
    open fun getSupportedExtensions(): Set<String>? = null

    /**
     * Checks if the given file extension is supported by this service.
     * If no specific extensions are defined, all extensions are considered supported.
     *
     * @param extension the file extension to check
     * @return true if the extension is supported, false otherwise
     */
    open fun isSupportedFileExtension(extension: String): Boolean {
        return getSupportedExtensions()?.contains(extension) != false
    }
}
