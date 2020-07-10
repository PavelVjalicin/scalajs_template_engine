package client.renderer.components

import org.scalajs.dom.raw.{HTMLElement, KeyboardEvent}
import org.scalajs.dom.{Event, html}
import client.renderer.main.{JSComponent, JSComponentWithState, ScalaElem}
import client.renderer.scalaElems._
import client.renderer.{main, utils}
import shared.POSTRoute
import upickle.default._
import scala.scalajs.js.annotation.JSExportTopLevel

object inputs {
    case class FormError() extends JSComponentWithState(None:Option[String]) {
        def render: ScalaElem = {
            d() {
                if(state.isDefined) {
                    d(className = "alert alert-danger", style="margin:10px 0 0 0")(
                        ErrorMessage(state.get)
                    )
                } else {
                    d()
                }
            }
        }
    }

    case class LoadingButton(
                                text:String,
                                onClick:()=>Any,
                                btnType:String="button",
                                style:String="",
                                className:String=""
                            ) extends JSComponentWithState(false) {
        def render: ScalaElem = {
            def newOnClick() = {
                reRender(true)
                onClick()
            }
            span()(
                Button(text,() => newOnClick(),className,btnType,style),
                if(state) span(className="fa fa-cog fa-spin fa-fw", style="font-size:16px;")() else span()
            )
        }
    }

    object ToggleButton {
        def apply(text:String,value:Boolean,onChangeCallBack:Option[(Boolean)=>Unit]) = {
            new ToggleButton(text,value,onChangeCallBack)
        }
    }

    case class SToggleButton(on:Button,off:Button)

    class ToggleButton(text:String,value:Boolean,onChangeCallBack:Option[(Boolean)=>Unit]) extends JSComponent {

        val on = NormalButton("On",onChange(true),style="margin-left:5px;",className = if(value) " btn-primary" else " btn-secondary")
        val off = NormalButton("Off",onChange(false),className = if(!value) " btn-primary" else " btn-secondary")

        private def onChange(bool:Boolean):()=>Unit = {

            () => {
                if(bool) {
                    on.elem.classList.add("btn-primary")
                    on.elem.classList.remove("btn-secondary")
                    off.elem.classList.add("btn-secondary")
                    off.elem.classList.remove("btn-primary")
                } else {
                    on.elem.classList.remove("btn-primary")
                    on.elem.classList.add("btn-secondary")
                    off.elem.classList.remove("btn-secondary")
                    off.elem.classList.add("btn-primary")
                }
                onChangeCallBack.foreach(_(bool))
            }
        }



        def render: ScalaElem = {

            d(className = "scala buttonTogglePair",
                text=text,
                style = "padding-left:0px;")(
                on,
                off
            )

        }
    }


    case class Form[T](action:POSTRoute[T],onSuccess:(()=>Unit)=>Unit,confirmForm:Boolean = true,autoComplete:Boolean=true)
                      (inputs:Seq[JSComponent with Input],sbmtButtonName:String)
                      (outputFunction: Seq[JSComponent with Input] =>T,errorValidations:Seq[Seq[JSComponent with Input] => Option[String]] = Seq())
                      (formFunction:(Seq[JSComponent with Input],LoadingButton,FormError)=>JSComponent)
                      (implicit rw:ReadWriter[T]) extends JSComponent {

        def onError(responseText:String):Unit = {

            val response = if(responseText!="") responseText else "Something went wrong"

            formError.reRender(Some(response))
            submitButton.reRender(false)
        }

        def onSubmit: () => Unit = { () =>

            inputs.foreach({ x=>
                x.cancelError()
            })

            val errorInputs = checkRequired(inputs)

            val customErrors = errorValidations.flatMap( ev => ev(inputs))

            formError.reRender(None)

            val notValidInputs = inputs.filter(_.isValid.isDefined)

            if(customErrors.isEmpty) {
                if(errorInputs.isEmpty) {
                    if(notValidInputs.isEmpty) {
                        val outputObject = outputFunction(inputs)

                        utils.requests.post(action,outputObject) {
                            () => onSuccess(
                                () => submitButton.reRender(false)
                            )
                        } {
                            onError
                        }
                    } else {
                        if(confirmForm) {
                            notValidInputs.foreach(x => x.errorTrigger(x.isValid))
                        }
                    }
                } else {
                    if(confirmForm) {
                        errorInputs.foreach(_.errorTrigger(None))
                    }
                }
            } else {
                submitButton.reRender(false)
                formError.reRender(Some(customErrors.head))
            }
        }

        def checkRequired(inputs:Seq[JSComponent with Input]): Seq[JSComponent with Input] = {
            val requiredInputs = inputs.filter(_.isRequired)

            requiredInputs.filter(_.value == "")
        }

        val formError = FormError()

        val formVal = form(action,onSubmit,confirmForm,autoComplete).asInstanceOf[ScalaElem]

        val submitButton = LoadingButton(sbmtButtonName,
            { () =>
                formVal.elem.dispatchEvent(main.submitEvent)
            },
            className = "btn btn-primary ignoreConfirm",
            style="margin-right:5px;",
            btnType = "submit")

        def render: ScalaElem = {
            d() (
                formVal(
                    formFunction(inputs,submitButton,formError)
                )
            )
        }
    }

