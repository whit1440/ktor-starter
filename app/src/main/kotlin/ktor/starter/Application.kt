package ktor.starter

import io.ktor.server.application.*
import ktor.starter.plugins.configureHTTP
import ktor.starter.plugins.configureSerialization
import ktor.starter.data.plugins.connectToPostgres

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    connectToPostgres()
}