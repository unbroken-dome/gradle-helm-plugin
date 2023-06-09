package com.citi.gradle.plugins.helm.publishing.util

import okhttp3.MultipartBody
import okhttp3.RequestBody

fun RequestBody.toMultipartBody(filename: String) = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addPart(MultipartBody.Part.createFormData("file", filename, this))
    .build()
