package shared
import upickle.default._


object jsComponentsExports{
}

case class GETRoute[Takes,+Returns](url:URL)

case class POSTRoute[T](url:URL) {
    type takes = T
}

case class DELETERoute[T](url:URL) {
    type takes = T
}


object jsRoutes {
    val prefix = "api/"
}
case class URL(path:String)

trait RouteParam {
    def paramSeq:Seq[(String,Any)]
}