    case class SelectOption(value:String,text:String)

    case class InlineNumberInput(
                                    override val inputOptions: InputOptions,
                                    initValue:Option[Long],
                                    min:Option[Int]=None,
                                    max:Option[Int] = None) extends InlineInput(
        inputOptions = inputOptions,
        NumberInput(initValue.map(_.toString).getOrElse(""),inputOptions.isRequired,min,max)
    )

    case class InlineCheckBoxInput(override val inputOptions: InputOptions,
                                   name:String,_value:Boolean)
        extends InlineInput(inputOptions, CheckBoxInput(name,_value,inputOptions.isRequired))

    case class InlineTextInput(override val inputOptions:InputOptions,
                               name:String,
                               override val initialValue: String="",
                               minLength:Option[Int]=None,
                               maxLength:Option[Int]=None,
                               isPassword:Boolean = false,
                               autoComplete:Option[String]=None,
                               hidden:Boolean = false)
        extends InlineInput(
            inputOptions,
            TextInput(name,initialValue,inputOptions.isRequired,minLength,maxLength,autoComplete,isPassword,hidden),
            hidden=hidden
        )

    case class InlineSelect(override val inputOptions:InputOptions,
                            name:String,
                            override val initialValue:String="",
                            options:Seq[SelectOption])
        extends InlineInput(
            inputOptions,
            input = DefaultSelect(name,initialValue,options,inputOptions))

    case class ErrorMessage(text:String)
        extends JSComponentWithState(text) {
        def render: ScalaElem = {
            d() { span(style="color:red;font-weight:bold;",text=state)}
        }
    }

    case class SuccessMessage(scalaElem: Option[ScalaElem],style:String = "") extends JSComponentWithState(scalaElem) {
        def render = {
            d()(
                if(state.isDefined) {
                    d(className="alert alert-success",style=style)(
                        state.get
                    )
                }  else {
                    d()
                }
            )

        }
    }

    class InlineInput(val inputOptions:InputOptions,
                      input:JSComponent with Input,
                      errorMessage:String="Field required",hidden:Boolean = false)
        extends JSComponentWithState(ErrorMessage("")) with Input {


        def isValid: Option[String] = input.isValid

        val initialValue = input.initialValue

        val isRequired = inputOptions.isRequired

        val initialTextValue = input.initialTextValue

        def value: String = input.value

        def onClick(f: Event => Any): Unit = input.onClick(f)

        def onChange(f: Event => Any): Unit = {
            cancelError()
            input.onChange(f)
        }

        override def onSubmit(f: Event => Any): Unit = {
            //cancelError()
            input.onSubmit(f)
        }

        def errorTrigger(str:Option[String]): Unit = {

            state.reRender(str.getOrElse(errorMessage))
            input.errorTrigger(str)
        }

        def cancelError(): Unit = {
            state.reRender("")
            input.cancelError()
        }

        input.onChange(x=>cancelError())

        val checkBoxValue = input.isInstanceOf[CheckBoxInput]

        def render: ScalaElem = {

            val isRequiredSpan =
                if(isRequired) Seq(span(style="color:red",text=" * "))
                else Seq()

            val labelEl =
                label(text=inputOptions.labelText,className = "form-label",style=s"width:${inputOptions.width.toString.trim}px;vertical-align:top;margin-top:8px;").appendSeq(
                    isRequiredSpan
                )

            val hiddenClass =if(hidden)" hidden" else ""

            d(className = "form-group form-inline"+hiddenClass) (
                labelEl,
                if(!checkBoxValue){
                    span(className="fieldValue")(
                        input.asInstanceOf[JSComponent]
                    )} else {
                    input.asInstanceOf[JSComponent]
                },
                state
            )
        }
    }

