import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime


fun main() = runBlocking {
    val prompt = """
        You are an expert AI who specialises in parsing a mathematical PDF document and dividing it into different sections semantically. There are strictly 4 "sections" of a Mathematical Document:

        1. Theory: This consists of explanatory text or background material.
        2. Example: This consists of an example problem and an example solution / walkthrough
        3. Problem: This consists of an exercise problem
        4. Problem Solution: This consists of the exercise problem's solution. This is in a separate category as it may occur much later (e.g. at the end) in the PDF

        In order to distinguish between these types, you must output everything by calling their respective functions. Problem and Problem Solution come with Problem IDs and these must strictly be positive integers starting from 1. These are NOT related to what comes in the LaTeX document but will be used to link problem's with their respective solutions.
    """.trimIndent()
    val modelName = "gemini-2.0-flash-exp"
    val pdfFilePath = "LM.pdf" // Replace with your PDF file path

    // Upload the PDF and get the assigned name
    val uploadedFileName = uploadPdfToApi(pdfFilePath)

    if (uploadedFileName != null) {
        // Wait for the file to be processed
        waitForFileActivation(uploadedFileName)

        // Use the uploaded file name in the sendRequest function
        val response = sendRequest(
            modelName = modelName,
            promptText = prompt,
            pdfFileName = uploadedFileName // Corrected parameter name
        )

        println("API Response: $response")
    } else {
        println("Failed to upload PDF.")
    }
}

//suspend fun main(){
//    val prompt = """
//        You are an expert AI who specialises in parsing a mathematical PDF document and dividing it into different sections semantically. There are strictly 4 "sections" of a Mathematical Document:
//
//        1. Theory: This consists of explanatory text or background material.
//        2. Example: This consists of an example problem and an example solution / walkthrough
//        3. Problem: This consists of an exercise problem
//        4. Problem Solution: This consists of the exercise problem's solution. This is in a separate category as it may occur much later (e.g. at the end) in the PDF
//
//        In order to distinguish between these types, you must output everything by calling their respective functions. Problem and Problem Solution come with Problem IDs and these must strictly be positive integers starting from 1. These are NOT related to what comes in the LaTeX document but will be used to link problem's with their respective solutions.
//    """.trimIndent()
//    val output = sendRequest1(prompt, "gemini-2.0-flash-exp", pdfFilePath = "LM.pdf")
//    output?.forEach{ println(it) }
//}

//suspend fun main(){
//    val file = File("AIMOpublic.txt")
//    val fileContent = file.readLines()
//    val file2 = File("answers-AIMOpublic-mathLMv2-max3.txt")
//    var i = 0
//    while (2*i < fileContent.size){
//        file2.appendText("Problem ${i+1}:\n")
//        val problemStatement = fileContent[2*i]
//        val answer = runMathLM2(problemStatement, maxIterations = 10)
////        val maxIterations = 25
////        var iterations = 0
////        while ((answers.size == 0 || answers.size == answers.toSet().size) && iterations < maxIterations){
////            val output = runMathLM2(problemStatement, maxIterations = 3)
////            val refinerAgentPrompt = "$refinerAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED CHAIN OF THOUGHT]\n$output\n[/CURATED CHAIN OF THOUGHT]"
////            val finalOutput = sendRequest(refinerAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.get(1)
////            answers.add(finalOutput)
////            iterations += 1
////        }
////        val answer = findDuplicate(answers)
//        answer?.let {
//            file2.appendText("\n\nFinal Answer: $it\n\n")
//        }
//        i += 1
//    }
//}
//
////suspend fun main(){
////    val file = File("AIMOpublic.txt")
////    val fileContent = file.readLines()
////    val file2 = File("answers-AIMOpublic.txt")
////    var i = 6
////    while (2*i < fileContent.size){
////        file2.appendText("Problem ${i+1}:\n")
////        val problemStatement = fileContent[2*i]
////        val answers = mutableListOf<String?>()
////        val maxIterations = 25
////        var iterations = 0
////        while ((answers.size == 0 || answers.size == answers.toSet().size) && iterations < maxIterations){
////            val output = sendRequest(problemStatement, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")
////            val refinerAgentPrompt = "$refinerAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED CHAIN OF THOUGHT]\n$output\n[/CURATED CHAIN OF THOUGHT]"
////            val finalOutput = sendRequest(refinerAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.get(1)
////            answers.add(finalOutput)
////            iterations += 1
////        }
////        val answer = findDuplicate(answers)
////        answer?.let {
////            file2.appendText("\n\nFinal Answer: $it (All Answers: $answers)")
////        }
////        i += 1
////    }
////
////}
//
//fun <T> findDuplicate(list: List<T>): T? {
//    val seen = mutableSetOf<T>()
//    for (item in list) {
//        if (!seen.add(item)) {
//            return item // This is the duplicate element
//        }
//    }
//    return null // No duplicates found
//}


//suspend fun main(){
//    val file = File("otis-func.txt")
//    val fileContent = file.readText()
//    val example1 = File("output-exp-4-short.txt").readText()
//    val problemStatement = "You are a Math Olympiad Aspirant who is being taught how to solve functional equations. You have already solved Example 2.1 (Indian Postal Set 2016). Here are the insights you gained from solving it:$example1\n\n\n Now go ahead and try Example 2.2 (ELMO 2014/1)\n\n$fileContent"
//    val output = runMathLM2(problemStatement, maxIterations = 10)
//    println(output)
//}

//fun main() {
//    val answersFile = File("answers-v2.txt")
//    val dataFile = File("livebench_data.json")
//    val jsonString = dataFile.readText()
//
//// Deserialize the JSON back into a List<RowData>
//    val loadedRows = Json.decodeFromString<List<RowData>>(jsonString)
//
//    var i = 235
//    while (i < 245){
//        i += 1
//        val problemID = loadedRows[i].question_id
//        if (loadedRows[i].livebench_removal_date?.isEmpty() == false){
//            answersFile.appendText("Problem ${i+1} ($problemID) has already been removed from LiveBench.")
//            continue
//        }
//        answersFile.appendText("\n-------\n")
//        answersFile.appendText("Problem ${i+1}. ID is $problemID\n")
//        println("Solving problem ${i+1}. ID is $problemID\n")
//        answersFile.appendText("mathLMv2's Answer: ")
//        runBlocking {
//            runMathLM2(loadedRows[i].turns[0])?.let { answersFile.appendText(it) }
//        }
//        answersFile.appendText("Gemini thinking's Answer:")
//        runBlocking {
//            sendRequest(loadedRows[i].turns[0], "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")?.let {answersFile.appendText(it)}
//        }
//        val correctAnswer = loadedRows[i].ground_truth
//        answersFile.appendText("Correct Answer: $correctAnswer")
//    }
//}