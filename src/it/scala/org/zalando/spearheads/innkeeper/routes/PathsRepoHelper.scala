package org.zalando.spearheads.innkeeper.routes

import java.time.LocalDateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.zalando.spearheads.innkeeper.dao.PathRow
import scala.collection.immutable.Seq

object PathsRepoHelper extends ScalaFutures with DaoHelper {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  def insertPath(pathRow: PathRow = samplePath()): PathRow = {

    pathsRepo.insert(PathRow(
      id = None,
      uri = pathRow.uri,
      hostIds = pathRow.hostIds,
      ownedByTeam = pathRow.ownedByTeam,
      createdBy = pathRow.createdBy,
      createdAt = pathRow.createdAt,
      updatedAt = pathRow.updatedAt,
      hasStar = pathRow.hasStar,
      isRegex = pathRow.isRegex
    )).futureValue
  }

  def samplePath(
    id: Long = 1,
    uri: String = "/uri",
    hostIds: Seq[Long] = Seq(1L, 2L, 3L),
    createdBy: String = "testuser",
    ownedByTeam: String = "testteam",
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    activateAt: LocalDateTime = LocalDateTime.now().minusHours(2),
    hasStar: Boolean = false,
    isRegex: Boolean = false) = {
    PathRow(
      id = Some(id),
      uri = uri,
      hostIds = hostIds,
      ownedByTeam = ownedByTeam,
      createdBy = createdBy,
      createdAt = createdAt,
      updatedAt = updatedAt,
      hasStar = hasStar,
      isRegex = isRegex
    )
  }
}
