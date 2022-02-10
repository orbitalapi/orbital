package io.vyne.connectors.aws.s3.registry

import io.vyne.connectors.aws.s3.AwsS3ConnectionConnectorConfiguration
import io.vyne.connectors.registry.ConnectionRegistry

interface AwsS3ConnectionRegistry: ConnectionRegistry<AwsS3ConnectionConnectorConfiguration>
