package ru.slie.luna.sdk.command;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.CargoUtils;
import ru.slie.luna.sdk.utils.ProjectUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

@CommandLine.Command(name = "run", description = "Запустить Tomcat с приложением и плагином")
public class RunCommand implements Runnable {
    ResourceBundle bundle = ResourceBundle.getBundle("messages");
    private final Logger log = LoggerFactory.getLogger("luna-sdk");

    @Override public void run() {
        Path currentDirPath = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(currentDirPath);
        Path runnerPom = Path.of(System.getProperty("luna.sdk.home")).resolve("runner").resolve("pom.xml");
        if (!Files.isRegularFile(runnerPom)) {
            throw new RuntimeException();
        }

        String hostDataBasePath = currentDirPath.resolve("target").resolve("mongo").toAbsolutePath().toString();
        Path lunaHome = currentDirPath.resolve("target").resolve("luna-home").toAbsolutePath();
        try {
            Files.createDirectories(lunaHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        try (MongoDBContainer mongo = new MongoDBContainer("mongo:8")
//                                                         .withReuse(true)
//                                                         .withFileSystemBind(hostDataBasePath, "/data/db", BindMode.READ_WRITE)) {

            //mongo.start();
//            String uri = mongo.getReplicaSetUrl("luna");
//            String database = "luna";

//            try {
//                Files.writeString(lunaHome.resolve("database.yml"), String.format("url: %s\ndatabase: %s", uri, database));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            log.info("{}", bundle.getString("command.run.cargo.run"));

        try {
            InstalledLocalContainer container = CargoUtils.makeContainer(currentDirPath.resolve("target").toAbsolutePath());
            container.start();
        } catch (MalformedURLException e) {
            log.error("failed to start");
            throw new RuntimeException(e);
        }
        //}
    }
}
