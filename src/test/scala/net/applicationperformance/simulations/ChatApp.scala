package net.applicationperformance.simulations
import io.gatling.http.Predef._
import io.gatling.core.Predef._
import net.applicationperformance.chains.Home
import net.applicationperformance.scenarios._
class ChatApp extends Simulation {

    private val protocol = http
      .baseUrl("http://localhost:5237")
      .acceptEncodingHeader("gzip, deflate, br")
      .connectionHeader("keep-alive")



    private val users   = Seq(
        Map("username" -> "user1"),
        Map("username" -> "user2"),
        Map("username" -> "user3"),
        Map("username" -> "user4"),
    ).toIndexedSeq

    setUp(ChatAppScenario.ChatScenario(users).inject(atOnceUsers(4))).protocols(protocol)
}
