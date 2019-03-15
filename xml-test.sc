import scala.xml.{Elem, Node, NodeSeq}
import scala.xml.transform.{RewriteRule, RuleTransformer}

val xml = <h1 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h1><p class="message_time faded-text--small">You sent this message on 12 March, 2019</p><p>What happens if I refuse to pay?</p><hr/><h2 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h2><p class="message_time faded-text--small">This message was sent to you on 12 March, 2019</p><p>I'm sorry but this tax bill is for you and you need to pay it. You can pay it online of at your bank.</p><hr/><h2 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h2><p class="message_time faded-text--small">You sent this message on 12 March, 2019</p><p>I have been sent a tax bill that I'm sure is for someone else as I don't earn any money. Please can you check.</p>



val stripHeadingsRule = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
        case elem: Elem if elem.label.eq("h1") => NodeSeq.Empty
        case n => n
    }
}

val transformer = new RuleTransformer(stripHeadingsRule)
xml.map(n => transformer.transform(n))


val xml2 = <root><a action="remove">element1</a> <a action="edit">element2</a></root>

val removeIt = new RewriteRule {
    override def transform(n: Node): Seq[Node] = n match {
        case e: Elem if (e \ "@action").text == "remove" => NodeSeq.Empty
        case `n` => n
    }
}

val transform2 = new RuleTransformer(removeIt)
transform2.transform(xml2)

