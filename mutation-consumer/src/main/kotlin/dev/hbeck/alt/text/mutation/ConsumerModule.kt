package dev.hbeck.alt.text.mutation

import com.google.inject.AbstractModule
import com.google.inject.name.Names


class ConsumerModule(private val configuration: MutationHandlerConfiguration) : AbstractModule() {
    override fun configure() {
        bind(UserActionHandler::class.java).annotatedWith(Names.named("storageHandler"))
            .to(StorageUserActionHandler::class.java)
        bind(UserActionHandler::class.java).annotatedWith(Names.named("queueingHandler"))
            .to(QueuingUserActionHandler::class.java)
    }
}