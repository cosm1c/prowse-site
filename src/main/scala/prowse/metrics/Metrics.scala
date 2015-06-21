package prowse.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import play.api.Play.{configuration, current}

object MetricsApplication {
  val registry = new com.codahale.metrics.MetricRegistry()

  // TODO: Consider using PickledGraphite for improved performance?
  val graphite: Graphite = new Graphite(
    configuration.getString("metrics.graphite.hostname").get,
    configuration.getInt("metrics.graphite.port").get)

  val reporter: GraphiteReporter = GraphiteReporter.forRegistry(MetricsApplication.registry)
    .prefixedWith(configuration.getString("metrics.prefix").get)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .filter(MetricFilter.ALL)
    .build(graphite)

  reporter.start(10, TimeUnit.SECONDS)
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsApplication.registry
}
