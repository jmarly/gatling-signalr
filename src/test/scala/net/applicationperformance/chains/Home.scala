package net.applicationperformance.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

object Home {

  def get(): ChainBuilder = exec(
    http("root")
      .get("/")
  )

  def signIn(): ChainBuilder = group("sign-in") {
    exec(
      http("post-token-signin")
        .post("/api/TokenApi/SignIn")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """{
            | "username" : "#{username}",
            | "password" : ""
            |}""".stripMargin))
        .check(jsonPath("$..token").saveAs("token"))
    )
  }
}
