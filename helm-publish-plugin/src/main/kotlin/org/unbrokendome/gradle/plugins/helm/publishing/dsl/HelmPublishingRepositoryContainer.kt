package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Action
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.reflect.Instantiator
import javax.inject.Inject


interface HelmPublishingRepositoryContainer : PolymorphicDomainObjectContainer<HelmPublishingRepository> {

    @JvmDefault
    fun artifactory(
        name: String,
        configuration: Action<ArtifactoryHelmPublishingRepository>
    ): ArtifactoryHelmPublishingRepository =
        create(name, ArtifactoryHelmPublishingRepository::class.java, configuration)


    @JvmDefault
    fun artifactory(
        configuration: Action<ArtifactoryHelmPublishingRepository>
    ): ArtifactoryHelmPublishingRepository =
        artifactory("default", configuration)


    @JvmDefault
    fun chartMuseum(
        name: String,
        configuration: Action<ChartMuseumHelmPublishingRepository>
    ): ChartMuseumHelmPublishingRepository =
        create(name, ChartMuseumHelmPublishingRepository::class.java, configuration)


    @JvmDefault
    fun chartMuseum(
        configuration: Action<ChartMuseumHelmPublishingRepository>
    ): ChartMuseumHelmPublishingRepository =
        chartMuseum("default", configuration)


    @JvmDefault
    fun harbor(
        name: String,
        configuration: Action<HarborHelmPublishingRepository>
    ): HarborHelmPublishingRepository =
        create(name, HarborHelmPublishingRepository::class.java, configuration)


    @JvmDefault
    fun harbor(
        configuration: Action<HarborHelmPublishingRepository>
    ): HarborHelmPublishingRepository =
        harbor("default", configuration)

    @JvmDefault
    fun nexus(
        name: String,
        configuration: Action<NexusHelmPublishingRepository>
    ): NexusHelmPublishingRepository =
        create(name, NexusHelmPublishingRepository::class.java, configuration)


    @JvmDefault
    fun nexus(
        configuration: Action<NexusHelmPublishingRepository>
    ): NexusHelmPublishingRepository =
        nexus("default", configuration)
}


@Suppress("LeakingThis")
private open class DefaultHelmPublishingRepositoryContainer
@Inject constructor(
    instantiator: Instantiator,
    objects: ObjectFactory
) : DefaultPolymorphicDomainObjectContainer<HelmPublishingRepository>(
    HelmPublishingRepository::class.java, instantiator, CollectionCallbackActionDecorator.NOOP
), HelmPublishingRepositoryContainer {

    init {
        registerFactory(
            ArtifactoryHelmPublishingRepository::class.java,
            objects::newArtifactoryHelmPublishingRepository
        )
        registerFactory(
            ChartMuseumHelmPublishingRepository::class.java,
            objects::newChartMuseumHelmPublishingRepository
        )
        registerFactory(
            HarborHelmPublishingRepository::class.java,
            objects::newHarborHelmPublishingRepository
        )
        registerFactory(
            CustomHelmPublishingRepository::class.java,
            objects::newCustomHelmPublishingRepository
        )
        registerFactory(
            NexusHelmPublishingRepository::class.java,
            objects::newNexusHelmPublishingRepository
        )

        // Default type is "custom"
        registerDefaultFactory(
            objects::newCustomHelmPublishingRepository
        )
    }
}


internal fun ObjectFactory.newHelmPublishingRepositoryContainer(instantiator: Instantiator): HelmPublishingRepositoryContainer =
    newInstance(DefaultHelmPublishingRepositoryContainer::class.java, instantiator)
