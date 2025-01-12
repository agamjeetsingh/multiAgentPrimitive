import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

fun parseResponse(response: String?): String?{
    if (response != null) {
        if (response.startsWith("{")) {
            // Likely a FunctionCall
            val functionCall = Json.decodeFromString<FunctionCall>(response)
            if (functionCall.name == "thoughts") {
                val chainOfThoughtsElement = functionCall.args["chainOfThoughts"]
                if (chainOfThoughtsElement != null) {
                    val chainOfThoughts = chainOfThoughtsElement.jsonArray.map { it.jsonPrimitive.content }
                    // println("Chain of Thoughts:")
                    // chainOfThoughts.forEach { println(it) }
                    return "Chain of Thoughts:\n\n" + chainOfThoughts.joinToString("\n")
                } else {
                    return "Error: 'chainOfThoughts' not found in function call arguments."
                }
            } else {
                return "Error: Unexpected function call: ${functionCall.name}"
            }
        } else {
            // Regular text response
            return "Generated Text: $response"
        }
    }
    return null
}