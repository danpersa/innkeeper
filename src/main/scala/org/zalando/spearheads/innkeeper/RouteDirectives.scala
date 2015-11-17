package org.zalando.spearheads.innkeeper

import akka.http.scaladsl.server.directives.BasicDirectives.pass
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{ Directive0, AuthorizationFailedRejection }
import org.zalando.spearheads.innkeeper.api._

/**
 * @author dpersa
 */
trait RouteDirectives {

  def isFullTextRoute(route: NewRoute): Directive0 = {
    route.matcher.pathMatcher match {
      case Some(StrictPathMatcher(_)) => pass
      case _                          => reject(AuthorizationFailedRejection)
    }
  }

  def isRegexRoute(route: NewRoute): Directive0 = {
    route.matcher.pathMatcher match {
      case Some(RegexPathMatcher(_)) => pass
      case _                         => reject(AuthorizationFailedRejection)
    }
  }
}

object RouteDirectives extends RouteDirectives
