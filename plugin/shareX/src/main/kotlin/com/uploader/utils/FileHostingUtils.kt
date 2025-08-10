package com.uploader.utils

/**
 * Utility functions for file hosting services.
 */

/**
 * Converts a set of file names to a Catbox-compatible string format.
 * Removes spaces from file names and joins them with spaces.
 */
fun Set<String>.toCatboxFiles(): String = this.map { it.replace(" ", "") }.joinToString(" ")
