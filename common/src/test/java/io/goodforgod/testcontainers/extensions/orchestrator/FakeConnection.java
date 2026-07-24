package io.goodforgod.testcontainers.extensions.orchestrator;

import java.util.List;

public record FakeConnection(String service, String image, List<String> aliases, boolean sharedNetwork, int networkIdentity) {}
