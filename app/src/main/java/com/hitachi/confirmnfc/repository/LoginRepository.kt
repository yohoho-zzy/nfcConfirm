package com.hitachi.confirmnfc.repository

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

/**
 * ログイン時にCSVデータを取得するRepositoryクラス。
 *
 * 主な処理:
 * - CloudFront上のCSVへBasic認証付きでアクセス
 * - 通信失敗時は最大3回までリトライ
 * - CSVを1行ずつ `CsvRecord` へ変換して返却
 */
class LoginRepository(
    /** エラーメッセージ取得に使うContext。 */
    private val context: Context
) {

    /** 取得対象CSVのURL。 */
    private val csvUrl = "https://d2kkch5g6rdzfp.cloudfront.net/abcdefg.csv"

    /**
     * CSVデータを取得してパース結果を返す。
     */
    suspend fun fetchCsv(
        userId: String,
        phoneNumber: String
    ): Result<List<CsvRecord>> = withContext(Dispatchers.IO) {
        // 一時的な通信障害を考慮し、最大3回リトライする。
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
                    // レスポンス本文をCSVとして読み取り、空行を除外する。
                    val records = connection.inputStream
                        .bufferedReader()
                        .use(BufferedReader::readLines)
                        .filter { it.isNotBlank() }
                        .map { line ->
                            CsvRecord(
                                line.split(",").map { it.trim() }
                            )
                        }

                    // レコードが1件以上あれば成功として返却する。
                    if (records.isNotEmpty()) {
                        return@withContext Result.success(records)
                    }

                    // 正常レスポンスだがデータが空の場合は業務エラーとする。
                    return@withContext Result.failure(
                        IllegalStateException(
                            context.getString(R.string.csv_empty)
                        )
                    )
                }
            } catch (_: Exception) {
                // 通信失敗時はリトライで吸収する。
            }

            // 最終試行以外は少し待ってから再試行する。
            if (index < maxRetry - 1) {
                delay(1500)
            }
        }

        // すべての試行が失敗した場合の最終エラー。
        Result.failure(
            IllegalStateException(
                context.getString(R.string.csv_fetch_failed)
            )
        )
    }
}
