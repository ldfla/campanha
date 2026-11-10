package la.meleva.fundraiser.participation;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;

class BuyerSurnameMigrationIntegrationTest {
    @Test
    void upgradePreservesExistingSnapshotAndAllowsFullSurname() throws Exception {
        try (var mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"))) {
            mysql.start();
            Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .target("30").load().migrate();
            try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO organizations (public_id,legal_name,type) VALUES ('10000000-0000-0000-0000-000000000001','Teste','ASSOCIATION')");
                sql.executeUpdate("INSERT INTO users (public_id,display_name,surname) VALUES ('10000000-0000-0000-0000-000000000002','Nome','Original')");
                sql.executeUpdate("INSERT INTO campaigns (public_id,organization_id,slug,title) VALUES ('10000000-0000-0000-0000-000000000003',1,'migracao-sobrenome','Teste')");
                sql.executeUpdate("INSERT INTO campaign_numbers (campaign_id,number_value) VALUES (1,1)");
                sql.executeUpdate("INSERT INTO campaign_number_buyers (number_id,campaign_id,buyer_user_id,buyer_phone,buyer_first_name,buyer_surname,buyer_pseudonym,created_at) "
                        + "VALUES (1,1,1,'+5511999999999','Nome','Original','N.O.',CURRENT_TIMESTAMP)");
            }
            Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).load().migrate();
            try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 var sql = connection.createStatement()) {
                try (var rows = sql.executeQuery("SELECT buyer_surname,buyer_pseudonym FROM campaign_number_buyers WHERE number_id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("buyer_surname")).isEqualTo("Original");
                    assertThat(rows.getString("buyer_pseudonym")).isEqualTo("N.O.");
                }
                try (var update = connection.prepareStatement("UPDATE campaign_number_buyers SET buyer_surname=? WHERE number_id=1")) {
                    update.setString(1, "S".repeat(150));
                    assertThat(update.executeUpdate()).isEqualTo(1);
                }
                try (var rows = sql.executeQuery("SELECT CHAR_LENGTH(buyer_surname) FROM campaign_number_buyers WHERE number_id=1")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(150);
                }
            }
        }
    }
}
