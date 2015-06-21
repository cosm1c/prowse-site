package prowse.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import prowse.actors.ClientWebSocketActor.GLOBAL_TOPIC

class ClientWebSocketActor(out: ActorRef, chatEventBus: ChatEventBus) extends Actor with ActorLogging {

  val chatName = s"User ${self.path.elements.drop(2).head}"

  // Only one
  chatEventBus.subscribe(self, GLOBAL_TOPIC)

  out ! s"Greetings $chatName"
  chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"User $chatName connected"))

  log.info("ClientWebSocketActor started {}", out)

  override def receive = {
    case MsgEnvelope(topic, payload) =>
      log.debug("MsgEnvelope({}, {})", topic, payload)
      out ! payload

    case msg: String =>
      log.debug("Message: {}", msg)
      chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"[$chatName] $msg"))
  }

  override def postStop(): Unit = {
    log.info("postStop")
    chatEventBus.publish(MsgEnvelope(GLOBAL_TOPIC, s"User $chatName disconnected"))
    chatEventBus.unsubscribe(self)
  }
}

object ClientWebSocketActor {

  val GLOBAL_TOPIC = "GLOBAL-TOPIC"

  def props(out: ActorRef, chatEventBus: ChatEventBus) = Props(new ClientWebSocketActor(out, chatEventBus))

}
