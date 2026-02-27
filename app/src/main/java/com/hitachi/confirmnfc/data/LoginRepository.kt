package com.hitachi.confirmnfc.data

import android.content.Context
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.model.CsvRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class LoginRepository(private val context: Context) {
    private val csvUrl = "https://d2kkch5g6rdzfp.cloudfront.net/abcdefg.csv"

    suspend fun fetchCsv(userId: String, phoneNumber: String): Result<List<CsvRecord>> = withContext(Dispatchers.IO) {
        val maxRetry = 3
        repeat(maxRetry) { index ->
            try {
                val connection = (URL(csvUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    val token = Base64.getEncoder().encodeToString("$userId:$phoneNumber".toByteArray())
                    setRequestProperty("Authorization", "Basic $token")
                }

                val code = connection.responseCode
                if (code in 200..299) {
                    val records = connection.inputStream.bufferedReader().use(BufferedReader::readLines)
                        .filter { it.isNotBlank() }
                        .map { line -> CsvRecord(line.split(",").map { it.trim() }) }
                    if (records.isNotEmpty()) {
                        return@withContext Result.success(records)
                    }
                    return@withContext Result.failure(
                        IllegalStateException(context.getString(R.string.csv_empty))
                    )
                }
            } catch (_: Exception) {
            }
            if (index < maxRetry - 1) {
                delay(1500)
            }
        }
        Result.failure(IllegalStateException(context.getString(R.string.csv_fetch_failed)))
    }
}
