package ru.slie.luna.sdk;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import ru.slie.luna.sdk.command.CompletionCommand;
import ru.slie.luna.sdk.command.CreateCommand;
import ru.slie.luna.sdk.command.PackageCommand;
import ru.slie.luna.sdk.command.RunCommand;
import ru.slie.luna.sdk.utils.SdkProperties;


@Command(name = "luna-sdk",
        mixinStandardHelpOptions = true,
        subcommands = {
            CompletionCommand.class,
            CreateCommand.class,
            PackageCommand.class,
            RunCommand.class
        },
        resourceBundle = "messages")
public class LunaCliMain implements Runnable {
    public static void main(String[] args) {
        // Used in option descriptions
        System.setProperty("sdk.luna.version", SdkProperties.get("luna.version"));
        CommandLine cmd = new CommandLine(new LunaCliMain()).setStopAtPositional(false).setAllowOptionsAsOptionParameters(true);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof CommandLine.PicocliException) {
                // Expected failures: print the message only, the stack trace is noise for the user
                commandLine.getErr().println(commandLine.getColorScheme().errorText(ex.getMessage()));
                return commandLine.getCommandSpec().exitCodeOnExecutionException();
            }
            throw ex;
        });
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    @Override public void run() {
        CommandLine.usage(this, System.out);
    }
}