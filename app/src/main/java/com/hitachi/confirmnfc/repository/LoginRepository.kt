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
 * ログイン時にCSVデータを取得するRepositoryクラス
 *
 * ・CloudFront上のCSVファイルを取得
 * ・Basic認証を使用してアクセス
 * ・最大3回までリトライ
 * ・CSVをCsvRecordのリストへ変換して返却
 */
class LoginRepository(private val context: Context) {

    // 取得対象のCSVファイルURL
    private val csvUrl = "https://d2kkch5g6rdzfp.cloudfront.net/abcdefg.csv"

    /**
     * CSVデータを取得する
     *
     * @param userId       ユーザーID（Basic認証用）
     * @param phoneNumber  電話番号（Basic認証用）
     * @return Result<List<CsvRecord>>
     */
    suspend fun fetchCsv(
        userId: String,
        phoneNumber: String
    ): Result<List<CsvRecord>> = withContext(Dispatchers.IO) {

        // 最大リトライ回数
        val maxRetry = 3

        repeat(maxRetry) { index ->
            try {

                // HTTP接続を生成
                val connection = (URL(csvUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000   // 接続タイムアウト（5秒）
                    readTimeout = 5000      // 読み取りタイムアウト（5秒）
                    requestMethod = "GET"

                    // Basic認証ヘッダーを設定
                    val token = Base64.getEncoder()
                        .encodeToString("$userId:$phoneNumber".toByteArray())
                    setRequestProperty("Authorization", "Basic $token")
                }

                // レスポンスコード取得
                val code = connection.responseCode

                // HTTP 2xx の場合のみ成功扱い
                if (code in 200..299) {

                    // CSVを1行ずつ読み込み
                    val records = connection.inputStream
                        .bufferedReader()
                        .use(BufferedReader::readLines)
                        .filter { it.isNotBlank() } // 空行除外
                        .map { line ->
                            // カンマ区切りで分割し、前後の空白を除去
                            CsvRecord(
                                line.split(",").map { it.trim() }
                            )
                        }

                    // データが存在する場合は成功
                    if (records.isNotEmpty()) {
                        return@withContext Result.success(records)
                    }

                    // CSVが空の場合
                    return@withContext Result.failure(
                        IllegalStateException(
                            context.getString(R.string.csv_empty)
                        )
                    )
                }

            } catch (_: Exception) {
                // 例外発生時はリトライへ
            }

            // 最終リトライでない場合は1.5秒待機
            if (index < maxRetry - 1) {
                delay(1500)
            }
        }

        // すべてのリトライが失敗した場合
        Result.failure(
            IllegalStateException(
                context.getString(R.string.csv_fetch_failed)
            )
        )
    }
}