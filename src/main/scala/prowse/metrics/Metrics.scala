package prowse.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{MetricFilter, MetricRegistry}
import play.Logger
import play.api.Play.{configuration, current}

object MetricsApplication {

  lazy val registry = initialiseMetrics

  private val metricsGraphiteHostnameKey: String = "metrics.graphite.hostname"

  def initialiseMetrics: MetricRegistry = {
    val reg: MetricRegistry = new MetricRegistry()

    val maybeMetricsHostname: Option[String] = configuration.getString(metricsGraphiteHostnameKey).filterNot(_.isEmpty)

    if (maybeMetricsHostname.isEmpty) {
      Logger.warn( s"""Graphite reporting disabled as no "$metricsGraphiteHostnameKey" provided""")

    } else {
      maybeMetricsHostname.map(
        new Graphite(_, configuration.getInt("metrics.graphite.port").get)).map(
          GraphiteReporter.forRegistry(reg)
            .prefixedWith(configuration.getString("metrics.prefix").get)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .build
        ).foreach(_.start(10, TimeUnit.SECONDS))

      Logger.info( s"""Graphite reporting initialised to host "${configuration.getString(metricsGraphiteHostnameKey).get}"""")
    }

    reg
  }
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsApplication.registry
}
