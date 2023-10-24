package net.applicationperformance.actions

import java.util.concurrent.{ExecutorService, Executors}
import com.microsoft.signalr.HubConnection
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import com.typesafe.scalalogging.StrictLogging


class SignalRHubAction(
                   val name : String,
                      val ctx : ScenarioContext,
                      val toCall : (HubConnection,Session) => Session,
                      val next : io.gatling.core.action.Action
                      ) extends ChainableAction with StrictLogging {

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
    logger.trace(s"user ${session.userId} submitted executor to pool: ${SignalRExecutor.threadPool.toString}")
  }
}

object SignalRExecutor {
  val threadPool: ExecutorService = Executors.newCachedThreadPool()
}

class SignalRCallExecutor(
                 val name : String,
                 val session : Session,
                 val toCall : (HubConnection,Session) => Session,
                 val statsEngine: StatsEngine,
                 val clock : Clock,
                 val next : Action
                 ) extends Runnable with StrictLogging {
  override def run(): Unit = {
    logger.trace(s"user ${session.userId} running $name")
    var retSession = session
    val connectionName = name.split("-").head
    val connection = session(connectionName).as[HubConnection]
    try {
      val stTime = clock.nowMillis
      retSession = toCall.apply(connection,session)
      val enTime = clock.nowMillis
      retSession = retSession.logGroupRequestTimings(stTime,enTime)
      statsEngine.logResponse(session.scenario,session.groups,name,stTime,enTime,OK,None, None)
    } catch {
      case ex: Exception =>
        logger.error(s"user ${session.userId} is running ${session.userId}->$name ${ex.getMessage}")
        statsEngine.logCrash(session.scenario,session.groups,name,ex.getMessage)
    }
    // resume the execution chain...
    retSession.eventLoop.execute(() => next ! retSession)
  }
}
