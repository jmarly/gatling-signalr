package net.applicationperformance.scenarios

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import net.applicationperformance.chains._


object ChatAppScenario {

  def ChatScenario(users: IndexedSeq[Map[String, String]]): ScenarioBuilder = scenario("Chat")
    .exec(session => {
      println(s"SESSION===>$session")
      session
    })
    .exec(
      feed(users),
      Home.get(),
      Home.signIn(),
      Messages.view()
    )
    .repeat(50) {
      exec(
        Messages.sendMessage("test")
      )
    }.exec(
    Messages.closeHub()
  )
}
