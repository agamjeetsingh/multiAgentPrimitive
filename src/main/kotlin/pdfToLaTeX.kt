import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

suspend fun sendRequest1(promptText: String, modelName: String, tools: List<Tool>? = null, getResponses: Int = 1, pdfFilePath: String? = null, generationConfig: GenerateConfig? = null): List<String?>? {
    val apiKey = System.getenv("GEMINI_API_KEY")

    if (apiKey == null) {
        println("Error: GEMINI_API_KEY environment variable not found!")
        return null
    }

    val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 1800000 // Set to 180 seconds
            socketTimeoutMillis = 1800000
            requestTimeoutMillis = 1800000
        }
    }

    var base64FileData: String? = null
    if (pdfFilePath != null) {
        val file = File(pdfFilePath)
        if (file.exists()) {
            base64FileData = Base64.getEncoder().encodeToString(file.readBytes())
        } else {
            println("Error: PDF file not found at $pdfFilePath")
            return null
        }
    }

    val requestData = GenerateContentRequest(
        contents = listOf(
            Content(
                parts = listOfNotNull(
                    if (pdfFilePath != null) Part(fileData = FileData("application/pdf", base64FileData ?: "")) else null,
                    if (promptText != null) Part(text = promptText) else null
                )
            )
        ),
        tools = tools,
        generationConfig = generationConfig
    )

    try {
        val response: HttpResponse = client.post(apiUrl) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            parameter("key", apiKey)
            setBody(requestData)
        }

        if (response.status.isSuccess()) {
            val apiResponse: GenerateContentResponse = response.body()
            val outputs = mutableListOf<String>()

            // Iterate through candidates and parts
            apiResponse.candidates?.forEach { candidate ->
                candidate.content.parts.forEach { part ->
                    if (part.text != null) {
                        outputs.add(part.text)
                    } else if (part.functionCall != null) {
                        outputs.add(Json.encodeToString(part.functionCall))
                    }
                }
            }
            client.close()
            return outputs

        } else {
            println("Error: ${response.status}")
            println(response.bodyAsText())
            client.close()
            return null
        }

    } catch (e: Exception) {
        // Catch all network/IO/parsing errors
        println("Error during API call: $e")
        client.close()
        return null
    }
}

suspend fun main(){
    val prompt = """
        You are an expert AI who specialises in parsing a mathematical PDF document and dividing it into different sections semantically. There are strictly 4 "sections" of a Mathematical Document:

        1. Theory: This consists of explanatory text or background material.
        2. Example: This consists of an example problem and an example solution / walkthrough
        3. Problem: This consists of an exercise problem
        4. Problem Solution: This consists of the exercise problem's solution. This is in a separate category as it may occur much later (e.g. at the end) in the PDF

        In order to distinguish between these types, you must output everything by calling their respective functions. Problem and Problem Solution come with Problem IDs and these must strictly be positive integers starting from 1. These are NOT related to what comes in the LaTeX document but will be used to link problem's with their respective solutions.
    """.trimIndent()
    val output = sendRequest1(prompt, "gemini-2.0-flash-exp", pdfFilePath = "LM.pdf")
    output?.forEach{ println(it) }
}