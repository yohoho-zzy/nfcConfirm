package com.hitachi.confirmnfc.repository

import android.content.Context
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.data.local.AppDatabase
import com.hitachi.confirmnfc.data.local.CsvRowEntity
import com.hitachi.confirmnfc.util.CsvKeyNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * ログイン時にCSVデータを取得するRepositoryクラス
 *
 * 主な処理:
 * - CloudFront上のCSVへBasic認証付きでアクセス
 * - 通信失敗時は最大3回までリトライ
 * - CSVを1行ずつ解析してRoomへ逐次保存
 */
class LoginRepository(
    /** エラーメッセージ取得に使うContext */
    private val context: Context
) {

    private val csvDao = AppDatabase.getInstance(context).csvDao()

    /** 取得対象CSVのURL */
    private val csvUrl = "https://d2kkch5g6rdzfp.cloudfront.net/abcdefg.csv"

    /**
     * CSVデータを取得してパース結果を返す
     */
    suspend fun fetchCsv(
        userId: String,
        phoneNumber: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val maxRetry = 3

        repeat(maxRetry) { index ->
            try {
                // 認証付きのHTTP接続を組み立てる。
                val connection = (URL(csvUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"

                    // userId:phoneNumber をBase64化してAuthorizationヘッダーへ設定。
                    val token = Base64.getEncoder()
                        .encodeToString("$userId:$phoneNumber".toByteArray())
                    setRequestProperty("Authorization", "Basic $token")
                }

                // HTTPステータスを評価し、2xxのみ成功扱いにする。
                val code = connection.responseCode

                if (code in 200..299) {
                    csvDao.clearAll()
                    var inserted = 0
                    val chunk = ArrayList<CsvRowEntity>(1000)

                    connection.inputStream.bufferedReader().use { reader ->
                        reader.lineSequence().forEach { line ->
                            if (line.isBlank()) return@forEach
                            val cols = line.split(",").map { it.trim() }
                            if (cols.isEmpty()) return@forEach

                            val tagKey = CsvKeyNormalizer.normalize(cols.getOrNull(0).orEmpty())
                            if (tagKey.isBlank()) return@forEach

                            chunk.add(
                                CsvRowEntity(
                                    tagKey = tagKey,
                                    nameValue = cols.getOrNull(1).orEmpty().trim(),
                                    codeValue = cols.getOrNull(2).orEmpty().trim()
                                )
                            )

                            if (chunk.size >= 1000) {
                                csvDao.insertAll(chunk)
                                inserted += chunk.size
                                chunk.clear()
                            }
                        }
                    }

                    if (chunk.isNotEmpty()) {
                        csvDao.insertAll(chunk)
                        inserted += chunk.size
                        chunk.clear()
                    }

                    if (inserted > 0) {
                        return@withContext Result.success(Unit)
                    }

                    return@withContext Result.failure(IllegalStateException(context.getString(R.string.msgCsvEmpty)))
                }
            } catch (_: Exception) { }

            // 最終試行以外は少し待ってから再試行する
            if (index < maxRetry - 1) {
                delay(1500)
            }
        }

        // すべての試行が失敗した場合の最終エラー
        Result.failure(
            IllegalStateException(
                context.getString(R.string.msgCsvFetchFailed)
            )
        )
    }
}
