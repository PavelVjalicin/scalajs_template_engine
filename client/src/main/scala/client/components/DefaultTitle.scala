package client.components

import client.renderer.main.JSComponent
import client.renderer.scalaElems.h
import client.renderer.main

case class DefaultTitle(text:String) extends JSComponent {
    def render: main.ScalaElem = {
        h(3, className = "columnHeader bottom-border", style = "margin-top:5px;padding:0px;") {
            text
        }
    }
}
