package la.meleva.fundraiser.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.ranking.RankingScope;
import la.meleva.fundraiser.ranking.RankingService;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * P18-06: ensaio de backup e restauração com o próprio scripts/backup-mysql.sh, executado dentro do contêiner
 * MySQL 8.4. Compara todas as tabelas por CHECKSUM TABLE e revalida os hashes lacrados no banco restaurado.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackupRestoreDrillIntegrationTest {
    private static final String SCRIPT = "/tmp/backup-mysql.sh";
    private static final String DUMP = "/tmp/drill.sql";
    private static final String DEFAULTS = "/tmp/drill.cnf";

    @Autowired MySQLContainer mysql;
    @Autowired ParticipationService participations;
    @Autowired RankingService rankings;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;

    @Test
    void scriptedBackupRestoresEveryTableIdenticallyAndKeepsSealedHashesVerifiable() throws Exception {
        seedRankingSnapshot();
        prepareContainer();
        String source = mysql.getDatabaseName();

        Map<String, Long> sourceChecksums;
        try (Connection root = root(); var lock = root.createStatement()) {
            lock.execute("FLUSH TABLES WITH READ LOCK");
            try {
                sourceChecksums = checksums(root, source);
                ExecResult backup = script("backup", source);
                assertThat(backup.getExitCode()).as(backup.getStderr()).isZero();
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
        assertThat(sourceChecksums).containsKeys("campaign_numbers", "participation_entries", "ranking_snapshots",
                "audit_events", "financial_ledger", "flyway_schema_history");

        ExecResult verify = script("verify", source);
        assertThat(verify.getExitCode()).as(verify.getStderr()).isZero();
        createDatabase("fundraiser_restore");
        ExecResult restore = script("restore", "fundraiser_restore");
        assertThat(restore.getExitCode()).as(restore.getStderr()).isZero();

        try (Connection root = root()) {
            assertThat(checksums(root, "fundraiser_restore")).isEqualTo(sourceChecksums);
            assertSnapshotHashesStillMatch(root, "fundraiser_restore");
        }
        System.out.println("[P18-06] tables=" + sourceChecksums.size() + " dump_bytes="
                + mysql.execInContainer("bash", "-c", "wc -c < " + DUMP).getStdout().trim() + " checksums=identical");
    }

    @Test
    void tamperedDumpIsRejectedBeforeTouchingTheTargetDatabase() throws Exception {
        prepareContainer();
        String source = mysql.getDatabaseName();
        ExecResult backup = script("backup", source);
        assertThat(backup.getExitCode()).as(backup.getStderr()).isZero();
        mysql.execInContainer("bash", "-c", "printf 'DROP TABLE IF EXISTS audit_events;\\n' >> " + DUMP);
        createDatabase("fundraiser_restore_tampered");

        ExecResult verify = script("verify", source);
        ExecResult restore = script("restore", "fundraiser_restore_tampered");

        assertThat(verify.getExitCode()).isNotZero();
        assertThat(restore.getExitCode()).isNotZero();
        assertThat(restore.getStderr()).contains("SHA-256 do dump não confere");
        try (Connection root = root(); var statement = root.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables "
                     + "WHERE table_schema = 'fundraiser_restore_tampered'")) {
            rows.next();
            assertThat(rows.getLong(1)).isZero();
        }
    }

    private void seedRankingSnapshot() {
        var organization = organizations.save(new Organization("Organização ensaio", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Ensaio de restauração", "p18-restore-drill-" + UUID.randomUUID(),
                CampaignMode.DEMO);
        campaign.setNumberRange(0, 19);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade ensaio"));
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            saved.transitionTo(next);
        }
        campaigns.save(saved);
        for (int i = 0; i < 3; i++) {
            User buyer = users.save(new User("Ensaio", "B" + i, "+551194400" + "%04d".formatted(i)));
            participations.request(buyer.getId(), saved.getPublicId(), UUID.randomUUID().toString(), 1,
                    beneficiary.getPublicId());
        }
        rankings.snapshot(null, saved.getPublicId(), new CreateSnapshotRequest(RankingScope.PARTICIPANT, null));
    }

    private void prepareContainer() throws Exception {
        mysql.copyFileToContainer(MountableFile.forHostPath(Path.of("..", "scripts", "backup-mysql.sh")), SCRIPT);
        String defaults = "[client]\nuser=root\npassword=" + mysql.getPassword() + "\n";
        mysql.copyFileToContainer(Transferable.of(defaults.getBytes(StandardCharsets.UTF_8), 0600), DEFAULTS);
        mysql.execInContainer("rm", "-f", DUMP, DUMP + ".sha256");
    }

    private ExecResult script(String action, String database) throws Exception {
        return mysql.execInContainer("env", "DB_HOST=127.0.0.1", "DB_PORT=3306", "DB_NAME=" + database,
                "DB_DEFAULTS_FILE=" + DEFAULTS, "bash", SCRIPT, action, DUMP);
    }

    private void createDatabase(String name) throws SQLException {
        try (Connection root = root(); var statement = root.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + name);
            statement.execute("CREATE DATABASE " + name + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private Connection root() throws SQLException {
        return DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
    }

    private static Map<String, Long> checksums(Connection connection, String schema) throws SQLException {
        var tables = new java.util.ArrayList<String>();
        try (var statement = connection.prepareStatement("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name")) {
            statement.setString(1, schema);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) tables.add(rows.getString(1));
            }
        }
        var result = new LinkedHashMap<String, Long>();
        for (String table : tables) {
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("CHECKSUM TABLE `" + schema + "`.`" + table + "`")) {
                rows.next();
                result.put(table, rows.getLong("Checksum"));
            }
        }
        return result;
    }

    private static void assertSnapshotHashesStillMatch(Connection connection, String schema) throws Exception {
        int verified = 0;
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT payload_json, snapshot_hash FROM `" + schema + "`.ranking_snapshots")) {
            while (rows.next()) {
                String payload = rows.getString(1);
                String recomputed = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(payload.getBytes(StandardCharsets.UTF_8)));
                assertThat(recomputed).isEqualTo(rows.getString(2));
                verified++;
            }
        }
        assertThat(verified).isPositive();
    }
}
