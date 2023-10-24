package net.applicationperformance.chains

import com.microsoft.signalr.HubConnection
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation.FalseSuccess.recover
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.recorder.internal.bouncycastle.oer.its.ieee1609dot2.basetypes.Duration

import java.util.concurrent.{ExecutorService, Executors}

class SignalRHubAction(
                   val name : String,
                      val ctx : ScenarioContext,
                      val toCall : (HubConnection,Session) => Session,
                      val next : io.gatling.core.action.Action
                      ) extends ChainableAction {

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
                 ) extends Runnable {
  override def run(): Unit = {
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
        print(s"excection ${session.userId}->$name ${ex.getMessage}")
        statsEngine.logCrash(session.scenario,session.groups,name,ex.getMessage)
    }
    // resume the execution chain...
    retSession.eventLoop.execute(() => next ! retSession)
  }
}
