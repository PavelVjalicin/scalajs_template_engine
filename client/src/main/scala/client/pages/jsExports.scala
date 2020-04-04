package client.pages

import client.components.AddButtonForm
import client.renderer.components.inputs.{InlineTextInput, InputOptions}
import client.renderer.main.{JSComponent, JSComponentWithState}
import client.renderer.scalaElems.{Button, d, h, span}
import client.renderer.{main, utils}
import shared.{IDInput, Note}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("comps")
object jsExports {
    @JSExport
    def NotesPage = {
        new NotesPage()
    }
}

case class NotesState(loaded:Boolean,
                      notes:Seq[Note])

class NotesPage extends JSComponentWithState[NotesState](NotesState(false,Seq())) {

    def getNotes(): Unit = {
        utils.requests.get(shared.jsRoutes.get.notes).foreach(notes => {
            this.reRender(NotesState(loaded = true,notes.seq))
        })
    }

    def onDelete(noteId:Long) = {
        utils.requests.delete(shared.jsRoutes.delete.note,IDInput(noteId))(onSuccess = { () =>
            getNotes()
        }) (onError=()=>None)
    }

    getNotes()



    def render: main.ScalaElem = {
        d()(
            if(this.state.loaded) d()(
                h(2)("Notes"),
                d().appendSeq(this.state.notes.map(note => new NoteComponent(note,onDelete))),
                new NoteInput(() => getNotes())
            ) else d(text = "Loading...")
        )
    }
}

class NoteComponent(note:Note,onDelete:(Long) => Unit) extends JSComponent {
    def render: main.ScalaElem = {
        d()(
            h(3)(note.title),
            d(text=note.text),
            Button("Remove",() => onDelete(note.id.get))
        )
    }
}

class NoteInput(onSubmit:()=>Unit) extends JSComponent {

    def title = {
        InlineTextInput(
            InputOptions("Title", isRequired = true, 100D),
            "title",
            ""
        )
    }

    def body = {
        InlineTextInput(
            InputOptions("Note", isRequired = true, 100D),
            "body",
            ""
        )
    }

    def render: main.ScalaElem = {
        d()(
            AddButtonForm("Add note",None,shared.jsRoutes.post.note,onSuccess =
                (f: () => Unit) => {
                    f()
                    onSubmit()
                },
                Seq(()=> title,() => body))
            { case Seq(title,body) => Note(title.value,body.value) } {
                case Seq(title,body) => d()(title,body)
            }
        )
    }
}