# SignalR with Gatling
## Introduction
While gatling supports WebSockets, calling a signalR server whit the Gatling built-in implementation does not seem to work.
When targetting a SignalR server, the connection is established and upgraded to a WebSocket, but, the
Ws.connect call is stuck waiting and never completes.

ASP.NET Core provides a SignalR java library, so why not try to implement this
to stress a SignalR server.

## Prerequisites

- JDK 1.8
- Latest version of maven
- clone and run the ASP.NET core ChatApp that is available in my reposotories <https://github.com/jmarly/chatapp>

## Running the test

from the root folder of the cloned repository execute

```shell
mvn gatling:test
```

## Interesting bits in this code...

As we call a third party library, by default, response times are not captured

Looking at following construct that builds an exec chain to start
a connection: 

```scala
exec(session => {
  val connection = session("myhubconnection").as[HubConnection]
  connection.start()
  session
})
```
This will start the connection, but will not report timings, even if one wraps the 3rd party execs in a 
group, since the groups will not have individual request response times, the group stats are not reported on.

To capture the reponse time properly, we can move up into the stack and place the SignalR calls into an action.
Implementing all the SignalR calls can be a bit complex, thanks to Scala expressions, this can be super easy.

The call above can be transformed into

```scala
exec(
      Hub("chathub-start") { (connection, session) => {
        connection.start().blockingAwait()
        session
      }}
)
```

In our Action execute we can now apply the expression and wrap it with time catching logic, like this:

```scala
override protected def execute(session: Session): Unit = {
  val connection = session(connectionName).as[HubConnection]
  // ...
  val stTime = clock.nowMillis
  retSession = toCall.apply(connection, session)
  val enTime = clock.nowMillis
  retSession = retSession.logGroupRequestTimings(stTime, enTime)
  statsEngine.logResponse(session.scenario, session.groups, name, stTime, enTime, OK, None, None)
  //...
  next ! retSession
}

```
Check the SignalRHubAction.scala source to see the above code.
The Hub(<hubname>) build call is defined in SignalRHubActionBuilder.sca.

### Handling async calls from the server
In this example, the ChatApp was just modifying the DOM to asynchronously insert the recieved messages in a list.
As a result, we don't really need to do much with the messages we recieve, hence the following callback code:

```scala
private def onRecieveMessage: Action2[String, String] = (user: String, message: String) => {
    logger.trace(s"received $message from $user")
  }
```
The callback call is implemented as follow:
```scala
  // ...
  connection.on("ReceiveMessage", onRecieveMessage, classOf[String], classOf[String])
  // ...
```


## Conclusions

This project demonstrate that it is possible to rapidly integrate simple SignalR logic into a regular Gatling HTTP 
base simulation.

It also shows how to potentially implement 3rd party calls with access to the Gatling internal logic without the 
need to have a full fledge plugin.

In this example, the async callbacks from the server suffer a rudimentary implementation. If time permits, future 
release of this code may show an await implementation of these.

if you want to get in touch : <info@applicationperformance.net>