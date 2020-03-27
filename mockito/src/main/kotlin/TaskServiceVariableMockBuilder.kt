package io.holunda.camunda.bpm.data.mockito

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.holunda.camunda.bpm.data.factory.VariableFactory
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.variable.VariableMap
import org.camunda.bpm.engine.variable.Variables.createVariables
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString

/**
 * Builder to mock the task service behavior regarding variables.
 */
class TaskServiceVariableMockBuilder(
    private val taskService: TaskService,
    private val variables: VariableMap = createVariables(),
    private val localVariables: VariableMap = createVariables(),
    private val factories: MutableList<VariableFactory<*>> = mutableListOf()
) {

    /**
     * Defines a global variable.
     * @param variableFactory variable to define.
     * @return fluent builder.
     */
    fun <T> define(variableFactory: VariableFactory<T>): TaskServiceVariableMockBuilder {
        factories.add(variableFactory)
        return this
    }

    /**
     * Defines a variable and sets a global value.
     * @param variableFactory factory to use.
     * @param value initial value.
     * @return fluent builder.
     */
    fun <T> set(variableFactory: VariableFactory<T>, value: T): TaskServiceVariableMockBuilder {
        define(variableFactory)
        variableFactory.on(variables).set(value)
        return this
    }

    /**
     * Defines a variable and sets a local value.
     * @param variableFactory factory to use.
     * @param value initial value.
     * @return fluent builder.
     */
    fun <T> setLocal(variableFactory: VariableFactory<T>, value: T): TaskServiceVariableMockBuilder {
        factories.add(variableFactory)
        variableFactory.on(localVariables).set(value)
        return this
    }

    /**
     * Performs the modifications on the task service.
     */
    fun build() {

        factories.forEach { factory ->

            // global
            doAnswer {
                factory.from(variables).get()
            }.whenever(taskService).getVariable(anyString(), eq(factory.name))

            doAnswer { invocation ->
                // Arguments: 0: taskId, 1: variable name, 2: value
                val value = invocation.getArgument<Any>(2)
                variables.set(factory.name, value)
                // FIXME: does this work?
                // factory.on(variables).set(value)
                Unit
            }.whenever(taskService).setVariable(anyString(), eq(factory.name), any())

            // local
            doAnswer {
                factory.from(localVariables).get()
            }.whenever(taskService).getVariableLocal(anyString(), eq(factory.name))

            doAnswer { invocation ->
                // Arguments: 0: taskId, 1: variable name, 2: value
                val value = invocation.getArgument<Any>(2)
                localVariables.set(factory.name, value)
                // FIXME: does this work?
                // factory.on(variables).set(value)
                Unit
            }.whenever(taskService).setVariableLocal(anyString(), eq(factory.name), any())
        }

        doAnswer { variables }.whenever(taskService).getVariables(anyString())
        doAnswer { invocation ->
            // Arguments: 0: taskId, 1: licat of variables
            val variablesList = invocation.getArgument<List<String>>(1)
            variables.filter { variablesList.contains(it.key) }
        }.whenever(taskService).getVariables(anyString(), anyList())
        doAnswer { localVariables }.whenever(taskService).getVariablesLocal(anyString())
        doAnswer { invocation ->
            // Arguments: 0: taskId, 1: licat of variables
            val variablesList = invocation.getArgument<List<String>>(1)
            localVariables.filter { variablesList.contains(it.key) }
        }.whenever(taskService).getVariablesLocal(anyString(), anyList())
    }
}