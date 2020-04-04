package client.renderer

import client.jsnative
import client.jsnative.App
import org.scalajs.dom
import org.scalajs.dom.ext.AjaxException
import org.scalajs.dom.raw.HTMLScriptElement
import org.scalajs.dom.{Event, XMLHttpRequest}
import client.renderer.main.ScalaDOM
import shared._
import upickle.default.{read, write}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import upickle.default._

object utils {

    class TrackedXMLHttpRequest() {

        private val xml = new XMLHttpRequest()

        def onreadystatechange(f:(Event)=>Unit):Unit = {
            xml.onreadystatechange = f
        }

        def readyState = xml.readyState

        def status = xml.status

        def responseText = xml.responseText

        def setRequestHeader(header: String, value: String): Unit = xml.setRequestHeader(header,value)

        def open(method: String, url: String, async: Boolean=true): Unit = {
            ScalaDOM.addOnGoingXMLRequest(this)
            xml.open(method, url, async)
        }

        def exception = AjaxException(xml)

        def send(data: js.Any) = xml.send(data)
    }

    def urlPrefix = dom.window.location.origin+"/"+jsRoutes.prefix

    object requests {

        def getMainContentPage(url:URL) = {
            val xml = new TrackedXMLHttpRequest()

            xml.onreadystatechange ( (e:Event) => {
                if(xml.readyState == 4) {
                    if(xml.status == 200) {
                        val mainContent = dom.document.getElementById("mainContent")
                        dom.window.history.pushState("{}",null,"/"+url.path)

                        mainContent.innerHTML = xml.responseText

                        import org.scalajs.dom.ext._

                        val scripts = mainContent.getElementsByTagName("script")

                        scripts.foreach({ x =>
                            jsnative.eval(x.asInstanceOf[HTMLScriptElement].text)
                        })

                        import client.jsnative._
                        App.addButtonHandlers($("#mainContent"))
                    } else if(xml.status == 401){
                        App.sessionExpired()
                    }
                }
            })

            xml.open("GET","/"+url.path+s"""?pageType=mainContent""",true)
            xml.send(null)
        }

        def get[E](jsR:GETRoute[Null,E])(implicit readWriter: ReadWriter[E]): Future[E] = {
            val xml = new TrackedXMLHttpRequest()

            val promise = Promise[E]()

            xml.onreadystatechange( (e: Event) => {
                if(xml.readyState == 4) {
                    if (xml.status == 200) {
                        val response = xml.responseText
                        val resp = read[E](response)
                        promise.success(resp)
                    } else if(xml.status == 401){
                        App.sessionExpired()
                    } else {
                        promise.failure(xml.exception)
                    }
                }
            })

            xml.open("GET",urlPrefix+jsR.url.path,true)
            xml.send(null)

            promise.future
        }

        def get[T<:RouteParam,E](jsR:GETRoute[T,E], params:T)(implicit readWriter: ReadWriter[E]): Future[E]= {
            val xml = new TrackedXMLHttpRequest()

            val promise = Promise[E]

            xml.onreadystatechange(
                (e:Event) => {
                    if(xml.readyState.toInt == 4) {
                        if (xml.status == 200) {
                            val response = xml.responseText
                            val resp = read[E](response)
                            promise.success(resp)
                        } else if(xml.status == 401){
                            App.sessionExpired()
                        } else {
                            promise.failure(xml.exception)
                        }
                    }
                })

            val questionMark = if(params.paramSeq.nonEmpty) "?" else ""

            val paramStr = questionMark + params.paramSeq.map { case (name,value) =>  name+"="+value.toString } .mkString("&")

            xml.open("GET",urlPrefix+jsR.url.path+paramStr,true)
            xml.send(null)
            promise.future
        }


        def delete[T<:RouteParam](jsR:DELETERoute[T],params:T)(onSuccess:()=>Unit)(onError:()=>Unit)(implicit readWriter: ReadWriter[T]): Unit = {
            val xml = new TrackedXMLHttpRequest()
            xml.onreadystatechange(
                (e:Event) => {
                    if(xml.readyState == 4) {
                        if (xml.status == 200) {
                            val response = xml.responseText
                            onSuccess()
                        } else if(xml.status == 401){
                            App.sessionExpired()
                        } else {
                            onError()
                        }
                    }
                })

            val paramStr = "?" + (params.paramSeq).map { case (name,value) =>  name+"="+value.toString } .mkString("&")

            xml.open("DELETE",urlPrefix+jsR.url.path+paramStr,true)
            xml.setRequestHeader("Csrf-Token",jsnative.App.t)
            xml.send(null)
        }

        def post[T](jsr:POSTRoute[T],obj:T)(onSuccess:()=>Unit)(onError:(String)=>Unit)(implicit readWriter: ReadWriter[T]):Unit = {

            val xml = new TrackedXMLHttpRequest()

            xml.onreadystatechange((e:Event) => {
                if(xml.readyState == 4) {
                    if (xml.status == 201) {
                        val response = xml.responseText
                        onSuccess()
                    } else if(xml.status == 401){
                        println(401)
                        App.sessionExpired()
                    } else {
                        onError(xml.responseText)
                    }
                }
            })

            xml.open("POST", urlPrefix+jsr.url.path)
            xml.setRequestHeader("Content-Type", "application/json; charset=UTF-8")
            xml.setRequestHeader("Csrf-Token",jsnative.App.t)
            xml.send(write(obj))
        }

    }

}
