package prowse.actors

import javax.inject.Singleton

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}

case class MsgEnvelope(topic: String, payload: String)

@Singleton
class ChatEventBus extends EventBus with LookupClassification {
  type Event = MsgEnvelope
  type Classifier = String
  type Subscriber = ActorRef

  override protected def classify(event: Event): Classifier = event.topic

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

  override protected val mapSize: Int = 128

}
