package ru.slie.luna.sdk.project;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ProjectBuilder {
    private String groupId;
    private String artifactId;
    private String version;
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");
    private final String projectDir;
    private final Logger log = LoggerFactory.getLogger("luna-sdk");

    public ProjectBuilder(String projectDir) {
        this.projectDir = projectDir;
    }

    public ProjectBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public ProjectBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public ProjectBuilder version(String version) {
        this.version = version;
        return this;
    }

    public void build() {
//
//
//        Model model = new Model();
//        model.setModelVersion("4.0.0");
//        model.setGroupId(groupId);
//        model.setArtifactId(artifactId);
//        model.setVersion(version);
//        model.setPackaging("jar");
//        model.setName(artifactId);
//        model.setDescription("Luna addon: " + groupId + ":" + artifactId);
//
//        Organization organization = new Organization();
//        organization.setName("Example company");
//        organization.setUrl("https://example.com");
//        model.setOrganization(organization);
//
//        Dependency lunaApiDep = new Dependency();
//        lunaApiDep.setGroupId("ru.slie.luna");
//        lunaApiDep.setArtifactId("luna-api");
//        lunaApiDep.setVersion("${luna.api.version}");
//        lunaApiDep.setScope("provided");
//        model.addDependency(lunaApiDep);
//
//        Dependency springDep = new Dependency();
//        springDep.setGroupId("org.springframework");
//        springDep.setArtifactId("spring-webmvc");
//        springDep.setVersion("7.0.2");
//        springDep.setScope("provided");
//        model.addDependency(springDep);
//
//        Build build = new Build();
//        Plugin compiler = new Plugin();
//        compiler.setGroupId("org.apache.maven.plugins");
//        compiler.setArtifactId("maven-compiler-plugin");
//        compiler.setVersion("3.14.1");
//        PluginExecution execution = new PluginExecution();
//        execution.addGoal("compile");
//        compiler.addExecution(execution);
//        build.addPlugin(compiler);
//
//        Resource resource = new Resource();
//        resource.setDirectory("src/main/resources");
//        resource.setFiltering(true);
//        //resource.setIncludes(List.of("luna-plugin.yaml"));
//        build.addResource(resource);
//        model.setBuild(build);
//
//        model.addProperty("maven.compiler.source", "21");
//        model.addProperty("maven.compiler.target", "21");
//        model.addProperty("luna.api.version", "1.0.0");
//
        Path targetDir = Path.of(projectDir).resolve(artifactId).toAbsolutePath();
//        if (Files.exists(targetDir)) {
//            if (!Files.isDirectory(targetDir)) {
//                throw new RuntimeException(bundle.getString("command.generate.project.target_dir_exists"));
//            }
//            try (var stream = Files.list(targetDir)) {
//                if (stream.findAny().isPresent()) {
//                    throw new RuntimeException(bundle.getString("command.generate.project.target_dir_exists"));
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(bundle.getString("command.generate.project.target_dir_exists"));
//            }
//        }
//
//        try {
//            Files.createDirectories(targetDir);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        try (Writer writer = new FileWriter(targetDir.resolve("pom.xml").toFile())) {
//            new MavenXpp3Writer().write(writer, model);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        DemoGenerator demoFiles = new DemoGenerator(targetDir, new DefaultArtifact(groupId, artifactId, "luna-plugin", version));
        try {
            //demoFiles.copyDemoFiles(targetDir, new DefaultArtifact(groupId, artifactId, "luna-plugin", version));
            demoFiles.processFiles();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        log.info(bundle.getString("command.generate.project.success"));
    }
}