    case class DefaultSelect(name:String,
                             override val initialValue:String="",
                             options:Seq[SelectOption],
                             inputOptions:InputOptions)
        extends SelectInput(name=name,
            initialValue=initialValue,
            options=options,
            className="form-control",
            isRequired = inputOptions.isRequired)

    case class SselectInput(name:String,
                            options:Seq[SelectOption],
                            className:String="",
                            isRequired:Boolean)

    case class InputOptions(labelText:String,isRequired:Boolean,width:Double=170D)

    case class RWInput(input:InlineInput,isWritable:Boolean)
        extends JSComponentWithState(isWritable:Boolean) with Input {


        def isValid: Option[String] = if(isWritable) input.isValid else None

        def onClick(f: Event => Any): Unit = None

        def onChange(f: Event => Any): Unit = None

        def onSubmit(f: Event => Any): Unit = input.onSubmit(f)

        def isRequired: Boolean = input.isRequired

        def value: String = input.value

        def initialValue: String = input.initialValue

        def initialTextValue: String = input.initialTextValue

        def errorTrigger(str:Option[String]): Unit = None

        def cancelError(): Unit = None

        //State isWritable:Boolean
        def render: ScalaElem = {
            d() {
                if(state) {
                    input
                } else {
                    new ReadOnlyInput(input)
                }
            }
        }
    }

    class ReadOnlyInput(input:InlineInput) extends JSComponent {
        def render: ScalaElem = {
            input match {
                case inp:InlineCheckBoxInput =>
                    val newInp = CheckBoxInput(inp.name,inp._value,false)
                    newInp.inp.elem.setAttribute("disabled","disabled")
                    d(className = "form-group form-inline readField") (
                        label(input.inputOptions.labelText,className = "form-label",style=s"width:${input.inputOptions.width.toString}px"),
                        newInp

                    )
                case _ =>
                    d(className = "form-group form-inline readField") (
                        label(input.inputOptions.labelText,className = "form-label",style=s"width:${input.inputOptions.width.toString}px"),
                        span(className="fieldValue",text=input.initialTextValue)
                    )
            }

        }
    }

    class Triangle extends JSComponent {
        def render = {
            img("/assets/images/triangle.svg",Some(10),Some(7),style = "position: relative;top: -1px;",className = "noselect")
        }
    }

    case class NumberInput(initialValue:String,
                           isRequired:Boolean,
                           min:Option[Int]=None,
                           max:Option[Int] = None) extends JSComponent with Input {

        val inp = input("","number",initialValue,isRequired)


        def isValid: Option[String] = {
            val d = value.toDouble

            def checkMin:Option[String] = {
                min.flatMap { m =>
                    if(d < m) Some(s"Number must be more then $m")
                    else None
                }
            }

            def checkMax:Option[String] = {
                max.flatMap { m =>
                    if(d > m) Some(s"Number must be less then $m")
                    else None
                }
            }

            val minIsValidError = checkMin

            if(minIsValidError.isEmpty) {
                checkMax
            } else minIsValidError
        }

        def onClick(f: Event => Any): Unit = inp.onClick _

        def onChange(f: Event => Any): Unit = inp.onChange _

        override def onSubmit(f: Event => Any): Unit = inp.onSubmit _

        def value: String = inp.value

        def initialTextValue: String = initialValue.toString

        def errorTrigger(str:Option[String]): Unit = {}

        def cancelError(): Unit = {}

        def render: ScalaElem = {
            inp
        }
    }

