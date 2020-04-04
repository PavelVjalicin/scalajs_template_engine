package controllers

import com.google.inject.Inject
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import play.filters.csrf.CSRF
import shared.{Note, Notes}
import upickle.default._

import scala.collection.mutable



class HomeController @Inject() (cc:ControllerComponents,parse:PlayBodyParsers) extends AbstractApiController(cc) {
    var idCounter = 1

    val notes:mutable.Map[Long,Note] = mutable.Map()

    def index = Action { req =>
        Ok(views.html.index(CSRF.getToken(req).get.value))
    }

    def postNote = Action { implicit req =>

        postResponse(shared.jsRoutes.post.note) { note =>
            notes += ((idCounter,note.copy(id=Some(idCounter))))
            idCounter = idCounter + 1
            Created
        }
    }

    def getNotes = Action { req =>
        getResponse(shared.jsRoutes.get.notes,req) { _ =>
            Some(
                Notes(notes.values.toSeq)
            )
        }
    }

    def deleteNote = Action { req =>
        deleteResponse(shared.jsRoutes.delete.note,req)( id => {
            notes -= id.id
            Ok
        })
    }
}
