package la.meleva.fundraiser.support;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;

class SupportMigrationIntegrationTest {
    @Test
    void upgradePreservesExistingUnassignedRequests() throws Exception {
        try (var mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"))) {
            mysql.start();
            Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .target("18").load().migrate();
            try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO users (public_id,display_name,surname) VALUES "
                        + "('ae95f28a-9c14-4c57-9f1d-fdc9a6ee458a','Pessoa','Legada')");
                sql.executeUpdate("INSERT INTO support_requests (public_id,user_id,subject,message) VALUES "
                        + "('e320168d-c52e-4c2d-90d8-e0bcdd8008b5',1,'Dúvida antiga','Mensagem antiga')");
            }
            Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).load().migrate();
            try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 var sql = connection.createStatement();
                 var rows = sql.executeQuery("SELECT user_id,subject,message,status,organization_id,idempotency_key,idempotency_fingerprint FROM support_requests "
                         + "WHERE public_id='e320168d-c52e-4c2d-90d8-e0bcdd8008b5'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("user_id")).isEqualTo(1);
                assertThat(rows.getString("subject")).isEqualTo("Dúvida antiga");
                assertThat(rows.getString("message")).isEqualTo("Mensagem antiga");
                assertThat(rows.getString("status")).isEqualTo("OPEN");
                assertThat(rows.getObject("organization_id")).isNull();
                assertThat(rows.getObject("idempotency_key")).isNull();
                assertThat(rows.getObject("idempotency_fingerprint")).isNull();
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
