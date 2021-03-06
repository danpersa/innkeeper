package org.zalando.spearheads.innkeeper.routes

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecToken._
import org.zalando.spearheads.innkeeper.routes.AcceptanceSpecsHelper._
import org.zalando.spearheads.innkeeper.routes.RoutesRepoHelper._
import org.zalando.spearheads.innkeeper.api._
import spray.json._
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import scala.collection.immutable.Seq

class PostPathsSpec extends FunSpec with BeforeAndAfter with Matchers {

  private val pathUri = "uri-1"
  private val hostIds = Seq(1L, 2L)
  private val otherOwningTeam = "otherOwningTeam"
  private def pathJsonString(pathUri: String = pathUri, hostIds: Seq[Long] = hostIds) =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}]
        |}
  """.stripMargin

  private val pathWithOwningTeamJsonString =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}],
        |  "owned_by_team": "$otherOwningTeam"
        |}
  """.stripMargin

  private def pathWithHasStarString(pathUri: String, hasStar: Boolean) =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}],
        |  "has_star": $hasStar
        |}
  """.stripMargin

  private def pathWithIsRegexString(pathUri: String, isRegex: Boolean) =
    s"""{
        |  "uri": "$pathUri",
        |  "host_ids": [${hostIds.mkString(", ")}],
        |  "is_regex": $isRegex
        |}
  """.stripMargin

  describe("post /paths") {

    describe("success") {

      before {
        recreateSchema
      }

      describe("when a token with the write scope is provided") {
        it("should create the new path") {
          val token = WRITE_TOKEN
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.hasStar should be(false)
        }

        it("should create the new star path") {
          val token = WRITE_TOKEN

          val pathUri = "/api/service"
          val requestBody = pathWithHasStarString(pathUri, hasStar = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.hasStar should be(true)
        }

        it("should create the new star path bypassing the pattern check for an admin token") {
          val token = ADMIN_TOKEN

          val pathUri = "/non-api-uri"
          val requestBody = pathWithHasStarString(pathUri, hasStar = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.hasStar should be(true)
        }

        it("should create the new star path bypassing the pattern check for a token with the admin scope") {
          val token = ADMIN_TEAM_TOKEN

          val pathUri = "/non-api-uri"
          val requestBody = pathWithHasStarString(pathUri, hasStar = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.hasStar should be(true)
        }
      }

      describe("when a token with the admin scope is provided") {
        val token = ADMIN_TOKEN

        it("should create the new path with the provided owning team") {
          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(otherOwningTeam))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
        }

        it("should create the new regex path") {
          val pathUri = "/some-path"
          val requestBody = pathWithIsRegexString(pathUri, isRegex = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.isRegex should be(true)
        }
      }

      describe("when an admin team token is provided") {
        val token = ADMIN_TEAM_TOKEN

        it("should create the new path with the provided owning team") {
          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(otherOwningTeam))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
        }

        it("should create the new regex path") {
          val pathUri = "/some-path"
          val requestBody = pathWithIsRegexString(pathUri, isRegex = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.OK)
          val entity = entityString(response)
          val path = entity.parseJson.convertTo[PathOut]

          path.uri should be(pathUri)
          path.ownedByTeam should be(TeamName(token.teamName))
          path.createdBy should be(UserName(token.userName))
          path.hostIds should be(hostIds)
          path.isRegex should be(true)
        }
      }
    }

    describe("failure") {
      describe("when no token is provided") {

        it("should return the 401 Unauthorized status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), "")
          response.status should be(StatusCodes.Unauthorized)
        }
      }

      describe("when an invalid token is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), INVALID_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH3")
        }
      }

      describe("when a token without the write scope is provided") {
        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), READ_TOKEN)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("AUTH1")
        }
      }

      describe("when a token doesn't have an associated uid") {
        val token = "token--employees-route.write_strict"

        it("should return the 403 Forbidden status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("TNF")
        }
      }

      describe("when an existing paths with the same uri host ids intersects with the provided host ids") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val path = PathsRepoHelper.samplePath(
            uri = pathUri,
            hostIds = Seq(1L, 2L, 3L)
          )
          PathsRepoHelper.insertPath(path)

          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("DPU")
        }
      }

      describe("when a star path is to be saved and it doesn't match the configured patterns") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val requestBody = pathWithHasStarString("wrong-uri", hasStar = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("SPP")
        }
      }

      describe("when a path with no host ids exists for the provided uri") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val path = PathsRepoHelper.samplePath(
            uri = pathUri,
            hostIds = Seq.empty
          )
          PathsRepoHelper.insertPath(path)

          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("DPU")
        }
      }

      describe("when host ids is empty") {
        val token = WRITE_TOKEN

        it("should return the 400 Bad Request status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(hostIds = Seq.empty), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("EPH")
        }
      }

      describe("when host ids is empty for an admin token") {
        val token = ADMIN_TOKEN

        it("should return the 400 Bad Request status") {
          val response = PathsSpecsHelper.postSlashPaths(pathJsonString(hostIds = Seq.empty), token)
          response.status should be(StatusCodes.BadRequest)
          entityString(response).parseJson.convertTo[Error].errorType should be("EPH")
        }
      }

      describe("when a token without admin privileges is provided and owned_by_team is different") {
        it("should return the 403 Forbidden status") {
          val token = WRITE_TOKEN

          val response = PathsSpecsHelper.postSlashPaths(pathWithOwningTeamJsonString, token)

          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("ITE")
        }
      }

      describe("when a token without admin privileges is provided when creating a regex path") {
        it("should return the 403 Forbidden status") {
          val token = WRITE_TOKEN
          val pathUri = "/some-path"
          val requestBody = pathWithIsRegexString(pathUri, isRegex = true)
          val response = PathsSpecsHelper.postSlashPaths(requestBody, token)

          response.status should be(StatusCodes.Forbidden)
          entityString(response).parseJson.convertTo[Error].errorType should be("ITE")
        }
      }
    }
  }
}
