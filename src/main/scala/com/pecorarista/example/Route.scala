package com.pecorarista.example

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.immutable.Seq

import akka.http.scaladsl.server.Directives._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{ get => _, _ }
import com.sksamuel.elastic4s.{ RequestFailure, RequestSuccess }
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import ApplicationTypes._
import JsonProtocol._
import com.sksamuel.elastic4s.sprayjson._

trait Route {

  implicit val elasticClient: ElasticClient
  implicit val executionContext: ExecutionContext

  val documentsIndexName = "documents"

  val routes = cors() {
    path(documentsIndexName) {
      get {
        parameter('term.?) { term =>
          complete(findDocuments(term))
        }
      } ~
        post {
          entity(as[UpdateDocumentRequest]) { request =>
            val message: Future[String] = updateDocument(request).map(_.getOrElse("Success!"))
            complete(message)
          }
        }
    }
  }

  private def findDocuments(term: Option[String]): Future[Seq[Document]] = {
    val queryBody = term match {
      case Some(s) => matchQuery("content", s)
      case None => matchAllQuery()
    }
    elasticClient
      .execute(
        search(documentsIndexName).limit(20).query(queryBody)
      )
      .map { response =>
        response match {
          case RequestSuccess(status @ _, body @ _, headers @ _, result) =>
            result.hits.hits.to[Seq].map(_.to[Document])
          case _ => Seq.empty[Document]
        }
      }
  }

  private def updateDocument(request: UpdateDocumentRequest): Future[Option[String]] =
    elasticClient
      .execute {
        update(request.uri)
          .in(documentsIndexName)
          .doc(UpdateDocumentRequest(request.uri, request.title, request.retrieved, request.content))
      }
      .map { response =>
        response match {
          case RequestFailure(status @ _, body @ _, headers @ _, error) => Some(error.reason)
          case _ => None
        }
      }

}
