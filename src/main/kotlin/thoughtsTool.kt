fun createThoughtsTool() : Tool {
    val items = Items(type = "string")

    val chainOfThoughtsProperty = Property(type = "array", items = items)

    val parameters = Parameters(
        type = "object",
        properties = mapOf("chainOfThoughts" to chainOfThoughtsProperty)
    )

    val functionDeclaration = FunctionDeclaration(
        name = "thoughts",
        description = "give your chain of thoughts here",
        parameters = parameters
    )

    return Tool(
        listOf(
            functionDeclaration
        )
    )
}