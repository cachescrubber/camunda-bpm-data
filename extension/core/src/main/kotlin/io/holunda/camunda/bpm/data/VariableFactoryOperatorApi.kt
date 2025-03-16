package io.holunda.camunda.bpm.data

import io.holunda.camunda.bpm.data.factory.VariableFactory
import org.operaton.bpm.engine.delegate.VariableScope
import org.operaton.bpm.engine.variable.VariableMap

/**
 * Operator getter from global scope.
 * @param factory factory defining the variable.
 */
operator fun <T> VariableMap.get(factory: VariableFactory<T>): T = factory.from(this).get()

/**
 * Operator getter from global scope.
 * @param factory factory defining the variable.
 */
operator fun <T> VariableScope.get(factory: VariableFactory<T>): T = factory.from(this).get()

