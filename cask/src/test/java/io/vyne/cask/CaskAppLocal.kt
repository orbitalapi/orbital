package io.vyne.cask

class CaskAppLocal {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CaskApp.main(arrayOf("--spring.profiles.active=local"))
        }
    }
}
