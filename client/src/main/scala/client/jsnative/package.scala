package client

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

package object jsnative {

    @js.native
    @JSGlobal
    object $ extends js.Object {
        def apply(str:String):ScalaJQ = js.native
        def apply(element:Element):ScalaJQ = js.native

    }

    @js.native
    @JSGlobal
    object App extends js.Object {
        def addButtonHandlers(scalaJQ: ScalaJQ):js.Any = js.native
        def t:String = js.native
        def sessionExpired():Unit = js.native
    }

    @js.native
    @JSGlobal
    object eval extends js.Object {
        def apply(str:String):Unit = js.native
    }

}

@js.native
@JSGlobal
class ScalaJQ extends js.Object {
    def modal(string: String):ScalaJQ = js.native
    def remove(): Unit = js.native
    def on(arg:String,func:js.Function0[Unit]):Any = js.native
}