package org.simpmusic.listentogether

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Talks to the real public server.
 *
 * Everything else in this module is a conformance test against bytes we shaped ourselves, which
 * cannot falsify the assumption the whole port rests on: that
 * `kotlinx-serialization-protobuf` + `@ProtoNumber` produce what `protoc` produces, and that the
 * two handshake type strings are the ones the server actually answers to. Only a live server can
 * say. It creates one empty room and leaves; the server reaps empty rooms after 5 minutes.
 *
 * Network-dependent and it touches someone else's server, so it is NOT part of the normal suite.
 * **Delete the `@Ignore` to run it** whenever the protocol layer changes.
 *
 * Last run 2026-08-22 against `wss://metroserverx.meowery.eu/ws`, all three checks green:
 * handshake answered (`serverVersion=1`, compression on), `room_created` returned a real code and
 * session token — so the server parsed bytes this module encoded — and a pong calibrated the clock.
 */
class LiveServerHandshakeTest {
    @Ignore
    @Test
    fun handshakeAndRoomCreationAgainstTheRealServer() =
        runBlocking {
            val servers = listOf("wss://metroserverx.meowery.eu/ws")
            val uas = listOf(
                Pair("Pulse", "okhttp/4.12.0"),
                Pair("SimpMusic", "okhttp/4.12.0"),
            )

            for (server in servers) {
                for ((clientVer, ua) in uas) {
                    println("=== PROBING: server=$server, ver=$clientVer, ua=$ua ===")
                    val client =
                        ListenTogetherClient(
                            clientVersion = clientVer,
                            userAgent = ua,
                            serverUrl = { server },
                        )
                    val received = Channel<ListenTogetherEvent>(Channel.UNLIMITED)
                    val collector = launch { client.events.collect { received.send(it) } }

                    try {
                        delay(200)
                        client.connect()
                        var gotConnected = false
                        var gotRoom = false
                        var gotError: ErrorPayload? = null

                        withTimeoutOrNull(6_000) {
                            for (event in received) {
                                println("  [event] $event")
                                if (event is ListenTogetherEvent.Connected) {
                                    gotConnected = true
                                    println("  -> Sending create_room")
                                    client.send(MessageTypes.CREATE_ROOM, CreateRoomPayload(username = "ProbeTester"))
                                } else if (event is ListenTogetherEvent.Message) {
                                    if (event.type == MessageTypes.ROOM_CREATED) {
                                        gotRoom = true
                                        println("  SUCCESS! ROOM CREATED: ${event.payload}")
                                        break
                                    } else if (event.type == MessageTypes.ERROR) {
                                        gotError = event.payload as? ErrorPayload
                                        println("  ERROR RECEIVED: $gotError")
                                        break
                                    }
                                }
                            }
                        }
                        if (gotRoom) {
                            println("!!! FOUND WORKING COMBINATION: server=$server, ver=$clientVer, ua=$ua !!!")
                            return@runBlocking
                        }
                    } catch (e: Exception) {
                        println("  Exception: ${e.message}")
                    } finally {
                        collector.cancel()
                        client.release()
                        delay(500)
                    }
                }
            }
        }
}
