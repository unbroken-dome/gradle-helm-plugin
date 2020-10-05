package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.PolymorphicDomainObjectContainer


/**
 * Base class for a rule that creates a domain object of type [T] from a domain object of type [S]
 * if it matches a certain name pattern.
 */
internal abstract class AbstractPatternRule<S : Named, T : Any>
/**
 * Constructs an instance of [AbstractPatternRule], creating target objects using a creator function.
 */
constructor(
    /** A function that creates and configures the target object on a successful name match. */
    private val targetCreator: (name: String, configureAction: Action<T>) -> Unit,
    /** The source container. */
    protected val sourceContainer: NamedDomainObjectCollection<S>,
    /** The pattern that is used for matching the source and target names. */
    protected val namePattern: RuleNamePattern
) : AbstractRule() {

    /**
     * Constructs an instance of [AbstractPatternRule] for a target [NamedDomainObjectContainer],
     * creating target objects using [NamedDomainObjectContainer.create].
     */
    constructor(
        /** The target container. */
        targetContainer: NamedDomainObjectContainer<T>,
        /** The source container. */
        sourceContainer: NamedDomainObjectCollection<S>,
        /** The pattern that is used for matching the source and target names. */
        namePattern: RuleNamePattern
    ) : this(
        { name, action -> targetContainer.create(name, action) }, sourceContainer, namePattern
    )


    /**
     * Constructs an instance of [AbstractPatternRule] for a target [PolymorphicDomainObjectContainer],
     * creating target objects using [PolymorphicDomainObjectContainer.create].
     */
    constructor(
        /** The type of target objects that this rule creates. */
        targetType: Class<T>,
        /** The target container. */
        targetContainer: PolymorphicDomainObjectContainer<in T>,
        /** The source container. */
        sourceContainer: NamedDomainObjectCollection<S>,
        namePattern: RuleNamePattern
    ) : this(
        { name, action -> targetContainer.create(name, targetType, action) }, sourceContainer, namePattern
    )


    override fun getDescription(): String = namePattern.toString()


    final override fun apply(targetName: String) {
        if (namePattern.matches(targetName)) {
            namePattern.findSource(targetName, sourceContainer)
                ?.let { source ->
                    targetCreator(targetName, Action { it.configureFrom(source) })
                }
        }
    }


    /**
     * Configures the target object after creation.
     *
     * @param source the source object
     */
    protected abstract fun T.configureFrom(source: S)
}


/**
 * Base class for a rule that creates a domain object of type [T] from two domain objects of type [S1] and [S2]
 * if it matches a certain name pattern.
 */
internal abstract class AbstractPatternRule2<S1 : Named, S2 : Named, T : Any>(
    /** A function that creates and configures the target object on a successful name match. */
    private val targetCreator: (name: String, configureAction: Action<T>) -> Unit,
    /** The container for the first type of source object. */
    protected val sourceContainer1: NamedDomainObjectCollection<S1>,
    /** The container for the second type of source object. */
    protected val sourceContainer2: NamedDomainObjectCollection<S2>,
    /** The pattern that is used for matching the source and target names. */
    protected val namePattern: RuleNamePattern2
) : AbstractRule() {

    constructor(
        targetContainer: NamedDomainObjectContainer<T>,
        sourceContainer1: NamedDomainObjectCollection<S1>,
        sourceContainer2: NamedDomainObjectCollection<S2>,
        namePattern: RuleNamePattern2
    ) : this(
        { name, action -> targetContainer.create(name, action) }, sourceContainer1, sourceContainer2, namePattern
    )


    constructor(
        targetType: Class<T>,
        targetContainer: PolymorphicDomainObjectContainer<in T>,
        sourceContainer1: NamedDomainObjectCollection<S1>,
        sourceContainer2: NamedDomainObjectCollection<S2>,
        namePattern: RuleNamePattern2
    ) : this(
        { name, action -> targetContainer.create(name, targetType, action) }, sourceContainer1, sourceContainer2,
        namePattern
    )


    override fun getDescription(): String = namePattern.toString()


    final override fun apply(targetName: String) {
        if (namePattern.matches(targetName)) {
            namePattern.findSources(targetName, sourceContainer1, sourceContainer2)
                ?.let { (source1, source2) ->
                    targetCreator(targetName, Action { it.configureFrom(source1, source2) })
                }
        }
    }


    /**
     * Configures the target object after creation.
     *
     * @param source1 the first source object
     * @param source2 the second source object
     */
    protected abstract fun T.configureFrom(source1: S1, source2: S2)
}


/**
 * Base class for a rule that creates a domain object of type [T] from two domain objects of type [SOuter] and [SInner]
 * if it matches a certain name pattern, where the collection of [SInner] objects is derived from each [SOuter] item
 * (i.e., a composition relationship)
 */
internal abstract class AbstractPatternRuleOuterInner<SOuter : Named, SInner : Named, T : Any>(
    /** A function that creates and configures the target object on a successful name match. */
    private val targetCreator: (name: String, configureAction: Action<T>) -> Unit,
    /** The container for the first type of source object. */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val outerSourceContainer: NamedDomainObjectCollection<SOuter>,
    /** The container for the second type of source object. */
    private val innerSourceContainerFunction: (SOuter) -> NamedDomainObjectCollection<SInner>,
    /** The pattern that is used for matching the source and target names. */
    protected val namePattern: RuleNamePattern2
) : AbstractRule() {

    constructor(
        targetContainer: NamedDomainObjectContainer<T>,
        outerSourceContainer: NamedDomainObjectCollection<SOuter>,
        innerSourceContainerFunction: (SOuter) -> NamedDomainObjectCollection<SInner>,
        namePattern: RuleNamePattern2
    ) : this(
        { name, action -> targetContainer.create(name, action) },
        outerSourceContainer, innerSourceContainerFunction, namePattern
    )


    constructor(
        targetType: Class<T>,
        targetContainer: PolymorphicDomainObjectContainer<in T>,
        outerSourceContainer: NamedDomainObjectCollection<SOuter>,
        innerSourceContainerFunction: (SOuter) -> NamedDomainObjectCollection<SInner>,
        namePattern: RuleNamePattern2
    ) : this(
        { name, action -> targetContainer.create(name, targetType, action) },
        outerSourceContainer, innerSourceContainerFunction, namePattern
    )


    override fun getDescription(): String = namePattern.toString()


    final override fun apply(targetName: String) {
        if (namePattern.matches(targetName)) {
            namePattern.findSources(targetName, outerSourceContainer, innerSourceContainerFunction)
                ?.let { (source1, source2) ->
                    targetCreator(targetName, Action { it.configureFrom(source1, source2) })
                }
        }
    }


    /**
     * Configures the target object after creation.
     *
     * @param outerSource the outer source object
     * @param innerSource the inner source object
     */
    protected abstract fun T.configureFrom(outerSource: SOuter, innerSource: SInner)
}
