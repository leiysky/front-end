/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.ast.UnaliasedReturnItem
import org.opencypher.v9_0.ast.semantics.{SemanticCheckResult, SemanticChecker, SemanticFeature, SemanticState, SemanticTable}
import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.SEMANTIC_CHECK
import org.opencypher.v9_0.rewriting.conditions.containsNoNodesOfType

case class SemanticAnalysis(warn: Boolean, features: SemanticFeature*)
  extends Phase[BaseContext, BaseState, BaseState] {

  override def process(from: BaseState, context: BaseContext): BaseState = {
    val startState = {
      if (from.initialFields.nonEmpty)
        SemanticState.withStartingVariables(from.initialFields.toSeq: _*)
      else
        SemanticState.clean
    }.withFeatures(features: _*)

    val SemanticCheckResult(state, errors) = SemanticChecker.check(from.statement(), startState)
    if (warn) state.notifications.foreach(context.notificationLogger.log)

    context.errorHandler(errors)

    val table = SemanticTable(types = state.typeTable, recordedScopes = state.recordedScopes)
    from.withSemanticState(state).withSemanticTable(table)
  }

  override def phase: CompilationPhaseTracer.CompilationPhase = SEMANTIC_CHECK

  override def description = "do variable binding, typing, type checking and other semantic checks"

  override def postConditions = Set(BaseContains[SemanticState], StatementCondition(containsNoNodesOfType[UnaliasedReturnItem]))
}
