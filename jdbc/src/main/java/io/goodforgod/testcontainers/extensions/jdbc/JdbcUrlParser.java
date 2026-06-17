package io.goodforgod.testcontainers.extensions.jdbc;

import java.net.URI;

final class JdbcUrlParser {

    private JdbcUrlParser() {}

    record HostAndPort(String host, int port) {}

    public static HostAndPort parseJdbc(String jdbcUrl) {
        try {
            if (jdbcUrl.startsWith("jdbc:oracle:")) {
                return parseOracle(jdbcUrl);
            } else {
                return parseJdbcStandard(jdbcUrl);
            }
        } catch (IllegalArgumentException e) {
            return parseFallback(jdbcUrl);
        }
    }

    public static HostAndPort parseJdbc(String driverClassName, String jdbcUrl) {
        try {
            if (driverClassName.startsWith("oracle.")) {
                return parseOracle(jdbcUrl);
            } else {
                return parseJdbcStandard(jdbcUrl);
            }
        } catch (IllegalArgumentException e) {
            return parseFallback(jdbcUrl);
        }
    }

    private static HostAndPort parseJdbcStandard(String jdbcUrl) {
        try {
            URI uri = URI.create(jdbcUrl.replace("jdbc:", ""));
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || port == -1) {
                throw new IllegalArgumentException("Cannot parse JDBC URL host/port: " + jdbcUrl);
            }

            return new HostAndPort(host, port);
        } catch (Exception e) {
            return parseFallback(jdbcUrl);
        }
    }

    private static HostAndPort parseFallback(String jdbcUrl) {
        int from = jdbcUrl.indexOf("//");
        if (from < 0) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }

        from += 2;
        return parseHostPort(jdbcUrl, from, findHostPortEnd(jdbcUrl, from));
    }

    private static HostAndPort parseOracle(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:oracle:")) {
            throw new IllegalArgumentException("Not an Oracle JDBC URL: " + jdbcUrl);
        }

        String url = jdbcUrl.substring("jdbc:oracle:".length());

        if (url.startsWith("thin:")) {
            url = url.substring("thin:".length());
        }

        if (url.startsWith("@")) {
            url = url.substring(1);
        }

        if (url.startsWith("(")) {
            return parseOracleDescriptionUrl(url);
        }

        if (url.startsWith("//")) {
            return parseOracleServiceNameUrl(url);
        }

        return parseOracleSidUrl(url);
    }

    private static HostAndPort parseOracleSidUrl(String url) {
        String[] parts = url.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid SID Oracle URL: " + url);
        }

        return new HostAndPort(parts[0], Integer.parseInt(parts[1]));
    }

    private static HostAndPort parseOracleServiceNameUrl(String url) {
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
        String upper = url.toUpperCase();

        int hostIdx = upper.indexOf("HOST=");
        int portIdx = upper.indexOf("PORT=");

        if (hostIdx < 0 || portIdx < 0) {
            throw new IllegalArgumentException("Invalid DESCRIPTION Oracle URL: " + url);
        }

        int hostStart = hostIdx + "HOST=".length();
        int hostEnd = upper.indexOf(")", hostStart);
        String host = url.substring(hostStart, hostEnd);

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

        return replaceStandard(jdbcUrl, oldHp, newHp);
    }

    private static String replaceStandard(String jdbcUrl, HostAndPort oldHp, HostAndPort newHp) {
        int from = jdbcUrl.indexOf("//");
        if (from < 0)
            return jdbcUrl;

        from += 2;
        int to = findHostPortEnd(jdbcUrl, from);
        HostAndPort found;
        try {
            found = parseHostPort(jdbcUrl, from, to);
        } catch (IllegalArgumentException e) {
            return jdbcUrl;
        }

        if (!found.equals(oldHp))
            return jdbcUrl;

        String replacement = newHp.host().contains(":")
                ? "[" + newHp.host() + "]:" + newHp.port()
                : newHp.host() + ":" + newHp.port();

        return jdbcUrl.substring(0, from) + replacement + jdbcUrl.substring(to);
    }

    private static int findHostPortEnd(String jdbcUrl, int from) {
        int end = jdbcUrl.length();
        for (char delimiter : new char[] { '/', '?', ';' }) {
            int idx = jdbcUrl.indexOf(delimiter, from);
            if (idx >= 0 && idx < end) {
                end = idx;
            }
        }

        return end;
    }

    private static HostAndPort parseHostPort(String jdbcUrl, int from, int to) {
        String hostPortPart = jdbcUrl.substring(from, to);

        if (hostPortPart.startsWith("[")) {
            int closing = hostPortPart.indexOf("]");
            if (closing < 0 || closing + 2 > hostPortPart.length() || hostPortPart.charAt(closing + 1) != ':') {
                throw new IllegalArgumentException("Invalid IPv6 host/port in URL: " + jdbcUrl);
            }

            String host = hostPortPart.substring(1, closing);
            int port = Integer.parseInt(hostPortPart.substring(closing + 2));
            return new HostAndPort(host, port);
        }

        String[] hp = hostPortPart.split(":", 2);
        if (hp.length != 2) {
            throw new IllegalArgumentException("Port is missing in URL: " + jdbcUrl);
        }

        return new HostAndPort(hp[0], Integer.parseInt(hp[1]));
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

        String after = jdbcUrl.substring(idx + 1);

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
