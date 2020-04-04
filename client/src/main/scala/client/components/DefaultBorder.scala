package client.components

import client.renderer.main.JSComponent
import client.renderer.scalaElems.d
import client.renderer.main

case class DefaultBorder(jsComps:JSComponent*) extends JSComponent {
    def render: main.ScalaElem = {
        d(className = "borderHollow",
            style = "padding:10px;display:inline-block")
            .appendSeq(
                jsComps
            )
    }
}
