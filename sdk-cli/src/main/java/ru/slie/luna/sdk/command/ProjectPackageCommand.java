package ru.slie.luna.sdk.command;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.I18nResolver;
import ru.slie.luna.sdk.utils.ProjectUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(name = "package", description = "${command.package.description}", mixinStandardHelpOptions = true)
public class ProjectPackageCommand implements Runnable {
    private final Logger log = LoggerFactory.getLogger("luna-sdk");
    private final Logger mavenLog = LoggerFactory.getLogger("maven");
    I18nResolver i18n = new I18nResolver();

    @Override
    public void run() {
        log.info(i18n.getString("command.package.build"));
        Path currentDirPath = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(currentDirPath);
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
        request.setOutputHandler(line -> mavenLog.info("[maven] - {}", line));
        request.setErrorHandler(line -> mavenLog.error("[maven] - {}", line));

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new RuntimeException(result.getExecutionException());
            }
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }

        log.info(i18n.getString("command.package.build.successful"));
    }
}