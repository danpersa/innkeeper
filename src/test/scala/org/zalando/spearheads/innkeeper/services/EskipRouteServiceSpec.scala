package org.zalando.spearheads.innkeeper.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import org.zalando.spearheads.innkeeper.FakeDatabasePublisher
import org.zalando.spearheads.innkeeper.api.{EskipRoute, EskipRouteWrapper, Filter, NameWithStringArgs, NewRoute, NumericArg, Predicate, RegexArg, RouteChangeType, RouteName, RouteOut, StringArg, UserName}
import org.zalando.spearheads.innkeeper.dao.{PathRow, ModifiedRoute, RouteData, RouteRow, RoutesRepo}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import org.zalando.spearheads.innkeeper.api.JsonProtocols._
import spray.json.pimpAny

class EskipRouteServiceSpec extends FunSpec with Matchers with MockFactory with ScalaFutures {

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val routesRepo = mock[RoutesRepo]
  val routeToEskipTransformer = mock[RouteToEskipTransformer]
  val eskipRouteService = new EskipRouteService(routesRepo, routeToEskipTransformer)

  describe("route to eskip") {

    describe("#currentEskipRoutes") {
      describe("when the common filters are enabled") {

        it ("should return the correct current routes") {
          val pagination = None

          (routesRepo.selectActiveRoutesData _)
            .expects(currentTime, pagination)
            .returning(FakeDatabasePublisher(Seq(routeData)))

          (routeToEskipTransformer.transform _)
            .expects(routeData)
            .returning(eskipRoute)

          val result = eskipRouteService.currentEskipRoutes(currentTime, pagination)
            .runWith(Sink.head).futureValue

          verifyRoute(result)
        }
      }

      describe("when the common filters are not enabled") {
        it ("should return the correct current routes") {
          val pagination = None

          (routesRepo.selectActiveRoutesData _)
            .expects(currentTime, pagination)
            .returning(FakeDatabasePublisher(Seq(routeData.copy(usesCommonFilters = false))))

          val eskipRouteWithoutCommonFilters =
            eskipRoute.copy(
              prependedFilters = Seq(),
              appendedFilters = Seq())

          (routeToEskipTransformer.transform _)
            .expects(routeData.copy(usesCommonFilters = false))
            .returning(eskipRouteWithoutCommonFilters)

          val result = eskipRouteService.currentEskipRoutes(currentTime, pagination)
            .runWith(Sink.head).futureValue

          result.name should be(RouteName(routeName))
          result.eskip should
            be("""myRoute: somePredicate("Hello",123) && somePredicate1(/^Hello$/,123)
                                   | -> someFilter("Hello",123)
                                   | -> someFilter1(/^Hello$/,123)
                                   | -> "endpoint.my.com"""".stripMargin)

          result.timestamp should be(referenceTime)
        }
      }
    }
  }

  describe("#findModifiedSince") {
    val emptyEskipRoute = EskipRoute(
      name = "",
      predicates = Seq.empty,
      filters = Seq.empty,
      prependedFilters = Seq.empty,
      appendedFilters = Seq.empty,
      endpoint = ""
    )

    it("should find a route") {
      (routesRepo.selectModifiedSince _).expects(referenceTime, currentTime).returning {
        FakeDatabasePublisher(Seq(ModifiedRoute(
          routeChangeType = RouteChangeType.Create,
          name = routeData.name,
          timestamp = referenceTime,
          routeData = Some(routeData)
        )))
      }

      (routeToEskipTransformer.transform _)
        .expects(routeData)
        .returning(emptyEskipRoute)

      val result = eskipRouteService.findModifiedSince(referenceTime, currentTime).runWith(Sink.head).futureValue
      result.routeChangeType should be (RouteChangeType.Create)
    }

    it("should find a deleted route") {
      (routesRepo.selectModifiedSince _).expects(referenceTime, currentTime).returning {
        FakeDatabasePublisher(Seq(ModifiedRoute(
          routeChangeType = RouteChangeType.Delete,
          name = routeData.name,
          timestamp = referenceTime,
          routeData = None
        )))
      }

      val result = eskipRouteService.findModifiedSince(referenceTime, currentTime).runWith(Sink.head).futureValue
      result.routeChangeType should be (RouteChangeType.Delete)
    }

  }

  private def verifyRoute(result: EskipRouteWrapper) = {
    result.name should be(RouteName(routeName))
    result.eskip should
      be(

        """myRoute: somePredicate("Hello",123) && somePredicate1(/^Hello$/,123)
          | -> prependedFirst("hello")
          | -> prependedSecond(1.5)
          | -> someFilter("Hello",123)
          | -> someFilter1(/^Hello$/,123)
          | -> appendedFirst()
          | -> appendedSecond(0.8)
          | -> "endpoint.my.com"""".stripMargin)

    result.timestamp should be(referenceTime)
  }

  val referenceTime = LocalDateTime.of(2015, 10, 10, 10, 10, 10)
  val currentTime = referenceTime.plusDays(1)

  // route
  val routeName = "myRoute"
  // path
  val pathUri = "/the-uri"
  val hostIds = Seq(1L, 2L, 3L)
  val pathId = 6L

  val newRoute = NewRoute(
    predicates = Some(Seq(
      Predicate("somePredicate", Seq(StringArg("Hello"), NumericArg("123"))),
      Predicate("somePredicate1", Seq(RegexArg("Hello"), NumericArg("123"))))),
    filters = Some(Seq(
      Filter("someFilter", Seq(StringArg("Hello"), NumericArg("123"))),
      Filter("someFilter1", Seq(RegexArg("Hello"), NumericArg("123")))))
  )

  val routeRow = RouteRow(
    id = Some(1L),
    pathId = pathId,
    name = routeName,
    routeJson = newRoute.toJson.compactPrint,
    activateAt = referenceTime,
    usesCommonFilters = true,
    createdBy = "user",
    createdAt = referenceTime,
    updatedAt = referenceTime,
    disableAt = None,
    description = None,
    hostIds = None
  )

  val pathRow = PathRow(
    id = Some(pathId),
    uri = pathUri,
    hostIds = hostIds,
    ownedByTeam = "team",
    createdBy = "user",
    createdAt = referenceTime,
    updatedAt = referenceTime,
    hasStar = false,
    isRegex = false
  )

  val routeData = RouteData(
    name = routeName,
    uri = pathUri,
    hostIds = hostIds,
    hasStar = false,
    isRegex = false,
    routeJson = newRoute.toJson.compactPrint,
    usesCommonFilters = true,
    activateAt = referenceTime,
    disableAt = None
  )

  val routeOut = RouteOut(
    1,
    1L,
    RouteName(routeName),
    newRoute,
    referenceTime,
    activateAt = LocalDateTime.of(2015, 10, 10, 10, 10, 11),
    UserName("user"),
    usesCommonFilters = false,
    disableAt = Some(LocalDateTime.of(2015, 11, 11, 11, 11, 11)),
    description = None,
    hostIds = None,
    hosts = None,
    path = None
  )

  val eskipRoute = EskipRoute(
    name = routeName,
    predicates = Seq(
      NameWithStringArgs("somePredicate", Seq(""""Hello"""", "123")),
      NameWithStringArgs("somePredicate1", Seq("/^Hello$/", "123"))),
    filters = Seq(
      NameWithStringArgs("someFilter", Seq(""""Hello"""", "123")),
      NameWithStringArgs("someFilter1", Seq("/^Hello$/", "123"))
    ),
    prependedFilters = Seq("""prependedFirst("hello")""", "prependedSecond(1.5)"),
    appendedFilters = Seq("appendedFirst()", "appendedSecond(0.8)"),
    endpoint = "\"endpoint.my.com\"")
}

