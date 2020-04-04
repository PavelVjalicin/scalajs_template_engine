package client.renderer

import client.renderer.components.inputs.Input
import client.renderer.utils.urlPrefix
import org.scalajs.dom.{Event, html}
import client.renderer.main._
import shared.{POSTRoute, URL}

object scalaElems {

    case class Cross(style:String="") extends JSComponent {
        val scalaElem = i(className="fa fa-times",style="color:red;font-size:18px;"+style)
        def render: ScalaElem = scalaElem
    }

    case class Sort(column:JSComponent,colNum:Byte,onClick:(Byte) => Unit,initialState:Option[Boolean]) extends JSComponentWithState(initialState) {

        def f = () => {
            onClick(colNum)
        }

        def render: ScalaElem = {

            span(style = "cursor:pointer;",onClick = Some(f))(
                column,
                if(state.isEmpty) {
                    span(className = "fa fa-sort sort-col")
                } else {
                    if(state.get)
                        span(className = "fa fa-sort fa-sort-asc sort-col")
                    else
                        span(className = "fa fa-sort fa-sort-desc sort-col")
                }
            )

        }
    }

    case class Tick() extends JSComponent {
        val scalaElem = i(className="fa fa-check",style="color:green;font-size:18px;")
        def render: ScalaElem = scalaElem
    }

    case class select(initialValue:String,
                      name:String,
                      className:String="",
                      isRequired:Boolean=false)
        extends ScalaElem(
            "select",
            className = className) with Input {

        def isValid = None

        if(isRequired) elem.setAttribute("required","required")

        def errorTrigger(str:Option[String]):Unit = {}

        lazy val initialTextValue = initialValue

        def cancelError(): Unit = {}

        private def s = elem.asInstanceOf[html.Select]
        elem.setAttribute("name",name)
        private def selected = s.selectedIndex
        def value = {
            s.options(selected).value
        }

        def textValue: String = s.options(selected).text
    }

    case class radio(
                        initialValue:String,
                        name:String,
                        radioValue:String,
                        className:String="",
                        selected:Boolean,
                        isRequired:Boolean=false
                    ) extends ScalaElem(style="cursor:pointer;",
        elemName = "input",
        className= className
    ) with Input {

        def isValid = None

        private def r = elem.asInstanceOf[html.Input]

        def value: String = if(r.checked) radioValue else ""

        def initialTextValue: String = initialValue

        elem.setAttribute("name",name)
        elem.setAttribute("type","radio")
        if(selected) elem.setAttribute("checked","checked")

        def errorTrigger(str:Option[String]):Unit = ???

        def cancelError(): Unit = ???
    }



    case class option(value:String,text:String,isSelected:Boolean) extends ScalaElem("option",text=text)  {
        elem.setAttribute("value",value)
        if(isSelected) elem.setAttribute("selected","selected")
    }

    case class form[T](action:POSTRoute[T],onSubmit:()=>Any,confirmForm:Boolean = true,autoComplete:Boolean = true) extends ScalaElem("form") {

        if(confirmForm)
            elem.setAttribute("data-form-confirm","")
        elem.setAttribute("action",urlPrefix + action.url.path)

        if(!autoComplete) {
            elem.setAttribute("autocomplete","off")
        }

        elem.addEventListener(
            "submit", (e:Event) => {
                e.preventDefault()
                if(!e.isTrusted) onSubmit()
                false
            },true
        )
    }

    case class i(text:String="",className:String="",style:String="") extends ScalaElem("i",className = className,text=text,style = style)

    case class label(text:String,className:String="",style:String="") extends ScalaElem("label",className=className,text=text,style = style)

    case class span(text:String="",className:String="",style:String="",onClick:Option[()=>Unit]=None) extends ScalaElem("span",className=className,text = text,style=style) {

        onClick.foreach({oc =>
            elem.addEventListener(
                "click", (e: Event) =>
                    oc()
            )
        })
    }


