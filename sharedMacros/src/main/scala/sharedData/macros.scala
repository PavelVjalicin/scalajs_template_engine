package sharedData

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object m {

    def classToURLParams[T](entity: T):String = macro imp[T]

    def imp[T:c.WeakTypeTag](c:blackbox.Context)(entity:c.universe.Tree):c.Tree = {
        import c.universe._
        val fields = symbolOf[T].asClass.primaryConstructor.typeSignature.paramLists.head

        val d = entity.duplicate

        def fieldToParam(field:Symbol) = {
            val fieldType = field.typeSignature



            if(fieldType==typeOf[Int] ||
            fieldType==typeOf[Long] ||
            fieldType==typeOf[String] ||
                fieldType==typeOf[Boolean])
                q"""
                     Some(${field.name.toString}+"="+${c.universe.TermName(field.name.toString)});
                 """
            else if(fieldType==typeOf[Option[Int]] ||
                fieldType==typeOf[Option[Long]] ||
                fieldType==typeOf[Option[String]]
            )
                q""" ${c.universe.TermName(field.name.toString)}
                .map({ x=> ${field.name.toString}+"="+x.toString}); """
            else
                q"""
                    if(${c.universe.TermName(field.name.toString)}.nonEmpty) {
                    Option(
                    ${c.universe.TermName(field.name.toString)}.map({
                    f => ${field.name.toString}+"="+f.toString
                    }).mkString("&"))
                    }; else None;

                 """
        }


        val list = q"List(..${fields.map(x=>fieldToParam(x))})"


        q""" "?"+$list.flatten.mkString("&");"""

    }


}