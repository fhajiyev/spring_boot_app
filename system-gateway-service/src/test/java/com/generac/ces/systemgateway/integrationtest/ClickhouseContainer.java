package com.generac.ces.systemgateway.integrationtest;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ClickhouseContainer extends GenericContainer<ClickhouseContainer> {
    private ClickhouseContainer() {
        super(
                DockerImageName.parse(
                                "381278841082.dkr.ecr.us-east-1.amazonaws.com/dataplatform-clickhouse")
                        .asCompatibleSubstituteFor("clickhouse")
                        .withTag("main-latest")); // ("upgrade-ch-image-2bc98df"));
    }

    public static ClickhouseContainer getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void start() {
        this.withExposedPorts(8123, 9000);
        super.start();
    }

    @Override
    public void stop() {
        // do nothing, JVM handles shut down
    }

    public String getUsername() {
        return "default";
    }

    public String getPassword() {
        return "password";
    }

    public String getDatabaseName() {
        return "default";
    }

    private static class LazyHolder {

        static final ClickhouseContainer INSTANCE =
                new ClickhouseContainer()
                        .withEnv("CLICKHOUSE_USER", "default")
                        .withEnv("CLICKHOUSE_DB", "default")
                        .withEnv("CLICKHOUSE_PASSWORD", "password")
                        .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
                        .withExposedPorts(8123, 9000)
                        .withReuse(true)
                        .waitingFor(Wait.forLogMessage(".*Saved preprocessed configuration.*", 4));
    }
}
