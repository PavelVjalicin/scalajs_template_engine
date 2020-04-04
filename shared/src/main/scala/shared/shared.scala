package shared
import upickle.default._

object Note {
    implicit val f:ReadWriter[Note] = macroRW
}
case class Note(title:String,text:String,id:Option[Long] = None)

object Notes {
    implicit val f:ReadWriter[Notes] = macroRW
}
case class Notes(seq:Seq[Note])

object jsComponentsExports{
}

case class GETRoute[Takes,+Returns](url:URL)

case class POSTRoute[T](url:URL) {
    type takes = T
}

case class DELETERoute[T](url:URL) {
    type takes = T
}

object IDInput {
    implicit val f:ReadWriter[IDInput] = macroRW
}
case class IDInput(id:Long,seq:Seq[Long]=Seq(),optID:Option[Long]=Some(1)) extends RouteParam {
    def paramSeq: Seq[(String, Any)] = Seq(("id",id))
}

object jsRoutes {
    val prefix = "api/"
    object paths {
        val note = URL("note")
    }
    object get {
        def notes = GETRoute[Null, Notes](paths.note)
    }

    object post {
        def note = POSTRoute[Note](paths.note)
    }

    object delete {
        def note = DELETERoute[IDInput](paths.note)
    }

}
case class URL(path:String)

trait RouteParam {
    def paramSeq:Seq[(String,Any)]
}