package edu.emory.mathcs.ir.web

import collection.JavaConverters._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.html.BoilerpipeContentHandler
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.BodyContentHandler

/**
  * Created by dsavenk on 4/28/16.
  */
object ContentExtractor {
  def apply(htmlCode: String, maxDocumentLength: Int = 100000, contentMinLength: Int = 0): List[String] = {
    val textHandler = new BoilerpipeContentHandler(new BodyContentHandler(maxDocumentLength))
    val metadata = new Metadata()
    val parser = new AutoDetectParser()
    val context = new ParseContext()
    try {
      parser.parse(new ByteArrayInputStream(
        htmlCode.getBytes(StandardCharsets.UTF_8)),
        textHandler,
        metadata,
        context)
      val content = textHandler.getTextDocument.getTextBlocks.asScala
        .filter(_.isContent)
        .map(_.getText)
        .filter(_.length >= contentMinLength)
        .map(_.replace("\n", ". ").replaceAll("\\s{2,}", ". "))
        .toList
      content
    } catch {
      case exc: Exception => System.err.println(exc.getMessage)
        Nil
    }
  }
}