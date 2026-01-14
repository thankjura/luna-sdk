package ru.slie.luna.sdk.command;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.ProjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

@CommandLine.Command(name = "package", description = "${command.package.description}", mixinStandardHelpOptions = true)
public class ProjectPackageCommand implements Runnable {
    private final Logger log = LoggerFactory.getLogger("luna-sdk");
    ResourceBundle bundle = ResourceBundle.getBundle("messages");

    @Override
    public void run() {
        Path currentDirPath = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(currentDirPath);
        Path lunaHome = currentDirPath.resolve("luna-home");
        try {
            Files.createDirectories(lunaHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path pomPath = currentDirPath.resolve("pom.xml");
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomPath.toFile());
        String mavenHome = System.getProperty("mvn.directory");
        if (mavenHome != null) {
            request.setMavenHome(new File(mavenHome));
        }

        request.addArg("package");
        request.setBatchMode(true);
        request.setUpdateSnapshots(true);
        request.setOutputHandler(line -> System.out.println("[Maven] " + line));
        request.setErrorHandler(line -> System.err.println("[Maven ERROR] " + line));

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getExecutionException());
            }
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }

        log.info(bundle.getString("command.package.build.successful"));
    }
}