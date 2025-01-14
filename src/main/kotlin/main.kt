import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.runBlocking

suspend fun main(){
    val file = File("input/AIMOpublic.txt")
    val fileContent = file.readLines()
    val file2 = File("output/answers-AIMOpublic-mathLMv2-max3.txt")
    var i = 0
    while (2*i < fileContent.size){
        file2.appendText("Problem ${i+1}:\n")
        val problemStatement = fileContent[2*i]
        val answer = runMathLM2(problemStatement, maxIterations = 10)
        answer?.let {
            file2.appendText("\n\nFinal Answer: $it\n\n")
        }
        i += 1
    }
}

suspend fun mainOld1(){
    val file = File("input/AIMOpublic.txt")
    val fileContent = file.readLines()
    val file2 = File("output/answers-AIMOpublic.txt")
    var i = 6
    while (2*i < fileContent.size){
        file2.appendText("Problem ${i+1}:\n")
        val problemStatement = fileContent[2*i]
        val answers = mutableListOf<String?>()
        val maxIterations = 25
        var iterations = 0
        while ((answers.size == 0 || answers.size == answers.toSet().size) && iterations < maxIterations){
            val output = sendRequest(problemStatement, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")
            val refinerAgentPrompt = "$refinerAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED CHAIN OF THOUGHT]\n$output\n[/CURATED CHAIN OF THOUGHT]"
            val finalOutput = sendRequest(refinerAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.get(1)
            answers.add(finalOutput)
            iterations += 1
        }
        val answer = findDuplicate(answers)
        answer?.let {
            file2.appendText("\n\nFinal Answer: $it (All Answers: $answers)")
        }
        i += 1
    }

}

fun <T> findDuplicate(list: List<T>): T? {
    val seen = mutableSetOf<T>()
    for (item in list) {
        if (!seen.add(item)) {
            return item // This is the duplicate element
        }
    }
    return null // No duplicates found
}

fun mainOld2() {
    val answersFile = File("output/answers-v2.txt")
    val dataFile = File("input/livebench_data.json")
    val jsonString = dataFile.readText()

// Deserialize the JSON back into a List<RowData>
    val loadedRows = Json.decodeFromString<List<RowData>>(jsonString)

    var i = 235
    while (i < 245){
        i += 1
        val problemID = loadedRows[i].question_id
        if (loadedRows[i].livebench_removal_date?.isEmpty() == false){
            answersFile.appendText("Problem ${i+1} ($problemID) has already been removed from LiveBench.")
            continue
        }
        answersFile.appendText("\n-------\n")
        answersFile.appendText("Problem ${i+1}. ID is $problemID\n")
        println("Solving problem ${i+1}. ID is $problemID\n")
        answersFile.appendText("mathLMv2's Answer: ")
        runBlocking {
            runMathLM2(loadedRows[i].turns[0])?.let { answersFile.appendText(it) }
        }
        answersFile.appendText("Gemini thinking's Answer:")
        runBlocking {
            sendRequest(loadedRows[i].turns[0], "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")?.let {answersFile.appendText(it)}
        }
        val correctAnswer = loadedRows[i].ground_truth
        answersFile.appendText("Correct Answer: $correctAnswer")
    }
}