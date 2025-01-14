import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class RowData(
    val question_id: String,
    val category: String,
    val ground_truth: String,
    val turns: List<String>,
    val task: String,
    val subtask: String,
    val livebench_release_date: String,
    val livebench_removal_date: String? = null,
    val expressions: String? = null,
    val release_date: Int? = null,
    val year: String? = null,
    val hardness: Double? = null
)

@Serializable
data class DatasetRow(
    val row_idx: Int,
    val row: RowData
)

@Serializable
data class DatasetResponse(
    val rows: List<DatasetRow>
)

fun generateUrls(dataset: String, config: String, split: String, totalRows: Int, length: Int): List<String> {
    val baseUrl = "https://datasets-server.huggingface.co/rows"
    val urls = mutableListOf<String>()
    var offset = 0

    while (offset < totalRows) {
        val currentLength = if (offset + length <= totalRows) length else totalRows - offset
        val url = "$baseUrl?dataset=$dataset&config=$config&split=$split&offset=$offset&length=$currentLength"
        urls.add(url)
        offset += length
    }

    return urls
}

suspend fun fetchAndProcessData(urls: List<String>): List<RowData> {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val allRows = mutableListOf<RowData>()

    for (url in urls) {
        try {
            val response: HttpResponse = client.get(url)

            if (response.status.isSuccess()) {
                val datasetResponse = response.body<DatasetResponse>()
                for (datasetRow in datasetResponse.rows) {
                    allRows.add(datasetRow.row)
                }
            } else {
                println("Error fetching data from $url: ${response.status}")
                println(response.bodyAsText())
            }
        } catch (e: Exception) {
            println("Error during API call to $url: $e")
        }
    }

    client.close()
    return allRows
}

fun main() {
    val urls = generateUrls("livebench/math", "default", "test", 368, 100)

    runBlocking {
        val allResponses = async { fetchAndProcessData(urls) }
        val allRows = allResponses.await() // Wait for the data fetching to complete
        // Serialize allRows to JSON
        val jsonString = Json.encodeToString(allRows)

        // Save to a file (e.g., "livebench_data.json")
        val dataFile = File("livebench_data.json")
        dataFile.writeText(jsonString)

    }
}