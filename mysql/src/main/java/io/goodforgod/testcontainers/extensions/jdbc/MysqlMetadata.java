package io.goodforgod.testcontainers.extensions.jdbc;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
final class MysqlMetadata extends JdbcMetadata {

    public MysqlMetadata(boolean network, String alias, String image, ContainerMode runMode, Migration migration) {
        super(network, alias, image, runMode, migration);
    }

    @Override
    protected @NotNull String networkAliasDefault() {
        return "mysql-" + System.currentTimeMillis();
    }
}
