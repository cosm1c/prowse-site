package prowse.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{MetricFilter, MetricRegistry}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object MetricsApplication {

  private[this] val logger = Logger(LoggerFactory.getLogger("name"))
  private val configuration: Config = ConfigFactory.load()
  private val metricsGraphiteHostnameKey: String = "metrics.graphite.hostname"

  lazy val registry = initialiseMetrics

  private def initialiseMetrics: MetricRegistry = {
    val reg: MetricRegistry = new MetricRegistry()

    val metricsHostname: String = configuration.getString(metricsGraphiteHostnameKey)

    if (metricsHostname.trim.isEmpty) {
      logger.warn( s"""Graphite reporting disabled as no "$metricsGraphiteHostnameKey" provided""")

    } else {
      val graphiteReporter: GraphiteReporter = GraphiteReporter.forRegistry(reg)
        .prefixedWith(configuration.getString("metrics.prefix"))
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .filter(MetricFilter.ALL)
        .build(new Graphite(metricsHostname, configuration.getInt("metrics.graphite.port")))

      graphiteReporter.start(10, TimeUnit.SECONDS)

      logger.info( s"""Graphite reporting initialised to host "$metricsHostname"""")
    }

    reg
  }
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsApplication.registry
}
