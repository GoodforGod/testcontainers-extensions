package io.goodforgod.testcontainers.extensions.jdbc;

import static io.goodforgod.testcontainers.extensions.jdbc.JdbcUrlParser.replaceHostPort;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.jdbc.JdbcUrlParser.HostAndPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Anton Kurako (GoodforGod)
 *
 * @since 26.11.2025
 */
class JdbcUrlParserTests {

    @Test
    void testStandardPostgresReplace() {
        String url = "jdbc:postgresql://oldhost:5432/mydb";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 5432), new HostAndPort("newhost", 5544));

        assertEquals("jdbc:postgresql://newhost:5544/mydb", out);
    }

    @Test
    void testStandardPostgresParse() {
        String url = "jdbc:postgresql://oldhost:5432/mydb";

        assertEquals(new HostAndPort("oldhost", 5432), JdbcUrlParser.parseJdbc(url));
    }

    @Test
    void testStandardMySQLReplace() {
        String url = "jdbc:mysql://192.168.0.1:3306/schema";
        String out = replaceHostPort(url, new HostAndPort("192.168.0.1", 3306), new HostAndPort("10.0.0.5", 3307));

        assertEquals("jdbc:mysql://10.0.0.5:3307/schema", out);
    }

    @Test
    void testStandardIpV6() {
        String url = "jdbc:postgresql://[2001:db8::1]:5432/db";
        String out = replaceHostPort(url, new HostAndPort("2001:db8::1", 5432), new HostAndPort("2001:db8::2", 5433));

        assertEquals("jdbc:postgresql://[2001:db8::2]:5433/db", out);
    }

    @Test
    void testSqlServerReplace() {
        String url = "jdbc:sqlserver://oldhost:1433;databaseName=mydb;encrypt=false";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 1433), new HostAndPort("newhost", 11433));

        assertEquals("jdbc:sqlserver://newhost:11433;databaseName=mydb;encrypt=false", out);
    }

    @Test
    void testSqlServerReplaceWithSlashPath() {
        String url = "jdbc:sqlserver://oldhost:1433/mydb;encrypt=false";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 1433), new HostAndPort("newhost", 11433));

        assertEquals("jdbc:sqlserver://newhost:11433/mydb;encrypt=false", out);
    }

    @Test
    void testSqlServerReplaceWithIpV6() {
        String url = "jdbc:sqlserver://[2001:db8::1]:1433;databaseName=mydb";
        String out = replaceHostPort(url, new HostAndPort("2001:db8::1", 1433), new HostAndPort("2001:db8::2", 11433));

        assertEquals("jdbc:sqlserver://[2001:db8::2]:11433;databaseName=mydb", out);
    }

    @Test
    void testSqlServerParse() {
        String url = "jdbc:sqlserver://oldhost:1433;databaseName=mydb;encrypt=false";

        assertEquals(new HostAndPort("oldhost", 1433),
                JdbcUrlParser.parseJdbc("com.microsoft.sqlserver.jdbc.SQLServerDriver", url));
    }

    @Test
    void testSqlServerParseWithoutDriverClassName() {
        String url = "jdbc:sqlserver://oldhost:1433;databaseName=mydb;encrypt=false";

        assertEquals(new HostAndPort("oldhost", 1433), JdbcUrlParser.parseJdbc(url));
    }

    @Test
    void testSqlServerNoReplaceIfNotMatched() {
        String url = "jdbc:sqlserver://oldhost:1433;databaseName=mydb;encrypt=false";
        String out = replaceHostPort(url, new HostAndPort("otherhost", 1433), new HostAndPort("newhost", 11433));

        assertEquals(url, out);
    }

    @ParameterizedTest
    @CsvSource({
            "jdbc:postgresql://oldhost:5432/mydb?ssl=false,jdbc:postgresql://newhost:15432/mydb?ssl=false,oldhost,5432,newhost,15432",
            "jdbc:cockroachdb://oldhost:26257/defaultdb?sslmode=disable,jdbc:cockroachdb://newhost:26258/defaultdb?sslmode=disable,oldhost,26257,newhost,26258",
            "jdbc:mysql://oldhost:3306/schema?useSSL=false&serverTimezone=UTC,jdbc:mysql://newhost:3307/schema?useSSL=false&serverTimezone=UTC,oldhost,3306,newhost,3307",
            "jdbc:mariadb://oldhost:3306/schema?allowPublicKeyRetrieval=true,jdbc:mariadb://newhost:3307/schema?allowPublicKeyRetrieval=true,oldhost,3306,newhost,3307",
            "jdbc:clickhouse://oldhost:8123/default?compress=0,jdbc:clickhouse://newhost:18123/default?compress=0,oldhost,8123,newhost,18123",
            "jdbc:db2://oldhost:50000/sample:user=db2inst1;,jdbc:db2://newhost:50001/sample:user=db2inst1;,oldhost,50000,newhost,50001",
            "jdbc:firebirdsql://oldhost:3050/var/db/example.fdb,jdbc:firebirdsql://newhost:3051/var/db/example.fdb,oldhost,3050,newhost,3051"
    })
    void testCommonJdbcUrlsReplace(String url,
                                   String expected,
                                   String oldHost,
                                   int oldPort,
                                   String newHost,
                                   int newPort) {
        String out = replaceHostPort(url, new HostAndPort(oldHost, oldPort), new HostAndPort(newHost, newPort));

        assertEquals(expected, out);
    }

    @ParameterizedTest
    @CsvSource({
            "jdbc:postgresql://oldhost:5432/mydb?ssl=false,oldhost,5432",
            "jdbc:cockroachdb://oldhost:26257/defaultdb?sslmode=disable,oldhost,26257",
            "jdbc:mysql://oldhost:3306/schema?useSSL=false&serverTimezone=UTC,oldhost,3306",
            "jdbc:mariadb://oldhost:3306/schema?allowPublicKeyRetrieval=true,oldhost,3306",
            "jdbc:clickhouse://oldhost:8123/default?compress=0,oldhost,8123",
            "jdbc:db2://oldhost:50000/sample:user=db2inst1;,oldhost,50000",
            "jdbc:firebirdsql://oldhost:3050/var/db/example.fdb,oldhost,3050"
    })
    void testCommonJdbcUrlsParse(String url, String host, int port) {
        assertEquals(new HostAndPort(host, port), JdbcUrlParser.parseJdbc(url));
    }

    @Test
    void testStandardReplaceHostNameWithIpV6() {
        String url = "jdbc:postgresql://oldhost:5432/db";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 5432), new HostAndPort("2001:db8::2", 5433));

        assertEquals("jdbc:postgresql://[2001:db8::2]:5433/db", out);
    }

    @Test
    void testStandardReplaceIpV6WithHostName() {
        String url = "jdbc:postgresql://[2001:db8::1]:5432/db";
        String out = replaceHostPort(url, new HostAndPort("2001:db8::1", 5432), new HostAndPort("newhost", 5433));

        assertEquals("jdbc:postgresql://newhost:5433/db", out);
    }

    @Test
    void testStandardNoReplaceIfNotMatched() {
        String url = "jdbc:mysql://localhost:3306/schema";
        String out = replaceHostPort(url, new HostAndPort("otherhost", 3307), new HostAndPort("new", 9999));

        assertEquals(url, out);
    }

    @Test
    void testOracleServiceNameReplace() {
        String url = "jdbc:oracle:thin:@//oldhost:1521/service";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 1521), new HostAndPort("newhost", 1541));

        assertEquals("jdbc:oracle:thin:@//newhost:1541/service", out);
    }

    @Test
    void testOracleServiceNameNoReplace() {
        String url = "jdbc:oracle:thin:@//oldhost:1521/service";
        String out = replaceHostPort(url, new HostAndPort("nomatch", 1111), new HostAndPort("new", 2222));

        assertEquals(url, out);
    }

    @Test
    void testOracleSidReplace() {
        String url = "jdbc:oracle:thin:@oldhost:1521:ORCL";
        String out = replaceHostPort(url, new HostAndPort("oldhost", 1521), new HostAndPort("newhost", 1540));

        assertEquals("jdbc:oracle:thin:@newhost:1540:ORCL", out);
    }

    @Test
    void testOracleSidNoReplace() {
        String url = "jdbc:oracle:thin:@oldhost:1521:ORCL";
        String out = replaceHostPort(url, new HostAndPort("otherhost", 1521), new HostAndPort("new", 1540));

        assertEquals(url, out);
    }

    @Test
    void testOracleDescriptionReplace() {
        String url = "jdbc:oracle:thin:@" +
                "(DESCRIPTION=" +
                "(ADDRESS=(HOST=oldhost)(PORT=1521))" +
                "(CONNECT_DATA=(SERVICE_NAME=service))" +
                ")";

        String out = replaceHostPort(url, new HostAndPort("oldhost", 1521), new HostAndPort("newhost", 1545));

        assertEquals(
                "jdbc:oracle:thin:@" +
                        "(DESCRIPTION=" +
                        "(ADDRESS=(HOST=newhost)(PORT=1545))" +
                        "(CONNECT_DATA=(SERVICE_NAME=service))" +
                        ")",
                out);
    }

    @Test
    void testOracleDescriptionMultipleAddresses() {
        String url = "jdbc:oracle:thin:@" +
                "(DESCRIPTION=" +
                "(ADDRESS_LIST=" +
                "(ADDRESS=(HOST=oldhost)(PORT=1521))" +
                "(ADDRESS=(HOST=oldhost)(PORT=1521))" +
                ")" +
                "(CONNECT_DATA=(SERVICE_NAME=orcl))" +
                ")";

        String out = replaceHostPort(url, new HostAndPort("oldhost", 1521), new HostAndPort("newhost", 1541));

        assertEquals(
                "jdbc:oracle:thin:@" +
                        "(DESCRIPTION=" +
                        "(ADDRESS_LIST=" +
                        "(ADDRESS=(HOST=newhost)(PORT=1541))" +
                        "(ADDRESS=(HOST=newhost)(PORT=1541))" +
                        ")" +
                        "(CONNECT_DATA=(SERVICE_NAME=orcl))" +
                        ")",
                out);
    }

    @Test
    void testOracleDescriptionNoReplace() {
        String url = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(HOST=db)(PORT=1521)))";

        String out = replaceHostPort(url, new HostAndPort("other", 1111), new HostAndPort("new", 2222));

        assertEquals(url, out);
    }
}
