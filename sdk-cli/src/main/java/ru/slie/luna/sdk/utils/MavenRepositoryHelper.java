package ru.slie.luna.sdk.utils;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenRepositoryHelper {
    public static Path getLocalRepositoryPath() {
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsFile(Paths.get(System.getProperty("user.home"), ".m2", "settings.xml").toFile());
        request.setGlobalSettingsFile(Paths.get(System.getenv("MAVEN_HOME") != null ? System.getenv("MAVEN_HOME") : "", "conf", "settings.xml").toFile());

        try {
            SettingsBuildingResult result = new DefaultSettingsBuilderFactory().newInstance().build(request);
            String localRepo = result.getEffectiveSettings().getLocalRepository();
            if (localRepo != null && !localRepo.isEmpty()) {
                return Path.of(localRepo);
            }
        } catch (Exception ignored) {

        }
        return Paths.get(System.getProperty("user.home"), ".m2", "repository");
    }

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";

    /**
     * Resolves an artifact from the local repository first, then from Maven Central.
     * Locally installed SNAPSHOT versions (mvn install) are resolved from the local repository.
     */
    public static ArtifactResult getArtifact(Artifact artifact, boolean offline) throws ArtifactResolutionException {
        RepositorySystem system = new RepositorySystemSupplier().get();
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setOffline(offline);

        Path localRepoPath = getLocalRepositoryPath();
        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        RemoteRepository central = new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL_URL).build();
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.addRepository(central);
        return system.resolveArtifact(session, request);
    }
}
