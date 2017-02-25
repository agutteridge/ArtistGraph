package com.github.agutteridge.artistgraph

import scala.io.Source
import java.io.{FileReader, FileNotFoundException, IOException}
import org.scalatra._
import spray.json._

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
  val wconfig = Utils.getConfig

  get("/") {
    contentType="text/html"
    jade("/index")
  }

  get("/login") { 
    val spotifyURLSeed = "https://accounts.spotify.com/authorize?" 
    def uuid = java.util.UUID.randomUUID.toString
    servletContext += ("state" -> uuid)
    val params = Map(
      "response_type" -> "code",
      "client_id" -> wconfig.client_id,
      "scope" -> "user-library-read",
      "redirect_uri" -> wconfig.redirect_uri,
      "state" -> uuid
    )

    halt(
      status = 301,
      headers = Map("Location" -> spotifyURLSeed),
      body = params
    )
  }

  get("/callback") {
    val requestState = request.cookies.getOrElse("state", None)
    if (!requestState || requestState != servletContext.getOrElse("state", None)) {
      ArtistGraphStack.notFound
    } else {
      // do all the stuff 
    }
  }
}
