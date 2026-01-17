package ru.slie.luna.sdk.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.slie.luna.sdk.project.ProjectBuilder;

import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.Scanner;

@CommandLine.Command(name = "generate", description = "${command.generate.description}", mixinStandardHelpOptions = true)
public class ProjectGenerateCommand implements Runnable {
    private final Logger log = LoggerFactory.getLogger("luna-sdk");
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

    @Override
    public void run() {
        if (groupId == null || artifactId == null) {
            enterInteractiveMode();
        }

        String currentDirPath = Paths.get("").toAbsolutePath().toString();

        if (projectDir == null) {
            projectDir = currentDirPath;
        }

        log.info(bundle.getString("command.generate.project_summary"));
        log.info("   groupId: {}", groupId);
        log.info("   artifactId: {}", artifactId);
        log.info("   version: {}", version);
        log.info("   directory: {}", projectDir);

        ProjectBuilder builder = new ProjectBuilder(projectDir);
        builder.groupId(groupId);
        builder.artifactId(artifactId);
        builder.version(version);

        builder.build();
    }

    private void enterInteractiveMode() {
        groupId = ask(bundle.getString("command.generate.option.group_id"), groupId, null);
        artifactId = ask(bundle.getString("command.generate.option.artifact_id"), artifactId, "my-addon");
        version = ask(bundle.getString("command.generate.option.version"), version, "1.0.0-SNAPSHOT");
        String defaultDir = Paths.get("").toAbsolutePath().toString();
        projectDir = ask(bundle.getString("command.generate.option.project_dir"), projectDir, defaultDir);
    }

    private String ask(String question, String currentValue, String defaultValue) {
        String defaultStr = "";
        if (defaultValue != null && !defaultValue.isEmpty()) {
            defaultStr = " [" + defaultValue + "]";
        } else if (currentValue != null) {
            defaultStr = " [" + currentValue + "]";
        }

        log.info("{}{}: ", question, defaultStr);
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            if (currentValue != null) return currentValue;
            if (defaultValue != null) return defaultValue;

            return ask(question, null, null);
        }

        return input;
    }
}