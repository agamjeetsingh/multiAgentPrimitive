typealias Agent = (String) -> String?

data class AgentConfig(val agent: Agent, val nextAgents: List<String>)
typealias AgentNetwork = Map<String, AgentConfig>

fun generateSolution(problem: String): String? {
    // Implementation
    println("Solving problem $problem")
    return "The solution to $problem is 4"
}

fun proofreadSolution(solution: String): String? {
    // Implementation
    println("Proofreading $solution")
    return "The solution is okay"
}

fun summarizeProgress(progress: String): String? {
    // Implementation
    return "Summary: $progress"
}

val agents: AgentNetwork = mapOf(
    "solver" to AgentConfig(::generateSolution, listOf("proofreader")),
    "proofreader" to AgentConfig(::proofreadSolution, listOf("summarizer")),
    "summarizer" to AgentConfig(::summarizeProgress, listOf("solver")),
    "finisher" to AgentConfig({input -> input}, emptyList()),
)