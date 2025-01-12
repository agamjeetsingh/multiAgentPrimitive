//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.launch
//import java.io.File
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//
//suspend fun runMathLM1(problemStatement: String): String? {
//    val thoughtsTool = createThoughtsTool()
//    var finalOutput: String? = null
//    val outputFile = File("output.txt")
//
//    coroutineScope {
//        var job = launch {
//            val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//
//            // println("\nThinking...")
//            var thinkerOutput = sendRequest(thinkerAgentSystemPrompt + problemStatement, "gemini-2.0-flash-exp", tools = listOf(thoughtsTool), getResponses = 1)?.get(0)
//            thinkerOutput = parseResponse(thinkerOutput)
//            thinkerOutput?.let {
//                val timestamp = LocalDateTime.now().format(timeFormat)
//                outputFile.appendText("\n\n[Thinker Output - $timestamp]\n\n$it")
//                // println("Thinker output saved to output.txt")
//            }
//
//            val maxIterations = 3
//            var iterationIndex = 0
//            while (iterationIndex < maxIterations){
//                iterationIndex += 1
//                // println("\nReflecting...")
//                val reflectionAgentPrompt = reflectionAgentSystemPrompt + "\n\nProblem: " + problemStatement + "\n\n-----\n\n" + "The Thinker's Thoughts:\n\n" + thinkerOutput
//                val reflectionOutput = sendRequest(reflectionAgentPrompt, "gemini-2.0-flash-exp", getResponses = 2)?.joinToString("\n")
//                reflectionOutput?.let {
//                    val timestamp = LocalDateTime.now().format(timeFormat)
//                    outputFile.appendText("\n\n[Reflection Output (Iteration $iterationIndex) - $timestamp]\n\n$it")
//                    // println("Reflection output for iteration $iterationIndex saved to output.txt")
//                }
//
//                val curationAgentPrompt = curatorAgentSystemPrompt + "\n\nProblem: " + problemStatement + "\n\n-----\n\n" + "The Thinker's Thoughts:\n\n" + thinkerOutput + "\n\n------\n\n" + "The Reflection Agent's Output:\n\n" + reflectionOutput
//                // println("\nOrganising thoughts...")
//                val curatedOutput = sendRequest(curationAgentPrompt, "gemini-2.0-flash-exp", getResponses = 2)?.joinToString("\n")
//                curatedOutput?.let {
//                    val timestamp = LocalDateTime.now().format(timeFormat)
//                    outputFile.appendText("\n\n[Curated Output (Iteration $iterationIndex) - $timestamp]\n\n$it")
//                    // println("Curated output for iteration $iterationIndex saved to output.txt")
//                }
//
//                if (iterationIndex >= maxIterations){
//                    val refinerAgentPrompt = "$refinerAgentSystemPrompt\n\n[PROBLEM]\n$problemStatement\n[/PROBLEM]\n\n[CURATED CHAIN OF THOUGHT]\n$curatedOutput\n[/CURATED CHAIN OF THOUGHT]"
//                    finalOutput = sendRequest(refinerAgentPrompt, "gemini-2.0-flash-exp", getResponses = 2)?.joinToString("\n")
//                    finalOutput?.let {
//                        val timestamp = LocalDateTime.now().format(timeFormat)
//                        outputFile.appendText("\n\n[Final Output - $timestamp]\n\n$it")
//                        // println("Final output saved to output.txt")
//                    }
//                    // print(finalOutput)
//                    break
//                }
//                // println("\nThinking for the ${iterationIndex+1} time")
//                val refreshedThinkerAgentPrompt = refreshedThinkerAgentSystemPrompt + "Problem: " + problemStatement + "\n\n-----\n\n" + "Previous Curated Chain of Thought\n\n" + curatedOutput
//                thinkerOutput = sendRequest(refreshedThinkerAgentPrompt, "gemini-2.0-flash-exp",listOf(thoughtsTool), getResponses = 2)?.joinToString("\n")
//                thinkerOutput = parseResponse(thinkerOutput)
//                thinkerOutput?.let {
//                    val timestamp = LocalDateTime.now().format(timeFormat)
//                    outputFile.appendText("\n\n[Final Output - $timestamp]\n\n$it")
//                    // println("Final output saved to output.txt")
//                }
//            }
//        }
//
//        var timeElapsed = 0
////        while (!job.isCompleted) {
////            print("\rTime elapsed: ${"%.1f".format(timeElapsed / 1000.0)}s")
////            delay(100)
////            timeElapsed += 100
////        }
//    }
//    return finalOutput
//}