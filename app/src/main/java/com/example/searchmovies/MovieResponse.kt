package com.example.searchmovies

data class MovieResponse(
    val message: String,
    val movie_info: MovieInfo
)

data class MovieInfo(
    val title: String,
    val overview: String,
    val release_date: String,
    val poster_path: String?
)