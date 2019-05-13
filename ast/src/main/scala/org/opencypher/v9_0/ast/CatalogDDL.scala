/*
 * Copyright © 2002-2019 Neo4j Sweden AB (http://neo4j.com)
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
package org.opencypher.v9_0.ast

import org.opencypher.v9_0.ast.semantics.SemanticCheckResult._
import org.opencypher.v9_0.ast.semantics.{SemanticAnalysisTooling, SemanticCheck, SemanticCheckResult, SemanticFeature, SemanticState}
import org.opencypher.v9_0.expressions.{Parameter, Variable}
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.symbols._


sealed trait CatalogDDL extends Statement with SemanticAnalysisTooling {

  def name: String

  override def returnColumns: List[String] = List.empty
}

sealed trait MultiDatabaseDDL extends CatalogDDL {
  override def semanticCheck: SemanticCheck =
    requireFeatureSupport(s"The `$name` clause", SemanticFeature.MultipleDatabases, position)
}

sealed trait MultiGraphDDL extends CatalogDDL {
  //TODO Refine to split between multigraph and views
  override def semanticCheck: SemanticCheck =
    requireFeatureSupport(s"The `$name` clause", SemanticFeature.MultipleGraphs, position)
}

final case class ShowUsers()(val position: InputPosition) extends MultiDatabaseDDL {

  override def name: String = "CATALOG SHOW USERS"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class CreateUser(userName: String,
                            initialStringPassword: Option[String],
                            initialParameterPassword: Option[Parameter],
                            requirePasswordChange: Boolean,
                            suspended: Boolean)(val position: InputPosition) extends MultiDatabaseDDL {
  UserNameValidator.assertValidUsername(userName)
  assert(initialStringPassword.isDefined || initialParameterPassword.isDefined)
  assert(!(initialStringPassword.isDefined && initialParameterPassword.isDefined))

  override def name = "CATALOG CREATE USER"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class DropUser(userName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG DROP USER"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class AlterUser(userName: String,
                           initialStringPassword: Option[String],
                           initialParameterPassword: Option[Parameter],
                           requirePasswordChange: Option[Boolean],
                           suspended: Option[Boolean])(val position: InputPosition) extends MultiDatabaseDDL {
  UserNameValidator.assertValidUsername(userName)
  assert(initialStringPassword.isDefined || initialParameterPassword.isDefined || requirePasswordChange.isDefined || suspended.isDefined)
  assert(!(initialStringPassword.isDefined && initialParameterPassword.isDefined))

  override def name = "CATALOG ALTER USER"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class ShowRoles(withUsers: Boolean, showAll: Boolean)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name: String = if (showAll) "CATALOG SHOW ALL ROLES" else "CATALOG SHOW POPULATED ROLES"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class CreateRole(roleName: String, from: Option[String])(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG CREATE ROLE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class DropRole(roleName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG DROP ROLE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class ShowDatabases()(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG SHOW DATABASES"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class ShowDatabase(dbName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG SHOW DATABASE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class CreateDatabase(dbName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG CREATE DATABASE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class DropDatabase(dbName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG DROP DATABASE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class StartDatabase(dbName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG START DATABASE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

final case class StopDatabase(dbName: String)(val position: InputPosition) extends MultiDatabaseDDL {

  override def name = "CATALOG STOP DATABASE"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

object CreateGraph {
  def apply(graphName: CatalogName, query: Query)(position: InputPosition): CreateGraph =
    CreateGraph(graphName, query.part)(position)
}

final case class CreateGraph(graphName: CatalogName, query: QueryPart)
  (val position: InputPosition) extends MultiGraphDDL {

  override def name = "CATALOG CREATE GRAPH"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this) chain
      query.semanticCheck
}

final case class DropGraph(graphName: CatalogName)(val position: InputPosition) extends MultiGraphDDL {

  override def name = "CATALOG DROP GRAPH"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}

object CreateView {
  def apply(graphName: CatalogName, params: Seq[Parameter], query: Query, innerQString: String)(position: InputPosition): CreateView =
    CreateView(graphName, params, query.part, innerQString)(position)
}

final case class CreateView(graphName: CatalogName, params: Seq[Parameter], query: QueryPart, innerQString: String)
  (val position: InputPosition) extends MultiGraphDDL {

  override def name = "CATALOG CREATE VIEW/QUERY"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this) chain
      recordGraphParameters chain
      query.semanticCheck

  private def recordGraphParameters(state: SemanticState): SemanticCheckResult = {
    params.foldLeft(success(state): SemanticCheckResult) { case (SemanticCheckResult(s, errors), p) =>
      s.declareVariable(Variable(s"$$${p.name}")(position), CTGraphRef) match {
        case Right(updatedState) => success(updatedState)
        case Left(semanticError) => SemanticCheckResult(s, errors :+ semanticError)
      }
    }
  }

}

final case class DropView(graphName: CatalogName)(val position: InputPosition) extends MultiGraphDDL {

  override def name = "CATALOG DROP VIEW/QUERY"

  override def semanticCheck: SemanticCheck =
    super.semanticCheck chain
      SemanticState.recordCurrentScope(this)
}
