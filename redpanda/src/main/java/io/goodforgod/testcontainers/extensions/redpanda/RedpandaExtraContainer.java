package io.goodforgod.testcontainers.extensions.redpanda;

import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

final class RedpandaExtraContainer extends RedpandaContainer {

    public RedpandaExtraContainer(String image) {
        super(image);
    }

    public RedpandaExtraContainer(DockerImageName imageName) {
        super(imageName);
    }

    @Override
    public String getHost() {
        String host = super.getHost();
        // windows WSL docker host support for local development on Windows machine
        if ("[::1]".equals(host)) {
            return "127.0.0.1";
        } else {
            return host;
        }
    }
}
