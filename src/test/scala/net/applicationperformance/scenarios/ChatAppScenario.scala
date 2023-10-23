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
    .repeat(50) {
      exec(
        Messages.sendMessage("test")
      ).pause(200 millisecond)
    }
    .pause(10)
    .exec(
      Messages.closeHub()
    )
}
