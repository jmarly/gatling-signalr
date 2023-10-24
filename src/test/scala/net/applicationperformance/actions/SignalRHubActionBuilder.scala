package net.applicationperformance.actions

import com.microsoft.signalr.HubConnection
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session
import io.gatling.core.structure.ScenarioContext

object Hub {
  def apply(name : String)(toCall : (HubConnection,Session) => Session) = new SignalRHubActionBuilder(name,toCall)
}

class SignalRHubActionBuilder(name : String, toCall : (HubConnection,Session) => Session) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = new SignalRHubAction(name,ctx,toCall,next)
}
