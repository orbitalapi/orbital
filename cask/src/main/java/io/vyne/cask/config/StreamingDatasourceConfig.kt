package io.vyne.cask.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource


//@Configuration
//@EnableJpaRepositories
class StreamingDatasourceConfig {

    /*
    //@Primary
    //@Bean("dataSource")
    //fun dataSource(
        @Value("\${spring.datasource.username}") username:String,
        @Value("\${spring.datasource.password}") password:String,
        @Value("\${spring.datasource.url}") url:String,
        @Value("\${spring.datasource.driver}") driverClassName:String
    ): DataSource {
        return DataSourceBuilder.create()
            .username(username)
            .password(password)
            .url(url)
            .driverClassName(driverClassName)
            .build()
    }


    @Bean("streamingDataSource")
    fun streamingDataSource(
        @Value("\${spring.datasource.username}") username:String,
        @Value("\${spring.datasource.password}") password:String,
        @Value("\${spring.datasource.url}") url:String,
        @Value("\${spring.datasource.driver}") driverClassName:String
    ): DataSource {
        return DataSourceBuilder.create()
            .username(username)
            .password(password)
            .url(url)
            .driverClassName(driverClassName)
            .build()
    }

     */

}