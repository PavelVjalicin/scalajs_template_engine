package client.components

import client.renderer.components.inputs.{Form, Input}
import client.renderer.main.{JSComponent, JSComponentWithState, ScalaElem}
import client.renderer.scalaElems._
import shared.POSTRoute
import upickle.default._

case class AddButtonForm[T](formName:String,
                            formTitle:Option[String] = None,
                            action:POSTRoute[T],
                            onSuccess:(()=>Unit)=>Unit,
                            inputs:Seq[()=>JSComponent with Input],
                            onOpen:Option[()=>Unit] = None)
                           (outputFunction: Seq[JSComponent with Input] =>T,errorValidations:Seq[Seq[JSComponent with Input] => Option[String]] = Seq())
                           (formFunction: Seq[JSComponent with Input] =>JSComponent)
                           (implicit rw:ReadWriter[T]) extends JSComponentWithState(false) {

    def newOnSuccess(btnLoad:()=>Unit) = {
        onSuccess(()=>reRender(false))
        btnLoad()
    }

    def render: ScalaElem = {

        def button = Button(formName,
            () => {
                onOpen.foreach( x=> x())
                reRender(true)
            },
            className = "btn btn-primary",
            style="border-radius:0 0 5px 0",
            btnType = "button"
        )
        span()(
            if(!state) button
            else {

                val i = inputs.map(_())
                Form(action, newOnSuccess)(i, "Submit") (
                    outputFunction,
                    errorValidations
                ) { (i,btn,errorField)=> {
                    DefaultBorder(
                        DefaultTitle(formTitle.getOrElse(formName)),
                        formFunction(i),
                        btn,
                        SecondaryButton("Cancel", () => {
                            reRender(false)
                        }, style = "float:right"),
                        errorField
                    )
                }
                }

            }
        )
    }
}