package io.vyne.cask

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CaskApp {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CaskApp::class.java, *args)
        }
    }

}
