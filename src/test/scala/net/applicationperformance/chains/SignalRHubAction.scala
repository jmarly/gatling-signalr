package net.applicationperformance.chains

import com.microsoft.signalr.HubConnection
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Clock
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext


class SignalRHubAction(
                      val name : String,
                      val ctx : ScenarioContext,
                      val toCall : (HubConnection,Session) => Session,
                      val next : io.gatling.core.action.Action
                      ) extends ExitableAction {

  override def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override def clock: Clock = ctx.coreComponents.clock

  override protected def execute(session: Session): Unit = {
    var retSession = session
    val connectionName = name.split("-").head
    val connection = session(connectionName).as[HubConnection]
    try {
      val stTime = clock.nowMillis
      retSession = toCall.apply(connection, session)
      val enTime = clock.nowMillis
      retSession = retSession.logGroupRequestTimings(stTime,enTime)
      statsEngine.logResponse(session.scenario,session.groups,name,stTime,enTime,OK,None, None)
    } catch {
      case ex: Exception => statsEngine.logCrash(session.scenario,session.groups,name,ex.getMessage)
    }
    next ! retSession
  }
}
