package com.manikandan.kotlinytmusicscraper.models

sealed class MediaType {
    data object Song : MediaType()

    data object Video : MediaType()
}