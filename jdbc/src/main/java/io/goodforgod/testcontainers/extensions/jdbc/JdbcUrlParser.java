package io.goodforgod.testcontainers.extensions.jdbc;

import java.net.URI;

final class JdbcUrlParser {

    private JdbcUrlParser() {}

    record HostAndPort(String host, int port) {}

    public static HostAndPort parseJdbc(String driverClassName, String jdbcUrl) {
        try {
            if (driverClassName.startsWith("oracle.")) {
                return parseOracle(jdbcUrl);
            } else {
                return parseJdbc(jdbcUrl);
            }
        } catch (IllegalArgumentException e) {
            return parseFallback(jdbcUrl);
        }
    }

    private static HostAndPort parseFallback(String jdbcUrl) {
        int from = jdbcUrl.indexOf("//");
        if (from < 0) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }
        from += 2; // пропустить "//"

        int to = jdbcUrl.indexOf("/", from);
        if (to < 0) {
            to = jdbcUrl.length();
        }

        String hostPortPart = jdbcUrl.substring(from, to);

        String host;
        int port;

        // IPv6: [2001:db8::1]:5432
        if (hostPortPart.startsWith("[")) {
            int closing = hostPortPart.indexOf("]");
            host = hostPortPart.substring(1, closing);
            port = Integer.parseInt(hostPortPart.substring(closing + 2)); // skip ]:
        } else {
            // IPv4 / hostname: host:port
            String[] hp = hostPortPart.split(":", 2); // важно: limit = 2
            host = hp[0];

            if (hp.length == 1) {
                throw new IllegalArgumentException("Port is missing in URL: " + jdbcUrl);
            }

            port = Integer.parseInt(hp[1]);
        }

