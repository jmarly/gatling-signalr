package net.applicationperformance.chains

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session.{Expression, GroupBlock}
import com.microsoft.signalr._
import java.lang.System.currentTimeMillis

object Messages {

  def view() = group("messages") {
    exec(
      http("post-messages")
        .post("/Chat/Messages")
        .formParam("token", "#{token}")
    ).exec(
      connect(),
      joinHub()
    )
  }

  def sendMessage(message: Expression[String]) = exec(session => {
      val connection = session("hubConnection").as[HubConnection]
      val clockStart = currentTimeMillis
      connection.send("SendMessage", message(session).toOption.getOrElse("default message"))
      session
    })


  def closeHub() = exec(session => {
    val connection = session("hubConnection").as[HubConnection]
    connection.stop()
    session
  })

  private def onRecieveMessage: Action2[String, String] = (user: String, message: String) => {
    println(s"received $message from $user")
  }

  private def
  connect() = group("hub-connect") {
    exec(session => {
      val connection = HubConnectionBuilder.create(s"http://localhost:5237/ChatHub").build()
      connection.on("ReceiveMessage", onRecieveMessage, classOf[String], classOf[String])
      connection.start().blockingAwait()
      session.set("hubConnection", connection)
    })
  }

  private def joinHub() = group("hub-join") {
    exec(session => {
      val connection = session("hubConnection").as[HubConnection]
      connection.send("JoinChatHub", session("token").as[String])
      session
    })
  }

}
