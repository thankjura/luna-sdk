package ru.slie.luna.sdk.command;

import picocli.CommandLine;

import java.util.ResourceBundle;
import java.util.Scanner;

@CommandLine.Command(name = "generate", description = "${command.generate.description}", mixinStandardHelpOptions = true)
public class GenerateCommand implements Runnable {
    ResourceBundle bundle = ResourceBundle.getBundle("messages");

    @CommandLine.Option(names = {"-g", "--groupId"}, descriptionKey = "command.generate.option.group_id")
    private String groupId;

    @CommandLine.Option(names = {"-a", "--artifactId"}, descriptionKey = "command.generate.option.artifact_id")
    private String artifactId;

    @CommandLine.Option(names = {"-v", "--version"}, descriptionKey = "command.generate.option.version")
    private String version = "1.0.0-SNAPSHOT";

    @CommandLine.Option(names = {"-d", "--dir"}, descriptionKey = "command.generate.option.project_dir")
    private String projectDir;

    private final Scanner scanner = new Scanner(System.in);

    @Override public void run() {
        if (groupId == null || artifactId == null) {
            enterInteractiveMode();
        }

        System.out.println(bundle.getString("command.generate.project_summary"));
        System.out.println("   groupId: " + groupId);
        System.out.println("   artifactId: " + artifactId);
        System.out.println("   version: " + version);
        System.out.println("   directory: " + (projectDir != null? projectDir: artifactId));
        System.out.println();
    }

    private void enterInteractiveMode() {
        groupId = ask(bundle.getString("command.generate.option.group_id"), groupId, null);
        artifactId = ask(bundle.getString("command.generate.option.artifact_id"), artifactId, "my-addon");
        version = ask(bundle.getString("command.generate.option.version"), version, "1.0.0-SNAPSHOT");
        String defaultDir = artifactId;
        projectDir = ask(bundle.getString("command.generate.option.project_dir"), projectDir, defaultDir);
    }

    private String ask(String question, String currentValue, String defaultValue) {
        String defaultStr = "";
        if (defaultValue != null && !defaultValue.isEmpty()) {
            defaultStr = " [" + defaultValue + "]";
        } else if (currentValue != null) {
            defaultStr = " [" + currentValue + "]";
        }

        System.out.print(question + defaultStr + ": ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            if (currentValue != null) return currentValue;
            if (defaultValue != null) return defaultValue;

            return ask(question, currentValue, defaultValue);
        }

        return input;
    }
}