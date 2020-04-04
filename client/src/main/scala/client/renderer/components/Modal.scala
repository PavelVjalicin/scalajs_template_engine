package client.renderer.components

import client.jsnative.$
import org.scalajs.dom.Element
import client.renderer.main
import client.renderer.main._
import client.renderer.scalaElems._

import scala.scalajs.js.timers.setTimeout

case class Modal(parent:Element,title:String,body:JSComponent,submitBtnText:String,onContinue:()=>Unit) extends JSComponent {

    def remove:Unit = {
        $(elem).modal("hide")
        setTimeout(1000) {
            $(elem).remove()
        }

    }

    def show = {
        renderTrue(parent)
        $(elem).modal("show")
        $(elem).on("hidden.bs.modal", remove _)
    }

    def onContinueRemove = {
        onContinue()
        remove
    }

    def render: main.ScalaElem = {

        val modalBody = d(className="modal-body") (
            body
        )

        val footer = d(className = "modal-footer") (
            SecondaryButton("Cancel",remove _),
                PrimaryButton(submitBtnText,onContinueRemove _,btnType = "button")
        )

        d(className = "modal fade") {
            d(className = "modal-dialog") (
                d(className="modal-content")(
                    d(className="modal-header")(
                        {
                            span(style="cursor:pointer;font-size:25px;",className = "close fa fa-times",onClick = Some(remove _))
                        },
                        h(3,className = "modal-title") {title}

                    ),
                    modalBody,
                    footer
                )
            )
        }
    }
}
