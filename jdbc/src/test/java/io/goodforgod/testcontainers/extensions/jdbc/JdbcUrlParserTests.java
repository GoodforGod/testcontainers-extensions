package io.goodforgod.testcontainers.extensions.jdbc;

import static io.goodforgod.testcontainers.extensions.jdbc.JdbcUrlParser.replaceHostPort;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.goodforgod.testcontainers.extensions.jdbc.JdbcUrlParser.HostAndPort;
import org.junit.jupiter.api.Test;

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
