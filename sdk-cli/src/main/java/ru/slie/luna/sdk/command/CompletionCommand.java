package ru.slie.luna.sdk.command;

import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(
        name = "completion",
        mixinStandardHelpOptions = true,
        description = "${command.completion.description}",
        helpCommand = true
)
public class CompletionCommand extends AutoComplete.GenerateCompletion {}
