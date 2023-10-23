package net.applicationperformance.chains

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session.{Expression, GroupBlock}
import com.microsoft.signalr._
import com.typesafe.scalalogging.LazyLogging
import io.gatling.core.structure.ChainBuilder

object Messages extends LazyLogging{

  def view() : ChainBuilder = group("messages") {
    exec(
      postToMessages(),
      connect(),
      joinHub()
    )
  }

  private def onRecieveMessage: Action2[String, String] = (user: String, message: String) => {
    logger.trace(s"received $message from $user")
  }

  private def postToMessages() = exec(
    http("post-messages")
      .post("/Chat/Messages")
      .formParam("token", "#{token}")
  )

  private def connect() = exec(session => {
    val baseUrl = session("gatling.http.cache.baseUrl").as[String]
    val hubUrl = baseUrl+ "/ChatHub"
      session.set("chathub", HubConnectionBuilder.create(hubUrl).build())
    }).exec(
      Hub("chathub-start") { (connection, session) => {
        connection.on("ReceiveMessage", onRecieveMessage, classOf[String], classOf[String])
        connection.start().blockingAwait()
        session
      }}
    )


  private def joinHub() = exec(
      Hub("chathub-join") { (connection, session) => {
        connection.send("JoinChatHub", session("token").as[String])
        session
      }}
  )

  def sendMessage(message: Expression[String]) : ChainBuilder = group("messages") {
    exec(
      Hub("chathub-send") { (connection, session) => {
        connection.send("SendMessage", message(session).toOption.getOrElse("default message"))
        session
      }}
    )
  }

  def closeHub() : ChainBuilder = group("messages") {
    exec(
      Hub("chathub-stop") { (connection, session) => {
        connection.stop()
        session
      }}
    )
  }
}
