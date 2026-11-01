package la.meleva.fundraiser.compliance;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida o adaptador S3 (compatível com qualquer object storage S3) contra um **SeaweedFS real**,
 * sem contexto Spring: cobre store + load do documento privado ponta a ponta.
 */
@Testcontainers
class S3PrivateDocumentStorageTest {

    @Container
    static final GenericContainer<?> SEAWEED =
            new GenericContainer<>(DockerImageName.parse("chrislusf/seaweedfs:latest"))
                    .withCommand("server", "-s3")
                    .withExposedPorts(8333)
                    .waitingFor(Wait.forHttp("/").forPort(8333).withStartupTimeout(Duration.ofMinutes(2)));

    private PrivateDocumentStorage storage() {
        return new S3PrivateDocumentStorage(
                "http://" + SEAWEED.getHost() + ":" + SEAWEED.getMappedPort(8333),
                "any", "any", "us-east-1", "fundraiser-private");
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
