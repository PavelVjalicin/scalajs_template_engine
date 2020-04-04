package client.components

import client.renderer.components.inputs.{Form, InlineInput, Input, RWInput}
import client.renderer.main.{JSComponent, JSComponentWithState}
import client.renderer.scalaElems.{PrimaryButton, SecondaryButton, d}
import client.renderer.main
import shared.POSTRoute
import upickle.default._

case class DefaultRWForm[T](formName:String,
                            userCanEdit:Boolean,
                            action:POSTRoute[T],
                            onSuccess:(()=>Unit)=>Unit,
                            inputs:Seq[()=>InlineInput],
                            isWritable:Boolean = false)
                           (outputFunction: Seq[JSComponent with Input] =>T,errorValidations:Seq[Seq[JSComponent with Input] =>Option[String]] = Seq())
                           (formFunction: Seq[JSComponent with Input] => JSComponent)
                           (implicit rw:ReadWriter[T])
    extends JSComponentWithState(isWritable) {
    def newOnSuccess(btnLoad:()=>Unit) = {
        onSuccess(()=>reRender(false))
        btnLoad()
    }
    def render: main.ScalaElem = {

        val i:Seq[JSComponent with Input] = inputs.map(_()).map(i=>RWInput(i,state))

        d() {
            if(state) {
                val form = Form(action, newOnSuccess)(i, "Save") (
                    outputFunction,errorValidations
                ) { (i, btn,errorField) =>
                    DefaultBorder(
                        DefaultTitle(formName),
                        formFunction(i),
                        btn,
                        SecondaryButton("Cancel", () => {
                            reRender(false)
                        }, style = "float:right"),
                        errorField
                    )
                }


                form

            } else {
                val editButton = if(userCanEdit) { PrimaryButton("Edit", () => reRender(true))} else d()
                DefaultBorder(
                    DefaultTitle(formName),
                    formFunction(i),
                    editButton
                )
            }
        }
    }
}
