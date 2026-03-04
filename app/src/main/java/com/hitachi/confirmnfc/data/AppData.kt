package com.hitachi.confirmnfc.data

import com.hitachi.confirmnfc.model.CsvRecord

/**
 * アプリ全体で共有するインメモリデータ
 */
object AppData {
    /** ログインで取得したCSVレコード */
    var csvRecords: List<CsvRecord> = emptyList()
}
