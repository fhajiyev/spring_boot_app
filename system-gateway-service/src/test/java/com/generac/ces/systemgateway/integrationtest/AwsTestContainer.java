package com.generac.ces.systemgateway.integrationtest;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Slf4j
@Getter
/** A wrapper class around localstackcontainer * */
public class AwsTestContainer extends LocalStackContainer {

    private AmazonKinesis kinesisClient;
    private StaticCredentialsProvider credentialsProvider;

    private AwsTestContainer() {
        super(DockerImageName.parse("localstack/localstack:0.14.5"));
    }

    @Override
    public void start() {
        if (super.isRunning()) {
            return;
        }
        super.withServices().withServices(Service.KINESIS).withReuse(true);

        super.start();
        super.waitingFor(Wait.forHealthcheck());
        credentialsProvider =
                StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(this.getAccessKey(), this.getSecretKey()));

        // need to disable for it to work with localstack
        System.setProperty(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, "true");
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
        kinesisClient =
                AmazonKinesisClientBuilder.standard()
                        .withEndpointConfiguration(
                                new AwsClientBuilder.EndpointConfiguration(
                                        this.getEndpointOverride(Service.KINESIS).toString(),
                                        this.getRegion()))
                        .build();
    }

    private static class LazyHolder {
        static final AwsTestContainer INSTANCE = new AwsTestContainer();
    }

    public static AwsTestContainer getInstance() {
        return LazyHolder.INSTANCE;
    }
}
