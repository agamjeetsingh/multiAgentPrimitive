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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.util.*

@Serializable
data class Tool(val functionDeclarations: List<FunctionDeclaration>)

@Serializable
data class FunctionDeclaration(val name: String, val description: String, val parameters: Parameters)

@Serializable
data class Parameters(val type: String, val properties: Map<String, Property>? = null, val items: Items? = null)

@Serializable
data class Property(val type: String, val items: Items? = null)

@Serializable
data class Items(val type: String)

@Serializable
data class FunctionCall(val name: String, val args: Map<String, JsonElement>)

@Serializable
data class FunctionResponsePart(val name: String, val response: String)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponsePart? = null,
    val fileData: FileData? = null
)

@Serializable
data class FileData(
    val mimeType: String,
    @SerialName("base64")
    val data: String
)

@Serializable
data class Candidate(val content: Content)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>?,
    val functionCall: FunctionCall? = null
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null,
    val generationConfig: GenerateConfig? = null
)

@Serializable
data class GenerateConfig(
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
)

suspend fun sendRequest(promptText: String, modelName: String, tools: List<Tool>? = null, getResponses: Int = 1, pdfFilePath: String? = null, generationConfig: GenerateConfig? = null): List<String?>? {
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
            val functionCallFromParts = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.functionCall
            if(functionCallFromParts != null) {
//                println("Function call requested ${functionCallFromParts.name}")
                client.close()
                return listOf(functionCallFromParts.args["stopNow"].toString())
            }
            var generatedText = mutableListOf<String?>()
            var i = 0
            while (i < getResponses){
                generatedText.add(apiResponse.candidates?.firstOrNull()?.content?.parts?.getOrNull(i)?.text)
                i += 1
            }
            client.close()
            return generatedText

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