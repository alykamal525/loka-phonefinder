package com.loka.phonefinder

data class Provider(
    val id: String,
    val name: String,
    val category: String,
    val type: String,
    val url: String,
    val homepage: String,
    val noteEn: String,
    val noteAr: String,
    val egypt: String,
    val status: String,
    val lastChecked: String,
    val lastResult: String,
    val modes: List<String>
)

enum class CardStatus(val ar: String, val en: String) {
    READY("جاهز", "READY"),
    SUCCESS("نجاح", "SUCCESS"),
    EXTERNAL("بحث خارجي", "EXTERNAL SEARCH"),
    NO_RESULTS("لا نتائج", "NO RESULTS"),
    UNAVAILABLE("غير متاح", "UNAVAILABLE"),
    AUTH("يتطلب مصادقة", "AUTHENTICATION REQUIRED"),
    ERROR("خطأ", "ERROR")
}

data class CardState(
    val status: CardStatus = CardStatus.READY,
    val checkedAt: Long = 0L,
    val durationMs: Long = -1L
)
