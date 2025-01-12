import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

@Serializable
data class Tool(val functionDeclarations: List<FunctionDeclaration>)

@Serializable
data class FunctionDeclaration(val name: String, val description: String, val parameters: Parameters)

@Serializable
data class Parameters(val type: String, val properties: Map<String, Property>? = null, val items: Items? = null)

@Serializable
data class Property(val type: String)

@Serializable
data class Items(val type: String)

@Serializable
data class FunctionCall(val name: String, val args: Map<String, JsonElement>)

@Serializable
data class FunctionResponsePart(val name: String, val response: String)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String? = null, val functionCall: FunctionCall? = null, val functionResponse: FunctionResponsePart? = null, val fileData: FileData? = null)

@Serializable
data class Candidate(val content: Content)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>?,
    val functionCall: FunctionCall? = null
)

@Serializable
data class GenerateContentRequest(val contents: List<Content>, val tools: List<Tool>? = null, val generationConfig: GenerateConfig? = null)

@Serializable
data class GenerateConfig(
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
)

@Serializable
data class FileData(
    val mimeType: String,
    @SerialName("base64")
    val data: String
)

@Serializable
data class UploadFileResponse(
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    val sizeBytes: String,
    val createTime: String,
    val updateTime: String,
    val expireTime: String? = null,
    val startTime: String? = null,
    val state: String
)

suspend fun uploadPdfToApi(
    pdfFilePath: String
): String? {
    val apiKey = System.getenv("GEMINI_API_KEY")

    if (apiKey == null) {
        println("Error: GEMINI_API_KEY environment variable not found!")
        return null
    }

    val apiUrl = "https://generativelanguage.googleapis.com/v1beta/files?key=$apiKey"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    val file = File(pdfFilePath)
    if (!file.exists()) {
        println("Error: PDF file not found at $pdfFilePath")
        return null
    }

    val base64FileData = Base64.getEncoder().encodeToString(file.readBytes())

    // Create the request body as a JSON object
    val requestBody = """
        {
          "mimeType": "application/pdf",
          "data": "$base64FileData"
        }
    """.trimIndent()

    try {
        val response: HttpResponse = client.post(apiUrl) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            parameter("key", apiKey)
            setBody(requestBody)
        }

        if (response.status.isSuccess()) {
            val uploadResponse: UploadFileResponse = response.body()
            println("File uploaded successfully. Name: ${uploadResponse.name}")
            return uploadResponse.name
        } else {
            println("Error uploading file: ${response.status}")
            println(response.bodyAsText())
            return null
        }
    } catch (e: Exception) {
        println("Error during API call: $e")
        return null
    } finally {
        client.close()
    }
}

suspend fun waitForFileActivation(fileId: String) {
    val apiKey = System.getenv("GEMINI_API_KEY")
    if (apiKey == null) {
        println("Error: GEMINI_API_KEY environment variable not found!")
        return
    }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    println("Waiting for file processing...")
    while (true) {
        try {
            val response: HttpResponse = client.get("https://generativelanguage.googleapis.com/v1beta/$fileId?key=$apiKey") {
                parameter("key", apiKey)
            }

            if (response.status.isSuccess()) {
                val fileResponse: UploadFileResponse = response.body()
                when (fileResponse.state) {
                    "ACTIVE" -> {
                        println("...file processing complete")
                        println()
                        client.close()
                        return
                    }
                    "PROCESSING" -> {
                        print(".")
                        delay(10000) // Wait for 10 seconds before polling again
                    }
                    else -> {
                        println("Error: File processing failed with state: ${fileResponse.state}")
                        client.close()
                        return
                    }
                }
            } else {
                println("Error checking file status: ${response.status}")
                println(response.bodyAsText())
                client.close()
                return
            }
        } catch (e: Exception) {
            println("Error during API call: $e")
            client.close()
            return
        }
    }
}

suspend fun sendRequest(
    modelName: String,
    promptText: String? = null,
    tools: List<Tool>? = null,
    generationConfig: GenerateConfig? = null,
    pdfFileName: String? = null // Change the parameter name
): String? {
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
    }

    // Prepare the parts for the request
    val parts = mutableListOf<Part>().apply {
        pdfFileName?.let { add(Part(fileData = FileData("application/pdf", it))) }
        promptText?.let { add(Part(text = it)) }
    }

    val requestData = GenerateContentRequest(
        contents = listOf(Content(parts = parts)),
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
            if (functionCallFromParts != null) {
                println("Function call requested: ${functionCallFromParts.name}")
                client.close()
                return Json.encodeToString(functionCallFromParts)
            }
            val generatedText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
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

fun main() = runBlocking {
    val prompt = "Summarize this document"
    val modelName = "gemini-1.5-pro-latest"
    val pdfFilePath = "file.pdf" // Replace with your PDF file path

    // Upload the PDF and get the assigned name
    val uploadedFileName = uploadPdfToApi(pdfFilePath)

    if (uploadedFileName != null) {
        // Wait for the file to be processed
        waitForFileActivation(uploadedFileName)

        // Use the uploaded file name in the sendRequest function
        val response = sendRequest(
            modelName = modelName,
            promptText = prompt,
            pdfFileName = uploadedFileName // Pass the file name here
        )

        println("API Response: $response")
    } else {
        println("Failed to upload PDF.")
    }
}