    case class img(path:String,height:Option[Int]=None,width:Option[Int]=None,style:String="",className:String="") extends ScalaElem("img",style=style,className = className) {

        elem.setAttribute("src",path)
        height.foreach(height => elem.setAttribute("height",height.toString))
        width.foreach(width => elem.setAttribute("width",width.toString))
    }

    case class table(className:String="") extends  ScalaElem("table",className = className)

    case class input(name:String,inputType:String,initialValue:String = "",isRequired:Boolean=false,autoComplete:Option[String]=None) extends ScalaElem("input",className = "form-control") with Input {

        def isValid = None

        def initialTextValue: String = initialValue

        def onClick(f:Event => Unit) = super.onClick _

        def onChange(f: Event => Unit) = super.onChange _

        def errorTrigger(str:Option[String]):Unit = {}

        def cancelError(): Unit = {}

        def value = elem.asInstanceOf[html.Input].value

        def textValue = elem.asInstanceOf[html.Input].value
        elem.setAttribute("name",name)
        elem.setAttribute("type",inputType)
        elem.setAttribute("value",initialValue)
        autoComplete.foreach({ x=>
            elem.setAttribute("autocomplete",x)
        })
        if(isRequired) elem.setAttribute("required","required")
    }
    case class td(text:String="",className:String="") extends  ScalaElem("td",text = text,className = className)
    case class tr(className:String="") extends  ScalaElem("tr",className = className)
    case class th(text:String="",className:String="",colSize:Option[Byte]=None,rowSize:Option[Byte]=None,style:String="") extends  ScalaElem("th",text = text,className = className,style = style) {
        colSize.foreach(x=>this.elem.setAttribute("colspan",x.toString))
        rowSize.foreach(x=>this.elem.setAttribute("rowspan",x.toString))
    }
    case class br() extends ScalaElem("br")
    case class a(href:URL,text:String="",style:String="",className:String="") extends ScalaElem("a",text=text,style=style,className=className) {
        elem.setAttribute("href","/"+href.path)
        this.onClick((e:Event) => {
            e.preventDefault()
            utils.requests.getMainContentPage(href)
        })
    }

    case class col() extends ScalaElem("div",className = "col-xs-6")

    case class row() extends ScalaElem("div",className = "row")

    case class d(className:String="",style:String="",text:String="") extends ScalaElem("div",className,style = style,text=text)

    case class strong(className:String="",style:String="",text:String="") extends ScalaElem("strong",className = className,text=text,style=style)

    case class h(int:Int,className:String="",style:String="")(text:String) extends ScalaElem("h"+int,text = text,style=style,className = className)

    case class NormalButton(name:String,
                               onClick:() => Any,
                               className:String="",
                               btnType:String="",
                               style:String="") extends Button(name,onClick,"btn" + className,btnType,style)

    case class SecondaryButton(name:String,
                               onClick:() => Any,
                               className:String="",
                               btnType:String="",
                               style:String="") extends Button(name,onClick,"btn btn-secondary " + className,btnType,style)

    case class PrimaryButton(name:String,
                             onClick:() => Any,
                             className:String="",
                             btnType:String="",
                             style:String="") extends Button(name,onClick,"btn btn-primary " + className,btnType,style)

    case class DangerButton(name:String,
                            onClick:() => Any,
                            className:String="",
                            btnType:String="",
                            style:String="") extends Button(name,onClick,"btn btn-danger " + className,btnType,style)

    object Button {
        def apply(name:String,onClick:() => Any,className:String="",btnType:String="",style:String=""): Button = {
            new Button(name,onClick,className,btnType,style)
        }
    }

    class Button(name:String,onClick:() => Any,className:String="",btnType:String="",style:String="") extends ScalaElem("button",className=className,style=style) {

        elem.textContent = name

        if(btnType!= "")
            elem.setAttribute("type",btnType)

        elem.addEventListener(
            "click", (e: Event) =>
                onClick()
        )
    }
}
