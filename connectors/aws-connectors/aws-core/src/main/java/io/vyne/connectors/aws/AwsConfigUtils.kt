package com.orbitalhq.connectors.aws

import com.orbitalhq.connectors.config.aws.*
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region

fun <T : AwsClientBuilder<*, *>> T.configureWithExplicitValuesIfProvided(configuration: AwsConnectionConfiguration): T {
    if (configuration.accessKey != null && configuration.secretKey != null) {
        this.credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    configuration.accessKey,
                    configuration.secretKey
                )
            )
        )
    }

    if (configuration.region != null) {
        this.region(Region.of(configuration.region))
    }

    if (configuration.endPointOverride != null) {
        this.endpointOverride(java.net.URI.create(configuration.endPointOverride))
    }

    return this
}
