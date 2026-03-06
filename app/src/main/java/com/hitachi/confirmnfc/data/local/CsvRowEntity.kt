package com.hitachi.confirmnfc.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CSVから取り込んだ照合用データの1行。
 */
@Entity(
    tableName = "csv_rows",
    indices = [Index(value = ["tag_key"])]
)
data class CsvRowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tag_key")
    val tagKey: String,
    @ColumnInfo(name = "name_value")
    val nameValue: String,
    @ColumnInfo(name = "code_value")
    val codeValue: String
)
