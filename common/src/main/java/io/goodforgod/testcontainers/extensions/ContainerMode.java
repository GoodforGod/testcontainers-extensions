package io.goodforgod.testcontainers.extensions;

/**
 * Container mode indicating when to run container
 */
public enum ContainerMode {

    /**
     * Specified container with same image will run ONCE per test execution and shared across all test
     * classes
     * <p>
     * Container image and Container network and Container network alias must be same across annotations
     * or container will be created such pair
     */
    PER_RUN,
    /**
     * Specified container will run once per test class and shared across all test methods
     */
    PER_CLASS,
    /**
     * Specified container will run once per test method
     */
    PER_METHOD
}
