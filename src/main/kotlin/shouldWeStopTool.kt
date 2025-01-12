fun createShouldWeStopTool() : Tool {

    val stopNowProperty = Property(
        type = "boolean"
    )

    val parameters = Parameters(
        type = "object",
        properties = mapOf(
            "stopNow" to stopNowProperty,
        )
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