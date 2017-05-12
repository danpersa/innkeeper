package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.dao.{PathRow, RouteRow}
import scala.collection.immutable.Seq

object RoutesRepoHelper extends DaoHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  def insertRoute(
    name: String = "THE_ROUTE",
    description: String = "desc",
    method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    disableAt: Option[LocalDateTime] = None,
    activateAt: LocalDateTime = LocalDateTime.now().minusHours(2),
    usesCommonFilters: Boolean = false,
    pathId: Option[Long] = None,
    pathHostIds: Seq[Long] = Seq.empty[Long],
    routeHostIds: Option[Seq[Long]] = None): RouteRow = {

    val resolvedPathId = pathId.getOrElse {
      insertTestPath(ownedByTeam, createdBy, createdAt, s"/path-for-$name", pathHostIds)
    }

    routesRepo.insert(RouteRow(
      pathId = resolvedPathId,
      name = name,
      routeJson = routeJson(method),
      createdBy = createdBy,
      createdAt = createdAt,
      updatedAt = createdAt,
      activateAt = activateAt,
      disableAt = disableAt,
      usesCommonFilters = usesCommonFilters,
      description = Some(description),
      hostIds = routeHostIds
    )).futureValue
  }

  def sampleRoute(
    id: Long = 0L,
    pathId: Long = 0L,
    name: String = "THE_ROUTE",
    method: String = "GET",
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now(),
    usesCommonFilters: Boolean = false,
    description: Option[String] = Some("desc")): RouteRow = {

    RouteRow(
      id = Some(id),
      pathId = pathId,
      name = name,
      routeJson = routeJson(method),
      createdBy = createdBy,
      createdAt = createdAt,
      updatedAt = createdAt,
      activateAt = activateAt,
      usesCommonFilters = usesCommonFilters,
      description = description,
      disableAt = None,
      hostIds = None
    )
  }

  def deleteRoute(id: Long, dateTime: Option[LocalDateTime] = None): Boolean = {
    routesRepo.delete(id, None, dateTime).futureValue
  }

  def routeJson(method: String = "POST"): String =
    s"""{
        |  "predicates": [{
        |      "name": "method",
        |      "args": [{
        |        "value": "$method",
        |        "type": "string"
        |      }]
        |  }],
        |  "filters": [{
        |      "name": "someFilter",
        |      "args": [{
        |        "value": "HelloFilter",
        |        "type": "string"
        |      }, {
        |        "value": "123",
        |        "type": "number"
        |      }, {
        |        "value": "0.99",
        |        "type": "number"
        |      }]
        |  }]
        |}""".stripMargin

  private def insertTestPath(
    ownedByTeam: String,
    createdBy: String,
    createdAt: LocalDateTime,
    uri: String,
    hostIds: Seq[Long]): Long = {

    val path = pathsRepo.insert(PathRow(
      id = None,
      uri = uri,
      hostIds = hostIds,
      ownedByTeam = ownedByTeam,
      createdAt = createdAt,
      updatedAt = createdAt,
      createdBy = createdBy,
      hasStar = false,
      isRegex = false
    )).futureValue

    path.id.get
  }
}
