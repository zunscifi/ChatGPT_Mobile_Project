package com.project.toandt.Control.Translate

class LanguageIdentificationException : Exception {

  constructor(message: String) : super(message)

  constructor(cause: Exception) : super(cause)
}

class ModelDownloadException(cause: Exception) : Exception(cause)

class TranslationException(cause: Exception) : Exception(cause)