package io.goodforgod.testcontainers.extensions;

import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal
public abstract class AbstractContainerMetadata implements ContainerMetadata {

    private final boolean network;
    private final String alias;
    private final String image;
    private final ContainerMode runMode;

    protected AbstractContainerMetadata(boolean network, String alias, String image, ContainerMode runMode) {
        this.network = network;
        this.runMode = runMode;
        this.alias = Optional.ofNullable(getEnvValue("Alias", alias)).orElse(networkAliasDefault());
        this.image = Optional.ofNullable(getEnvValue("Image", image)).orElseThrow(
                () -> new IllegalArgumentException(getClass() + " expected image from '" + image + "' but received null"));
    }

    private static boolean isEnvironmentValue(String value) {
        return value != null && value.startsWith("${") && value.endsWith("}");
    }

    private static String getEnvValue(String name, String envOrValue) {
        if (isEnvironmentValue(envOrValue)) {
            final String envProperty = envOrValue.substring(2, envOrValue.length() - 1);
            final String[] environmentAndDefault = envProperty.split("\\|");

            if (environmentAndDefault.length > 2) {
                throw new IllegalArgumentException(
                        name + " property can contain only 1 ':' symbol but got: " + envProperty);
            } else if (environmentAndDefault.length == 2) {
                final String envValue = System.getenv(environmentAndDefault[0]);
                if (envValue == null) {
                    return (environmentAndDefault[1].isBlank())
                            ? null
                            : environmentAndDefault[1];
                }

                return envValue;
            }

            return System.getenv(environmentAndDefault[0]);
        } else {
            return envOrValue;
        }
    }

    @NotNull
    protected abstract String networkAliasDefault();

    @Override
    public boolean networkShared() {
        return network;
    }

    @Override
    public @Nullable String networkAlias() {
        return alias;
    }

    @Override
    public @NotNull String image() {
        return image;
    }

    @Override
    public @NotNull ContainerMode runMode() {
        return runMode;
    }
}
