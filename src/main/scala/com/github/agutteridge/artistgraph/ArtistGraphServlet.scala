package com.github.agutteridge.artistgraph

import scala.io.Source
import java.io.{FileReader, FileNotFoundException, IOException}
import org.scalatra._
import spray.json._
import org.slf4j.{Logger, LoggerFactory}

case class WebappConfig(client_id: String, client_secret: String, redirect_uri: String)

object ConfigProtocol extends DefaultJsonProtocol {
  implicit val accessFormat = jsonFormat3(WebappConfig)
}

object Utils {
  def getConfig: WebappConfig = {
    import ConfigProtocol._
    val filename = "config.json"
    var jsonStr: String = ""

    try {
      jsonStr = Source.fromFile(filename).getLines.mkString
    } catch {
      case ex: FileNotFoundException => println("Couldn't find that file.")
      case ex: IOException => println("Had an IOException trying to read that file")
    }

    val json = jsonStr.parseJson
    json.convertTo[WebappConfig]
  }
}

class ArtistGraphServlet extends ArtistGraphStack {
  val logger =  LoggerFactory.getLogger("com.github.agutteridge.artistgraph.ArtistGraphServlet")
  val wconfig = Utils.getConfig

  get("/") {
    logger.info("Index accessed.")
    contentType="text/html"
    jade("/index")
  }

  get("/login") { 
    logger.info("Login page accessed.")
    def uuid = java.util.UUID.randomUUID.toString
    servletContext += ("state" -> uuid)
    val spotifyURLSeed = "https://accounts.spotify.com/authorize?"
    val params = Map(
      "response_type" -> "token",
      "client_id" -> wconfig.client_id,
      "scope" -> "user-library-read",
      "redirect_uri" -> wconfig.redirect_uri,
      "state" -> uuid
    )
    val finalURL = spotifyURLSeed + params.toList.map(p => s"${p._1}=${p._2}").mkString("&")

    halt(
      status = 301,
      headers = Map("Location" -> finalURL)
    )
  }

  get("/callback") {
    logger.info("in callback")
    logger.info(request.getRequestURL.toString)
    val requestState = servletContext.getOrElse("state", None)
    val responseState = params("state")
    logger.info(s"requestState: $requestState")
    logger.info(s"responseState: $responseState")

    if (responseState == None || requestState != responseState) {
      logger.info("Not found.")
      notFound
    } else {
      servletContext -= "state"
      val at = params("access_token")

      halt(
        headers = Map(
          "Location" -> "https://api.spotify.com/v1/me/albums",
          "Authorization" -> s"Bearer $at"
        ),
        body = Map("limit" -> 50)
      )
    }
  }
}
