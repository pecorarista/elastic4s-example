package com.pecorarista.example

import scala.io.StdIn
import scala.collection.immutable.Seq

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.akka.{ AkkaHttpClient, AkkaHttpClientSettings }

object Main extends App with Route {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val httpServerPort = 8080
  val elasticPort = 9200
  val akkaClient = AkkaHttpClient(AkkaHttpClientSettings(Seq(s"localhost:${elasticPort}")))
  val elasticClient = ElasticClient(akkaClient)
  val bindingFuture = Http().bindAndHandle(routes, "localhost", httpServerPort)

  println(s"Server online at http://localhost:${httpServerPort}/.\nPress Return to exit.")

  StdIn.readLine()

  elasticClient.close()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
    }

}
