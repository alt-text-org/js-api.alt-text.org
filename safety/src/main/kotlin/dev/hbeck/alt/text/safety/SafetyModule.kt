package dev.hbeck.alt.text.safety

import com.google.inject.AbstractModule


class SafetyModule : AbstractModule() {
    override fun configure() {
        bind(ReportHandler::class.java).to(LoggingReportHandler::class.java)
    }
}