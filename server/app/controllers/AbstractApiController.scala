package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request, Result}
import shared.{DELETERoute, GETRoute, POSTRoute}
import upickle.default._

class AbstractApiController @Inject() (cc:ControllerComponents) extends AbstractController(cc) {

    import scala.reflect.runtime.{universe => ru}

    def urlParamsToClass[T](req:Request[AnyContent])(implicit tag:ru.TypeTag[T]) = {
        val constructorMethod = tag.tpe.member(
            ru.termNames.CONSTRUCTOR
        ).asMethod

        val constructorParams = constructorMethod.paramLists.head.map(x => x.name -> x.typeSignature)

        val rm = ru.runtimeMirror(getClass.getClassLoader)

        val instanceMirror = rm.reflectClass(tag.tpe.typeSymbol.asClass)

        val q = req.queryString


        val constructorValues = constructorParams.map({
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Boolean] =>
                q.getOrElse(fieldName.toString,
                    throw new Exception("Required field not found"))
                    .head.toBoolean
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Long] =>
                q.getOrElse(fieldName.toString,
                    throw new Exception("Required field not found"))
                    .head.toLong
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Seq[Long]] =>
                q.getOrElse(fieldName.toString,
                    Seq())
                    .map(_.toLong)
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Seq[String]] =>
                q.getOrElse(fieldName.toString,
                    Seq())
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Option[Long]] =>
                q.get(fieldName.toString).map(_.head.toLong)
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Option[String]] =>
                q.get(fieldName.toString).map(_.head)
            case (fieldName,fieldType) if fieldType <:< ru.typeOf[Option[Int]] =>
                q.get(fieldName.toString).map(_.head.toInt)
        })

        val constructorMethodInvoked = instanceMirror.reflectConstructor(constructorMethod)

        constructorMethodInvoked.apply(constructorValues:_*).asInstanceOf[T]
    }

    def postResponse[T](route:POSTRoute[T])(func:T=>Result)(implicit t:ReadWriter[T],requestHeader:Request[AnyContent]) = {
        val json = requestHeader.body.asJson.getOrElse(BadRequest)
        val takes = read[T](json.toString)
        func(takes)
    }

    def getResponse[T,E](route:GETRoute[T,E],req:Request[AnyContent])(func:T=>Option[E])(implicit tag:ru.TypeTag[T],e:Writer[E]) = {
        if(tag.tpe.toString!="Null"){

            val takes = urlParamsToClass[T](req)

            func(takes).map(x=>Ok(write(x))).getOrElse(BadRequest)
        } else {
            func(null.asInstanceOf[T]).map(x=>Ok(write(x))).getOrElse(BadRequest)
        }
    }

    def deleteResponse[T](route:DELETERoute[T],req:Request[AnyContent])(func:T=>Result)(implicit tag:ru.TypeTag[T], t:ReadWriter[T]): Result = {
        if(tag.tpe.toString!="Null"){

            val takes = urlParamsToClass[T](req)

            func(takes)
        } else {
            func(null.asInstanceOf[T])
        }
    }
}
