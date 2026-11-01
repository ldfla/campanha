package la.meleva.fundraiser.compliance;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida o adaptador S3 (compatível com qualquer object storage S3) contra um **Garage real**
 * (engine local escolhida), sem contexto Spring: cobre store + load do documento privado.
 */
@Testcontainers
class S3PrivateDocumentStorageTest {

    private static final String ACCESS_KEY = "GK000000000000000000000000";
    private static final String SECRET_KEY = "0".repeat(64);

    @Container
    static final GenericContainer<?> GARAGE =
            new GenericContainer<>(DockerImageName.parse("dxflrs/garage:v2.5.0"))
                    .withCopyFileToContainer(MountableFile.forClasspathResource("garage.toml"), "/etc/garage.toml")
                    .withExposedPorts(3900)
                    .waitingFor(Wait.forLogMessage(".*S3 API server listening.*", 1)
                            .withStartupTimeout(Duration.ofMinutes(2)));

    @BeforeAll
    static void initCluster() throws Exception {
        String node = null;
        for (int attempt = 0; attempt < 30 && node == null; attempt++) {
            node = nodeId(GARAGE.execInContainer("/garage", "status").getStdout());
            if (node == null) Thread.sleep(1000);
        }
        assertThat(node).as("Garage node id").isNotNull();
        exec("layout", "assign", "-z", "dc1", "-c", "2G", node);
        exec("layout", "apply", "--version", "1");
        exec("key", "import", ACCESS_KEY, SECRET_KEY, "--yes", "-n", "fundraiser");
        exec("bucket", "create", "fundraiser-private");
        exec("bucket", "allow", "--read", "--write", "--owner", "fundraiser-private", "--key", "fundraiser");
    }

    private static void exec(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "/garage";
        System.arraycopy(args, 0, command, 1, args.length);
        GARAGE.execInContainer(command);
    }

    private static String nodeId(String status) {
        for (String line : status.split("\\R")) {
            if (line.contains("3901")) {
                String candidate = line.strip().split("\\s+")[0];
                if (candidate.matches("[0-9a-f]{16}")) return candidate;
            }
        }
        return null;
    }

    private PrivateDocumentStorage storage() {
        return new S3PrivateDocumentStorage(
                "http://" + GARAGE.getHost() + ":" + GARAGE.getMappedPort(3900),
                ACCESS_KEY, SECRET_KEY, "garage", "fundraiser-private");
    }

    @Test
    void storesAndLoadsDocumentThroughS3CompatibleStorage() {
        byte[] content = "documento-confidencial-de-teste".getBytes(StandardCharsets.UTF_8);

        String key = storage().store("documento.txt", "text/plain", content);

        assertThat(key).startsWith("private/");
        assertThat(storage().load(key)).isEqualTo(content);
    }

    @Test
    void loadsUnknownKeyWithFailure() {
        assertThatThrownBy(() -> storage().load("private/inexistente"))
                .isInstanceOf(IllegalStateException.class);
    }
}
