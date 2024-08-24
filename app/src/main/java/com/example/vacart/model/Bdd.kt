package com.example.vacart.model

data class Bdd(
    val berthCode: String,
    val berthNo: Int,
    val bsd: List<Bsd>,
    val cabinCoupe: Any,
    val cabinCoupeNameNo: String,
    val enable: Boolean,
    val from: String,
    val quotaCntStn: Any,
    val to: String
)