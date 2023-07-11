package io.goodforgod.testcontainers.extensions.jdbc;

/**
 * Container mode indicating when to run container
 */
public enum ContainerMode {
    /**
     * Specified container with same image will run ONCE per test execution and shared across all test classes (if image is same or will create container per image)
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
