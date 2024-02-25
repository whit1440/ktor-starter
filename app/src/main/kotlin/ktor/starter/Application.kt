package ktor.starter

import io.ktor.server.application.*
import wiz.games.plugins.configureHTTP
import wiz.games.plugins.configureRouting
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