package com.hitachi.confirmnfc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CsvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<CsvRowEntity>)

    @Query("DELETE FROM csv_rows")
    suspend fun clearAll()

    @Query("SELECT * FROM csv_rows WHERE tag_key = :tagKey")
    suspend fun findByTagKey(tagKey: String): List<CsvRowEntity>

    @Query("SELECT COUNT(*) FROM csv_rows")
    suspend fun countAll(): Int
}
