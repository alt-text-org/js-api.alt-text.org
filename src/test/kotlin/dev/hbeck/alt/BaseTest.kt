package dev.hbeck.alt

import com.google.inject.Guice
import com.google.inject.Injector
import org.junit.Before
import org.junit.Ignore


@Ignore
abstract class BaseTest {

    private val injector: Injector = Guice.createInjector(TestModule())

    @Before
    fun setUp() {
        injector.injectMembers(this)
    }
}