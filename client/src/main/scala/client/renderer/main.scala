package client.renderer

import client.renderer.utils.TrackedXMLHttpRequest
import org.scalajs.dom
import org.scalajs.dom.{Element, Event, Node, document}
import client.renderer.components.Modal

import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

object main {
    //Adds missing submit event that is not available for ScalaJS
    val submitEvent = js.Dynamic.newInstance(js.Dynamic.global.CustomEvent)("submit",js.Dynamic.literal(cancelable = true)).asInstanceOf[dom.CustomEvent]

    /**
     * Used to construct virtual DOM and render it to DOM
     */
    @JSExportTopLevel("ScalaDOM")
    object ScalaDOM {
        //Can be used to track and debug virtual DOM
        //Add rendered objects by ScalaDOM
        //val tree = ArrayBuffer[JSComponent]()

        private var onGoingXMLRequests:ArrayBuffer[TrackedXMLHttpRequest] = ArrayBuffer()

        private[renderer] def addOnGoingXMLRequest(trackedXMLHttpRequest: TrackedXMLHttpRequest):Unit = {
            onGoingXMLRequests += trackedXMLHttpRequest
        }

        /**
         * Used for selenium testing purposes
         * @return ScalaDOM is finished rendering.
         */
        @JSExport
        def XMLRequestsActive:Boolean = {
            onGoingXMLRequests = onGoingXMLRequests.flatMap({ xml =>
                if(xml.readyState == 4) None
                else Some(xml)
            })
            onGoingXMLRequests.nonEmpty
        }


        /**
         * @param id Element ID used for rendering the component.
         * @param jsComponent Component to render
         * @return
         */
        @JSExport
        def render(id:String,jsComponent:JSComponent) = {
            val main: Element = document.getElementById(id)
            jsComponent.renderTrue(main)
        }

        def spawnModal(title:String,text:JSComponent,submitButtonText:String="Save Changes",onContinue:()=>Unit) = {
            val body = dom.document.getElementsByTagName("body")(0)
            val modal = Modal(body,title,text,submitButtonText,onContinue)
            modal.show
        }
    }

    /**
     * Stateful component that is used to display changing data.
     * @param initialState Component state
     * @tparam T Type of Component State
     */
    abstract class JSComponentWithState[T](initialState:T) extends JSComponent {

        private var hiddenState = initialState

        def state = hiddenState

        def setState(state:T) = {
            hiddenState = state
        }

        /**
         * Re-renders the component based on the new state provided.
         * @param state State to replace current state with.
         */
        def reRender(state:T) = {

            hiddenState = state
            //console.log(s"COMP ${this.getClass} reRender called")

            removeChildren(jscChildren.head,elem)

            val newRender = render()

            jscChildren.head.jscChildren.clear()

            newRender.jscChildren.foreach({c=>build(jscChildren.head,c,false)})

        }
    }

    /**
     * Example of JS component can be found at client.pages.ClientTest
     */
    trait JSComponent {

        /**
         * Returns a virtual DOM of a component
         * @return Component to render
         */
        def render: ScalaElem

        private[renderer] val jscChildren = ArrayBuffer[JSComponent]()

        private[renderer] def removeChildren(jsc:JSComponent, parent:Element):Unit = {
            jsc.jscChildren.foreach {
                case q:ScalaElem =>
                    //console.log(q.elem,parent)
                    parent.removeChild(q.elem)
                case q:JSComponent =>
                    //console.log(parent,q.elem)
                    parent.removeChild(q.elem)
                    //removeChildren(q,parent)
            }
        }

        /**
         * Recursively constructs DOM tree from virtual DOM
         * @param parentJSC Parent JSComponent
         * @param jsc Current JSComponent
         * @param initial True if rendered first-time false if stateful component is re-rendered
         */
        private[renderer] def build(parentJSC:JSComponent, jsc:JSComponent, initial:Boolean=true): Unit = {
            jsc match {
                case q:ScalaElem =>
                    if(!initial)
                        parentJSC.jscChildren += q
                    parentJSC.elem.appendChild(q.elem)

                    q.jscChildren.foreach({ c=>
                        build(q,c)
                    })
                case q:JSComponent =>
                    q.jscChildren.clear()
                    val jsc = q.render
                    if(!initial)
                        parentJSC.jscChildren += q
                    q.elem = jsc.elem
                    q.jscChildren+=jsc
                    q.parentElem = parentJSC.elem
                    build(parentJSC,jsc)
            }
        }

