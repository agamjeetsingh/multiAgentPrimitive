import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun runMathLM2(problemStatement: String, verbose: Boolean = false, maxIterations: Int = 1): String? {
    var finalOutput: String? = null
    val outputFile = File("output/examples/output-exp-7.txt")
    val shouldWeStop = createShouldWeStopTool()
    val totalSummary = mutableListOf<String?>()

    coroutineScope {
        var job = launch {
            val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            if (verbose){
                println("Thinking...")
            }
            var thinkerOutput = sendRequest(thinkerAgentSystemPrompt + problemStatement, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")
            thinkerOutput?.let {
                val timestamp = LocalDateTime.now().format(timeFormat)
                outputFile.appendText("\n\n[Thinker Output - $timestamp]\n\n")
            }

            var iterationIndex = 0
            while (iterationIndex < maxIterations){
                iterationIndex += 1
                if (verbose){
                    println("Reflecting...")
                }
                val reflectionAgentPrompt = reflectionAgentSystemPrompt + "\n\nProblem: " + problemStatement + "\n\n-----\n\n" + "The Thinker's Thoughts:\n\n" + thinkerOutput
                val reflectionOutput = sendRequest(reflectionAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")
                reflectionOutput?.let {
                    val timestamp = LocalDateTime.now().format(timeFormat)
                    outputFile.appendText("\n\n[Reflection Output (Iteration $iterationIndex) - $timestamp]\n\n$it")
                }

                if (verbose){
                    println("Organising Thoughts...")
                }
                val curationAgentPrompt = curatorAgentSystemPrompt + "\n\nProblem: " + problemStatement + "\n\n-----\n\n" + "The Thinker's Thoughts:\n\n" + thinkerOutput + "\n\n------\n\n" + "The Reflection Agent's Output:\n\n" + reflectionOutput
                val curatedOutput = sendRequest(curationAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.get(1) // Ignoring the thoughts here
                curatedOutput?.let {
                    val timestamp = LocalDateTime.now().format(timeFormat)
                    outputFile.appendText("\n\n[Curated Output (Iteration $iterationIndex) - $timestamp]\n\n$it")
                }
                totalSummary.add(curatedOutput)

                val stoppingAgentPrompt =
                    "$stoppingAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED SUMMARY]\n$curatedOutput\n[/CURATED SUMMARY]\n\n[REFLECTIONS]\n$reflectionOutput\n[/REFLECTIONS]"

                val stoppingAgentOutput = sendRequest(stoppingAgentPrompt, "gemini-2.0-flash-exp", tools = listOf(shouldWeStop), getResponses = 1)?.get(0)?.lowercase()
                val verdictMap = mapOf(
                    "true" to "STOP!!",
                    "false" to "Keep Going!"
                )
                stoppingAgentOutput?.let {
                    val timestamp = LocalDateTime.now().format(timeFormat)
                    outputFile.appendText("\n\n[Stopping Agent's Verdict (Iteration $iterationIndex) - $timestamp]\n\n${verdictMap[it]}")
                }

                if (iterationIndex >= maxIterations || stoppingAgentOutput == "true"){
                    val refinerAgentPrompt = "$refinerAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED CHAIN OF THOUGHT]\n$curatedOutput\n[/CURATED CHAIN OF THOUGHT]"
                    finalOutput = sendRequest(refinerAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.get(1)
                    finalOutput?.let {
                        val timestamp = LocalDateTime.now().format(timeFormat)
                        outputFile.appendText("\n\n[Final Output - $timestamp]\n\n$it")
                    }
                    break
                }
                if (verbose){
                    println("Thinking...")
                }
                val refreshedThinkerAgentPrompt = refreshedThinkerAgentSystemPrompt + "Problem: " + problemStatement + "\n\n-----\n\n" + "Previous Curated Chain of Thought\n\n" + totalSummary.joinToString("\n")
                thinkerOutput = sendRequest(refreshedThinkerAgentPrompt, "gemini-2.0-flash-thinking-exp-1219", getResponses = 2)?.joinToString("\n")
                thinkerOutput?.let {
                    val timestamp = LocalDateTime.now().format(timeFormat)
                    outputFile.appendText("\n\n[Thinker Output - $timestamp]\n\n$it")
                }
            }
        }

        var timeElapsed = 0
//        while (!job.isCompleted) {
//            print("\rTime elapsed: ${"%.1f".format(timeElapsed / 1000.0)}s")
//            delay(100)
//            timeElapsed += 100
//        }
    }
    return finalOutput
}