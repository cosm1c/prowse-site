package prowse.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import prowse.actors.ClientWebSocketActor.GLOBAL_TOPIC

import scala.concurrent.duration._

class ClientWebSocketActor(out: ActorRef, chatEventBus: ChatEventBus) extends Actor with ActorLogging {

  import context.dispatcher

  val chatName = s"User ${self.path.elements.drop(2).head}"
  chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"User $chatName connected"))
  chatEventBus.subscribe(self, GLOBAL_TOPIC)

  private case object Tick

  val tick = context.system.scheduler.schedule(0.millis, 200.millis, self, Tick)

  log.info("ClientWebSocketActor started {}", out)

  override def receive = {
    case Tick =>
      out ! System.nanoTime().toString

    case MsgEnvelope(topic, payload) =>
      log.debug("MsgEnvelope({}, {})", topic, payload)
      out ! payload

    case msg: String =>
      log.debug("Message: {}", msg)
      chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"[$chatName] $msg"))
  }

  override def postStop(): Unit = {
    log.info("postStop")
    tick.cancel()
    chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"User $chatName disconnected"))
    chatEventBus.unsubscribe(self)
  }
}

object ClientWebSocketActor {

  val GLOBAL_TOPIC = "GLOBAL-TOPIC"

  def props(out: ActorRef, chatEventBus: ChatEventBus) = Props(new ClientWebSocketActor(out, chatEventBus))

}
