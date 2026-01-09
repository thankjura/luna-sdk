package ru.slie.luna.sdk;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import ru.slie.luna.sdk.command.GenerateCommand;
import ru.slie.luna.sdk.command.GenerateCompletionCommand;
import ru.slie.luna.sdk.command.RunCommand;


@Command(name = "luna-sdk", mixinStandardHelpOptions = true, subcommands = {GenerateCompletionCommand.class, GenerateCommand.class, RunCommand.class}, resourceBundle = "messages")
public class LunaSdkMain implements Runnable {
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new LunaSdkMain());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    @Override public void run() {
        CommandLine.usage(this, System.out);
    }
}