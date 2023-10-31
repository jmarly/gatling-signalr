# SignalR with Gatling
## Introduction
While gatling supports WebSockets, calling a signalR server whit the Gatling built-in implementation does not seem to work.
When targetting a SignalR server, the connection is established and upgraded to a WebSocket, but, the
Ws.connect call is stuck waiting and never completes.

ASP.NET Core provides a SignalR java library, so why not try to implement this
to stress a SignalR server.

## Prerequisites

- JDK 19
- Latest version of maven
- clone and run the ASP.NET core ChatApp that is available in my reposotories <https://github.com/jmarly/chatapp>

## Running the test

from the root folder of the cloned repository execute

```shell
mvn gatling:test
```

## Interesting bits in this code...

As we call a third party library we must consider a few things:

1. The following exec block will 'work' ie. call the SignalR API, but it will potentially block the other vittual 
   users while the SignalR call is executing as we are not supposed to make bloccking calls this way.
2. The response time will not be reported.

The following implementation would potentially block the Gatling execution threads, to avoid this, we move the 
SignalR calls to their own thread pool and insure the Galting threads are only used to dispatch the API calls.
```scala
exec(session => {
  val connection = session("myhubconnection").as[HubConnection]
  connection.start()
  session
})
```
We resolve the above issue by building underlying actions to submit the actual API execution to a separate thread 
pool. To build the chain, we have the Hub action buolder:

```scala
exec(
      Hub("chathub-start") { (connection, session) => {
        connection.start().blockingAwait()
        session
      }}
)
```

In our Hub Action execute we create a new thread to handle the SignalR API call. This is to ensure the blocking signslr 
call don't block the Virtual users event loop.  

```scala
override protected def execute(session: Session): Unit = {
  // prevent blocking the event loop with api blocking calls...
  SignalRExecutor.threadPool.submit(new SignalRCallExecutor(
    name,
    session,
    toCall,
    ctx.coreComponents.statsEngine,
    ctx.coreComponents.clock,
    next
  ))
}
```
In the SignalR executor thread we do:
```scala
    // ..
    val stTime = clock.nowMillis
    retSession = toCall.apply(connection,session)
    val enTime = clock.nowMillis
    retSession = retSession.logGroupRequestTimings(stTime,enTime)
    statsEngine.logResponse(session.scenario,session.groups,name,stTime,enTime,OK,None, None)
    // ...
```
After logging the response time, we place the next action in the event loop to resume the execution chain.

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