    case class CheckBoxInput(name:String,
                             _value:Boolean,
                             isRequired:Boolean) extends JSComponent with Input {

        val initialValue = "true"

        val inp = input(name,"checkbox",initialValue,isRequired)

        if(_value == true) {
            inp.elem.setAttribute("checked","checked")
        }

        def isValid = None

        def onClick(f: Event => Any): Unit = inp.onClick _

        def onChange(f: Event => Any): Unit = inp.onChange _

        def onSubmit(f: Event => Any): Unit = inp.onSubmit _

        def value: String =
            if(inp.elem.asInstanceOf[org.scalajs.dom.html.Input].checked)
                name else "false"

        def initialTextValue: String = inp.initialTextValue

        def errorTrigger(str:Option[String]): Unit = inp.errorTrigger(str)

        def cancelError(): Unit = inp.cancelError()

        def render: ScalaElem = {
            span() (inp,span(className="checkBoxValue"))
        }
    }

    case class TextInput(name:String,
                         initialValue:String,
                         isRequired:Boolean,
                         minLength:Option[Int]=None,
                         maxLength:Option[Int]=None,
                         autoComplete:Option[String]=None,
                         isPassword:Boolean = false,
                         hidden:Boolean=false) extends JSComponent with Input {

        val inp = input(name,if(isPassword) "password" else "text",initialValue,isRequired,autoComplete = autoComplete)

        if(hidden) {
            inp.elem.classList.add("hidden")
        }

        minLength.foreach({ m =>
            inp.elem.setAttribute("minlength",m.toString)
        })

        maxLength.foreach({ m =>
            inp.elem.setAttribute("maxlength",m.toString)
        })

        def isValid:Option[String] = {
            val size = value.size
            def checkMin = {
                minLength.flatMap({ m =>
                    if(size < m) {
                        Some(s"This field has to have more then ${m-1} characters")
                    } else {
                        None
                    }
                })
            }

            def checkMax = {
                maxLength.flatMap({ m =>
                    if(size > m) {
                        Some(s"This field has to have less then ${m+1} characters")
                    } else {
                        None
                    }
                })
            }

            val minError = checkMin

            if(minError.isEmpty) {
                checkMax
            } else minError
        }

        def onClick(f: Event => Any): Unit = inp.onClick _

        def onChange(f: Event => Any): Unit = inp.onChange _

        def onSubmit(f: Event => Any): Unit = inp.onSubmit(f)

        def value: String = inp.value

        def initialTextValue: String = inp.initialTextValue

        def errorTrigger(str:Option[String]): Unit = inp.errorTrigger(str)

        def cancelError(): Unit = inp.cancelError()

        def render: ScalaElem = inp
    }


    case class SearchInput(name:String) extends JSComponent with Input {

        val inp = input(name=name,"text")

        inp.onKeyDown(e=> {
            val ke =e.asInstanceOf[KeyboardEvent]
            if(ke.keyCode == 13) {
                ke.preventDefault()
                false
            }
        })

        inp.elem.setAttribute("autofocus","true")

        inp.elem.setAttribute("tabindex","-1")

        inp.elem.asInstanceOf[HTMLElement].focus()

        def isValid = None

        def onClick(f: Event => Any): Unit = inp.onClick(f)

        def onChange(f: Event => Any): Unit = inp.onChange(f)

        def onSubmit(f: Event => Any): Unit = inp.onSubmit _

        def isRequired: Boolean = false

        def value: String = inp.value

        def initialValue: String = ""

        def initialTextValue: String = ""

        def errorTrigger(str:Option[String]): Unit = {}

        def cancelError(): Unit = {}

        def render: ScalaElem = {
            inp
        }
    }

    case class SearchSelectMenu(searchName:String) extends JSComponentWithState(false) {

        val inp = SearchInput(searchName)

        def render: ScalaElem = {
            d() {
                if(state) d(className="borderHollow",style = "position:absolute;padding:5px;") (
                    inp
                )
                else d()
            }

        }
    }

