package io.goodforgod.testcontainers.extensions.jdbc;

import java.net.URI;
import org.jetbrains.annotations.NotNull;

/**
 * Anton Kurako (GoodforGod)
 *
 * @since 22.05.2024
 */
class ClickhouseConnectionImpl extends JdbcConnectionImpl {

    ClickhouseConnectionImpl(Params params, Params network) {
        super(params, network);
    }

    static ClickhouseConnectionImpl forProtocol(String driverProtocol,
                                                String host,
                                                int port,
                                                String database,
                                                String username,
                                                String password) {
        var jdbcUrl = String.format("jdbc:%s://%s:%d/%s", driverProtocol, host, port, database);
        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        return new ClickhouseConnectionImpl(params, null);
    }

    static ClickhouseConnectionImpl forJDBC(String jdbcUrl,
                                            String host,
                                            int port,
                                            String hostInNetwork,
                                            Integer portInNetwork,
                                            String database,
                                            String username,
                                            String password) {
        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        final Params network;
        if (hostInNetwork == null) {
            network = null;
        } else {
            var jdbcUrlInNetwork = jdbcUrl.replace(host + ":" + port, hostInNetwork + ":" + portInNetwork);
            network = new ParamsImpl(jdbcUrlInNetwork, hostInNetwork, portInNetwork, database, username, password);
        }

        return new ClickhouseConnectionImpl(params, network);
    }

    static ClickhouseConnectionImpl forExternal(String jdbcUrl, String username, String password) {
        final URI uri = URI.create(jdbcUrl);
        var host = uri.getHost();
        var port = uri.getPort();

        final int dbSeparator = uri.getPath().indexOf(';');
        final String database = (dbSeparator == -1)
                ? uri.getPath()
                : uri.getPath().substring(0, dbSeparator);

        var params = new ParamsImpl(jdbcUrl, host, port, database, username, password);
        return new ClickhouseConnectionImpl(params, null);
    }

    private volatile ClickhouseLiquibaseJdbcMigrationEngine liquibaseJdbcMigrationEngine;

    @Override
    public @NotNull JdbcMigrationEngine migrationEngine(Migration.@NotNull Engines engine) {
        if (engine == Migration.Engines.LIQUIBASE) {
            if (liquibaseJdbcMigrationEngine == null) {
                this.liquibaseJdbcMigrationEngine = new ClickhouseLiquibaseJdbcMigrationEngine(this);
            }
            return this.liquibaseJdbcMigrationEngine;
        } else {
            return super.migrationEngine(engine);
        }
    }
}
