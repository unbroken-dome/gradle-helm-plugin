package com.citi.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Action
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.reflect.Instantiator
import javax.inject.Inject


interface HelmPublishingRepositoryContainer : PolymorphicDomainObjectContainer<HelmPublishingRepository> {

    fun artifactory(
        name: String,
        configuration: Action<ArtifactoryHelmPublishingRepository>
    ): ArtifactoryHelmPublishingRepository =
        create(name, ArtifactoryHelmPublishingRepository::class.java, configuration)


    fun artifactory(
        configuration: Action<ArtifactoryHelmPublishingRepository>
    ): ArtifactoryHelmPublishingRepository =
        artifactory("default", configuration)


    fun chartMuseum(
        name: String,
        configuration: Action<ChartMuseumHelmPublishingRepository>
    ): ChartMuseumHelmPublishingRepository =
        create(name, ChartMuseumHelmPublishingRepository::class.java, configuration)


    fun chartMuseum(
        configuration: Action<ChartMuseumHelmPublishingRepository>
    ): ChartMuseumHelmPublishingRepository =
        chartMuseum("default", configuration)


    fun harbor(
        name: String,
        configuration: Action<HarborHelmPublishingRepository>
    ): HarborHelmPublishingRepository =
        create(name, HarborHelmPublishingRepository::class.java, configuration)


    fun harbor(
        configuration: Action<HarborHelmPublishingRepository>
    ): HarborHelmPublishingRepository =
        harbor("default", configuration)

    fun nexus(
        name: String,
        configuration: Action<NexusHelmPublishingRepository>
    ): NexusHelmPublishingRepository =
        create(name, NexusHelmPublishingRepository::class.java, configuration)


    fun nexus(
        configuration: Action<NexusHelmPublishingRepository>
    ): NexusHelmPublishingRepository =
        nexus("default", configuration)

    fun gitlab(
        name: String,
        configuration: Action<GitlabHelmPublishingRepository>
    ): GitlabHelmPublishingRepository =
        create(name, GitlabHelmPublishingRepository::class.java, configuration)

    fun gitlab(
        configuration: Action<GitlabHelmPublishingRepository>
    ): GitlabHelmPublishingRepository =
        gitlab("default", configuration)
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
        registerFactory(ArtifactoryHelmPublishingRepository::class.java) { name ->
            objects.newArtifactoryHelmPublishingRepository(name)
        }
        registerFactory(ChartMuseumHelmPublishingRepository::class.java) { name ->
            objects.newChartMuseumHelmPublishingRepository(name)
        }
        registerFactory(HarborHelmPublishingRepository::class.java) { name ->
            objects.newHarborHelmPublishingRepository(name)
        }
        registerFactory(CustomHelmPublishingRepository::class.java) { name ->
            objects.newCustomHelmPublishingRepository(name)
        }
        registerFactory(NexusHelmPublishingRepository::class.java) { name ->
            objects.newNexusHelmPublishingRepository(name)
        }
        registerFactory(GitlabHelmPublishingRepository::class.java) { name ->
            objects.newGitlabHelmPublishingRepository(name)
        }

        // Default type is "custom"
        registerDefaultFactory { name ->
            objects.newCustomHelmPublishingRepository(name)
        }
    }
}


internal fun ObjectFactory.newHelmPublishingRepositoryContainer(instantiator: Instantiator): HelmPublishingRepositoryContainer =
    newInstance(DefaultHelmPublishingRepositoryContainer::class.java, instantiator)