    class SelectInput(name:String,
                      val initialValue:String="",
                      options:Seq[SelectOption],
                      className:String="",
                      val isRequired:Boolean) extends JSComponentWithState(null:select) with Input {

        var errorTriggered = false

        val initialTextValue = options.find(o=>o.value==initialValue).map(_.text).getOrElse("")

        def isValid: Option[String] = None

        def onClick(f: Event => Any): Unit = state.onClick(f)

        def onChange(f: Event => Any): Unit = state.onChange(f)

        override def onSubmit(f: Event => Any): Unit = state.onSubmit(f)

        def errorTrigger(str:Option[String]): Unit = {
            errorTriggered = true
            reRender({
                val s = state
                val e = state.copyEventCallbacks
                val ns = state.copy(className = className + " error")
                ns.setEventCallbacks(e)
                ns
            })
        }

        def textValue: String = state.textValue

        def cancelError(): Unit = {
            if(errorTriggered) {
                state.elem.setAttribute("class",className)
                errorTriggered = false
            }
        }

        setState({
            val s = select(initialValue,name,className,isRequired)
            s.onChange(_ => cancelError())
            s
        })

        def value:String = state.value

        def render: ScalaElem = {
            span()(
                state.appendSeq(
                    options.map(o=>option(o.value,o.text,o.value==initialValue))
                )
            )
        }
    }

    class RadioInput(
                        override val initialValue:String,
                        name:String,
                        radioValue:String,
                        className:String="",
                        val selected:Boolean,
                        override val isRequired:Boolean=false
                    ) extends JSComponentWithState(null:radio) with Input {

        override def isValid: Option[String] = None

        def onClick(f: Event => Any): Unit = state.onClick(f)

        def onChange(f: Event => Any): Unit = state.onChange(f)

        def onSubmit(f: Event => Any): Unit = state.onSubmit _

        def value: String = state.value

        def initialTextValue: String = initialValue

        def errorTrigger(str:Option[String]): Unit = ???

        def cancelError(): Unit = ???

        setState({
            radio(initialValue,name,radioValue,className,selected,isRequired)
        })

        def render: ScalaElem = {
            span()(
                state,span(className="radioBoxValue")
            )
        }
    }
    trait Input {
        //None if valid Some(errorMessage) if not valid
        def isValid:Option[String]
        def onClick(f:Event => Any):Unit
        def onChange(f:Event => Any):Unit
        def onSubmit(f:Event => Any):Unit
        def isRequired:Boolean
        def value:String
        def initialValue:String
        def initialTextValue:String
        def errorTrigger(str:Option[String]):Unit
        def cancelError()
    }

    class RadioInputWithText(text:String,val radioInput: RadioInput) extends JSComponent with Input {

        def isValid: Option[String] = None

        def onClick(f: Event => Any): Unit = {
            radioInput.onClick(f)
            s.onClick(f)
        }

        def onChange(f: Event => Any): Unit = radioInput.onChange(f)

        def onSubmit(f: Event => Any): Unit = radioInput.onSubmit _

        def isRequired: Boolean = radioInput.isRequired

        def value: String = radioInput.value

        def initialValue: String = radioInput.value

        def initialTextValue: String = radioInput.initialTextValue

        def errorTrigger(str:Option[String]): Unit = ???

        def cancelError(): Unit = ???

        val s = span(
            text=text,
            style = "vertical-align:super;font-weight:bold;padding-left:5px;padding-right:5px;cursor:pointer;",
            onClick =Some( () => {
                radioInput.state.elem.asInstanceOf[html.Input].checked = true
            })
        )
        def render: ScalaElem = {
            span()(
                radioInput,
                s
            )
        }
    }

    class GroupRadioInput(val isRequired:Boolean,inputs:Seq[RadioInputWithText],layout:Seq[RadioInputWithText]=>JSComponent) extends JSComponent with Input {

        def groupOnClick(groupRadioFunc: GroupRadioInput=>Any) ={
            inputs.foreach(_.onClick((e)=>{
                groupRadioFunc(this)
            }))

        }

        override def isValid: Option[String] = None

        def onClick(e: Event => Any): Unit = {

            inputs.foreach(_.onClick(e))
        }

        def onChange(f: Event => Any): Unit = inputs.foreach(_.onChange(f))

        def onSubmit(f: Event => Any): Unit = inputs.foreach(_.onSubmit(f))

        def value: String = inputs.map(i=>i.value).find(str => str != "").getOrElse("")

        def initialValue: String = inputs.find(i=> i.radioInput.selected).map(_.initialValue).getOrElse("")

        def initialTextValue: String = inputs.find(i=> i.radioInput.selected).map(_.initialTextValue).getOrElse("")

        def errorTrigger(str:Option[String]): Unit = ???

        def cancelError(): Unit = ???
        def render: ScalaElem = {
            span()(
                layout(inputs)
            )
        }
    }
}
