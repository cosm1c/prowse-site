package prowse.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import play.Logger
import play.api.Play.{configuration, current}

object MetricsApplication {
  val registry = new com.codahale.metrics.MetricRegistry()

  private val metricsGraphiteHostnameKey: String = "metrics.graphite.hostname"

  // TODO: Consider using PickledGraphite for improved performance?
  val graphite: Option[Graphite] = configuration.getString(metricsGraphiteHostnameKey).filterNot(_.isEmpty)
    .map(new Graphite(_, configuration.getInt("metrics.graphite.port").get))

  val reporter: Option[GraphiteReporter] = graphite.map(
    GraphiteReporter.forRegistry(MetricsApplication.registry)
      .prefixedWith(configuration.getString("metrics.prefix").get)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build)

  reporter.foreach(_.start(10, TimeUnit.SECONDS))

  if (reporter.isDefined)
    Logger.warn( s"""Graphite reporting initialised to host "${configuration.getString(metricsGraphiteHostnameKey).get}"""")
  else
    Logger.warn( s"""Graphite reporting disabled as no "$metricsGraphiteHostnameKey" provided""")
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsApplication.registry
}
