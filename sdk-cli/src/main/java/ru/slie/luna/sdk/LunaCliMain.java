package ru.slie.luna.sdk;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import ru.slie.luna.sdk.command.CompletionCommand;
import ru.slie.luna.sdk.command.ProjectGenerateCommand;
import ru.slie.luna.sdk.command.ProjectPackageCommand;
import ru.slie.luna.sdk.command.RunCommand;


@Command(name = "luna-sdk",
        mixinStandardHelpOptions = true,
        subcommands = {
            CompletionCommand.class,
            ProjectGenerateCommand.class,
            ProjectPackageCommand.class,
            RunCommand.class
        },
        resourceBundle = "messages")
public class LunaCliMain implements Runnable {
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new LunaCliMain()).setStopAtPositional(false).setAllowOptionsAsOptionParameters(true);
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    @Override public void run() {
        CommandLine.usage(this, System.out);
    }
}