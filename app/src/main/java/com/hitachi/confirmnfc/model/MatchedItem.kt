package com.hitachi.confirmnfc.model

/**
 * 検索結果（複数行マッチする可能性あり）
 */
data class MatchedItem(
    val nameValue: String,
    val codeValue: String,
    val record: CsvRecord
)
