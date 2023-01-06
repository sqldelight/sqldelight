package app.cash.sqldelight.gradle

import org.gradle.api.*
import org.gradle.api.model.*
import org.gradle.api.provider.*

internal fun <T> NamedDomainObjectContainer<T>.maybeRegister(
    name: String,
    configure: Action<T>,
): NamedDomainObjectProvider<T> = if (name in names) {
    named(name, configure)
} else register(name, configure)

internal fun <T> NamedDomainObjectContainer<T>.elements(
    providerFactory: ProviderFactory,
): Provider<Set<T>> = providerFactory.provider {
    this
}

internal inline fun <reified T> Provider<Collection<T>>.resolveRecursive(
    providerFactory: ObjectFactory,
    crossinline getChildren: T.() -> Provider<Collection<T>>,
    onError: T.() -> Nothing
): Provider<Collection<T>> {
    val s: Provider<Collection<T>> = map { items ->
        val provider = providerFactory.domainObjectSet(T::class.java)
        for (item in items) { 
            provider.addAllLater(item.getChildren())
        }
        provider
    }
    return s
}