        return new HostAndPort(host, port);
    }

    private static HostAndPort parseJdbc(String jdbcUrl) {
        try {
            URI uri = URI.create(jdbcUrl.replace("jdbc:", ""));
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || port == -1) {
                throw new IllegalArgumentException("Cannot parse JDBC URL host/port: " + jdbcUrl);
            }

            return new HostAndPort(host, port);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl, e);
        }
    }

    private static HostAndPort parseOracle(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:oracle:")) {
            throw new IllegalArgumentException("Not an Oracle JDBC URL: " + jdbcUrl);
        }

        String url = jdbcUrl.substring("jdbc:oracle:".length());

        // Убираем "thin:" если есть
        if (url.startsWith("thin:")) {
            url = url.substring("thin:".length());
        }

        // Снимаем префикс '@'
        if (url.startsWith("@")) {
            url = url.substring(1);
        }

        // -----------------------------------------------------
        // 1. CASE: TNS DESCRIPTION URL
        // jdbc:oracle:thin:@(DESCRIPTION=...)
        // -----------------------------------------------------
        if (url.startsWith("(")) {
            return parseOracleDescriptionUrl(url);
        }

        // -----------------------------------------------------
        // 2. CASE: SERVICE_NAME URL
        // @//host:port/service
        // -----------------------------------------------------
        if (url.startsWith("//")) {
            return parseOracleServiceNameUrl(url);
        }

        // -----------------------------------------------------
        // 3. CASE: SID URL
        // @host:port:SID
        // -----------------------------------------------------
        return parseOracleSidUrl(url);
    }

    private static HostAndPort parseOracleSidUrl(String url) {
        // Формат: host:port:SID
        // jdbc:oracle:thin:@localhost:1521:ORCL
        String[] parts = url.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid SID Oracle URL: " + url);
        }
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return new HostAndPort(host, port);
    }

    private static HostAndPort parseOracleServiceNameUrl(String url) {
        // Формат: //host:port/service
        // Убираем //
        // jdbc:oracle:thin:@//db.example.com:1522/service
        String rest = url.substring(2);

        int colon = rest.indexOf(":");
        int slash = rest.indexOf("/", colon + 1);

        if (colon < 0 || slash < 0) {
            throw new IllegalArgumentException("Invalid service-name Oracle URL: " + url);
        }

        String host = rest.substring(0, colon);
        int port = Integer.parseInt(rest.substring(colon + 1, slash));

        return new HostAndPort(host, port);
    }

    private static HostAndPort parseOracleDescriptionUrl(String url) {
        // Ищем HOST=... и PORT=...
        // jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(HOST=myhost)(PORT=1523))(CONNECT_DATA=(SID=mysid)))
        String upper = url.toUpperCase();

        int hostIdx = upper.indexOf("HOST=");
        int portIdx = upper.indexOf("PORT=");

        if (hostIdx < 0 || portIdx < 0) {
            throw new IllegalArgumentException("Invalid DESCRIPTION Oracle URL: " + url);
        }

        // HOST=value
        int hostStart = hostIdx + "HOST=".length();
        int hostEnd = upper.indexOf(")", hostStart);
        String host = url.substring(hostStart, hostEnd);

        // PORT=value
        int portStart = portIdx + "PORT=".length();
        int portEnd = upper.indexOf(")", portStart);
        int port = Integer.parseInt(url.substring(portStart, portEnd));

        return new HostAndPort(host, port);
    }

    public static String replaceHostPort(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        if (jdbcUrl.contains("(DESCRIPTION=")) {
            return replaceInDescription(jdbcUrl, oldHp, newHp);
        }

        if (jdbcUrl.matches("jdbc:oracle:.*@//.*")) {
            return replaceInOracleServiceName(jdbcUrl, oldHp, newHp);
        }

        if (jdbcUrl.matches("jdbc:oracle:.*@[^/]+:\\d+:[^/]+")) {
            return replaceInOracleSid(jdbcUrl, oldHp, newHp);
        }

        // -------------------------
        // НЕ-ORACLE: PostgreSQL/MySQL
        // -------------------------
        return replaceStandard(jdbcUrl, oldHp, newHp);
    }

    private static String replaceStandard(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        int from = jdbcUrl.indexOf("//");
        if (from < 0)
            return jdbcUrl;
        from += 2;

        int to = jdbcUrl.indexOf("/", from);
        if (to < 0)
            to = jdbcUrl.length();

        String hostPort = jdbcUrl.substring(from, to);

        // IPv6
        if (hostPort.startsWith("[")) {
            int closing = hostPort.indexOf("]");
            String foundHost = hostPort.substring(1, closing);
            int foundPort = Integer.parseInt(hostPort.substring(closing + 2));

            if (foundHost.equals(oldHp.host()) && foundPort == oldHp.port()) {
                return jdbcUrl.substring(0, from)
                        + "[" + newHp.host() + "]:" + newHp.port()
                        + jdbcUrl.substring(to);
            }
            return jdbcUrl;
        }

        // IPv4 / hostname
        String[] hp = hostPort.split(":", 2);
        if (hp.length != 2)
            return jdbcUrl;

        String foundHost = hp[0];
        int foundPort = Integer.parseInt(hp[1]);

        if (!foundHost.equals(oldHp.host()) || foundPort != oldHp.port())
            return jdbcUrl;

        return jdbcUrl.substring(0, from)
                + newHp.host() + ":" + newHp.port()
                + jdbcUrl.substring(to);
    }

    private static String replaceInOracleServiceName(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        int idx = jdbcUrl.indexOf("@//");
        if (idx < 0)
            return jdbcUrl;
        int start = idx + 3;

        int slash = jdbcUrl.indexOf("/", start);
        if (slash < 0)
            return jdbcUrl;

        String hostPort = jdbcUrl.substring(start, slash);

        String[] hp = hostPort.split(":", 2);
        if (hp.length != 2)
            return jdbcUrl;

        String foundHost = hp[0];
        int foundPort = Integer.parseInt(hp[1]);

        if (!foundHost.equals(oldHp.host()) || foundPort != oldHp.port())
            return jdbcUrl;

        return jdbcUrl.substring(0, start)
                + newHp.host() + ":" + newHp.port()
                + jdbcUrl.substring(slash);
    }

    private static String replaceInOracleSid(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        int idx = jdbcUrl.indexOf("@");
        if (idx < 0)
            return jdbcUrl;

        String after = jdbcUrl.substring(idx + 1); // host:port:SID

        String[] parts = after.split(":", 3);
        if (parts.length < 3)
            return jdbcUrl;

        String foundHost = parts[0];
        int foundPort = Integer.parseInt(parts[1]);

        if (!foundHost.equals(oldHp.host()) || foundPort != oldHp.port())
            return jdbcUrl;

        return jdbcUrl.substring(0, idx + 1)
                + newHp.host() + ":" + newHp.port() + ":" + parts[2];
    }

    private static String replaceInDescription(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        String result = jdbcUrl;

        result = result.replace(
                "(HOST=" + oldHp.host() + ")",
                "(HOST=" + newHp.host() + ")");

        result = result.replace(
                "(PORT=" + oldHp.port() + ")",
                "(PORT=" + newHp.port() + ")");

        return result;
    }
}
