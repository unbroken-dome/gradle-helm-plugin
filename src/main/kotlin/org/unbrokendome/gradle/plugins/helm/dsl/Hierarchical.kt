package org.unbrokendome.gradle.plugins.helm.dsl


/**
 * Implemented by configuration blocks inside tasks or other DSL elements that can
 * inherit properties from a parent.
 */
internal interface Hierarchical<T : Any> {

    /**
     * Sets the parent of this element. This causes all contained properties
     * to be set to track the corresponding properties of the parent.
     */
    fun setParent(parent: T)
}
