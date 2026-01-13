package ru.slie.luna.sdk.command;

import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(
        name = "generate-completion",
        mixinStandardHelpOptions = true,
        description = "${command.generate-completion.description}",
        helpCommand = true
)
public class GenerateCompletionCommand extends AutoComplete.GenerateCompletion {
}