        /**
         * Renders virtual dom to parent element
         * @param parent Element to render into
         * @return Virtual DOM of component
         */
        private[renderer] def renderTrue(parent: Element):JSComponent = {
            this.parentElem = parent

            this.jscChildren.clear()

            val jsc = render
            elem = jsc.elem
            //console.log(s"COMP ${this.getClass} rendered")
            jscChildren += jsc

            //for testing
            //ScalaDOM.tree += jsc

            parent.appendChild(jsc.elem)
            jsc.jscChildren.foreach({ c=>
                build(jsc,c)
            })

            jsc
        }

        //html element assigned to represent rendered JSComponent
        //Assigned based on render function return value of ScalaElem after ScalaElem has been rendered
        private[renderer] var elem:   Element = null

        private[renderer] var parentElem: Element = null

    }

    class EventDispatcher(elem:Element,event:String) {
        lazy val callbacks:ArrayBuffer[Event =>Any] = ArrayBuffer()
        def init() = {
            elem.addEventListener(event,(e:Event)=>{
                callbacks.foreach({c=>
                    c(e)
                })
            })
        }

        def addCallback[T](f:Event => T):Unit = {
            if(callbacks.isEmpty) init()
            callbacks += f
        }
    }

    /**
     * Used to abstract away JavaScript implementation from Scala implementation
     * @param elemName Elem name. Example: "div"
     * @param className Class of elem
     * @param text Text of elem
     * @param style Style of elem
     */
    class ScalaElem(elemName:String,
                    className:String = "",
                    text:String="",
                    style:String="") extends JSComponent {

        elem = document.createElement(elemName)

        lazy private val onChangeSource = new EventDispatcher(this.elem,"change")

        def onChange(f:Event =>Any) = onChangeSource.addCallback(f)

        lazy private val onClickSource = new EventDispatcher(this.elem,"click")

        def onClick(f:Event =>Any) = onClickSource.addCallback(f)

        lazy private val onKeyUpSource = new EventDispatcher(this.elem,"keyup")

        def onKeyUp(f:Event =>Any) = onKeyUpSource.addCallback(f)

        lazy private val onKeyDownSource = new EventDispatcher(this.elem,"keydown")

        def onKeyDown(f:Event =>Any) = onKeyDownSource.addCallback(f)

        private[renderer]def copyEventCallbacks: (ArrayBuffer[Event => Any], ArrayBuffer[Event => Any], ArrayBuffer[Event => Any], ArrayBuffer[Event => Any]) = {
            (onChangeSource.callbacks , onClickSource.callbacks,onKeyUpSource.callbacks,onKeyDownSource.callbacks)
        }

        private[renderer] def setEventCallbacks(tuple:(ArrayBuffer[Event => Any], ArrayBuffer[Event => Any], ArrayBuffer[Event => Any], ArrayBuffer[Event => Any])):Unit = {
            onChangeSource.init()
            onChangeSource.callbacks ++= tuple._1
            onClickSource.init()
            onClickSource.callbacks ++= tuple._2
            onKeyUpSource.init()
            onKeyUpSource.callbacks ++= tuple._3
            onKeyDownSource.init()
            onKeyDownSource.callbacks ++= tuple._4
        }

        private def remove: Node = elem.parentNode.removeChild(elem)

        //Not used for ScalaElem since ScalaElem is rendered on initialisation.
        def render:ScalaElem= {
            this
        }

        /**
         * Used to link components together.
         * Example:
         * d()(
         *      span(text="Example"),
         *      span(text="Example2")
         * )
         * @param comps Components to add inside of component
         * @return Current ScalaElem
         */
        def apply(comps: JSComponent*): ScalaElem = {
            //println(s"ELEM $elemName $className apply")
            appendSeq(comps)
            this
        }

        /**
         * Same thing as apply but with Seq
         * Example:
         * d().appendSeq(
         *      Seq(
         *          span(text="Example"),
         *          span(text="Example2")
         *      )
         * )
         *
         * @param comps Seq of components to include
         * @return Current component
         */
        def appendSeq(comps: Seq[JSComponent]): ScalaElem = {
            //children ++= comps
            jscChildren ++= comps
            this
        }

        if(className != "")
            className.split(" ").foreach(x=>elem.classList.add(x))

        if(style!="")
            elem.setAttribute("style",style)

        if(text != "")
            elem.textContent = text

        //console.log(s"COMP $elemName $className initialised")
    }
}
