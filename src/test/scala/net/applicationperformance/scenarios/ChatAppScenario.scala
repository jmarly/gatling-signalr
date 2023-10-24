package net.applicationperformance.scenarios

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import net.applicationperformance.chains._

import scala.concurrent.duration.DurationInt


object ChatAppScenario {

  def ChatScenario(users: IndexedSeq[Map[String, String]]): ScenarioBuilder = scenario("Chat")
    .exec(
      feed(users),
      Home.get(),
      Home.signIn(),
      Messages.view()
    )
    .repeat(50,"messageid") {
      exec(
        Messages.sendMessage("sending message #{messageid} to the server")
      ).pause(200 millisecond)
    }
    .exec(
      Messages.stopHub(),
      Messages.closeHub()
    )
}
