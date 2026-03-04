package com.hitachi.confirmnfc.model

/**
 * CSVの1行分を保持するデータモデル
 */
data class CsvRecord(
    /** カンマ分割後の列一覧 */
    val columns: List<String>
)
