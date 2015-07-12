package prowse.controllers

import javax.inject.Singleton

import nl.grons.metrics.scala.Timer
import play.api.Play.current
import play.api.mvc._
import prowse.actors.{ChatEventBus, ClientWebSocketActor}
import prowse.metrics.Instrumented

@Singleton
class WebSocketController extends Instrumented {

  val get: Timer = metrics.timer("get")

  private val chatEventBus: ChatEventBus = new ChatEventBus()

  // TODO: support protobuf payload
  def socket = get.time {
    WebSocket.acceptWithActor[String, String] { request => out =>
      ClientWebSocketActor.props(out, chatEventBus)
    }
  }

}
