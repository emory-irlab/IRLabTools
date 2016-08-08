package edu.emory.mathcs.ir.tools.clueweb

import java.io.{BufferedReader, FileInputStream, InputStream, InputStreamReader}

import edu.stanford.nlp.simple.Document
import lemur.nopol.ResponseIterator
import lemur.nopol.ResponseIterator.WarcEntry

import collection.JavaConverters._
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

case class FaccAnnotationRecord(docid: String,
                                encoding: String,
                                name: String,
                                mid: String,
                                beg: Int,
                                end: Int,
                                conf: Float)

/**
  * Created by dsavenk on 8/8/16.
  */
object EntitySentenceIndexingApp {

  @tailrec
  def readDocumentFromWarc(warcIterator: ResponseIterator, currentWarcDocumentId: String): Option[WarcEntry] = {
    if (warcIterator.hasNext) {
      val rec = warcIterator.next()
      if (currentWarcDocumentId.equals(rec.trecId))
        Some(rec)
      else {
        readDocumentFromWarc(warcIterator, currentWarcDocumentId)
      }
    } else {
      None
    }
  }

  def getWarcFilePath(fileId: String): String = {
    val parts = fileId.split("-")
    val cluewebPathStr = parts(1).substring(0, 2)
    val cluewebPartNo = cluewebPathStr.toInt
    val cluewebDir =  if (cluewebPartNo < 10) "ClueWeb1" else "ClueWeb2"
    val disk =
      if (cluewebPartNo < 5) "Disk1"
      else if (cluewebPartNo < 10) "Disk2"
      else if (cluewebPartNo < 15) "Disk3"
      else "Disk4"

    //s"/home/dsavenk/Projects/$cluewebDir/$disk/ClueWeb12_$cluewebPathStr/${parts(1)}/${parts(1)}-${parts(2)}.warc.gz"
    s"/$cluewebDir/$disk/ClueWeb12_$cluewebPathStr/${parts(1)}/${parts(1)}-${parts(2)}.warc.gz"
  }

  def processRecords(docRecords: Seq[FaccAnnotationRecord], document: Document): Unit = {
    if (docRecords.nonEmpty) {
      val docid = docRecords.head.docid
      val entities = docRecords.map(rec => rec.name.toLowerCase() -> rec.mid).toMap
      for (sent <- document.sentences().asScala) {
        val text = sent.text().replace("\t", " ").replace("\n", " ").replace("\r", " ")
        val textLow = text.toLowerCase
        if (entities.keys.exists(textLow.contains(_))) {
          print(docid + "\t" + sent.text() + "\t")
          for ((name, mid) <- entities) {
            if (textLow.contains(name)) {
              print(name + "\t" + mid + "\t")
            }
          }
        }
        println()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    var currentWarcDocument = ""
    var currentWarcFile = ""
    var warcStream: Option[FileInputStream] = None
    var warcIterator: Option[ResponseIterator] = None
    var currentDocument: Option[Document] = None
    var docRecords = new ArrayBuffer[FaccAnnotationRecord]()

    for (line <- scala.io.Source.stdin.getLines()) {
      val Array(docid, encoding, name, start, end, conf, _, mid) = line.split("\t")

      if (docid != currentWarcDocument) {
        val warcPath = getWarcFilePath(docid)
        if (currentWarcFile != warcPath) {
          if (warcStream.isDefined) {
            warcStream.get.close()
          }
          warcStream = Some(new FileInputStream(warcPath))
          warcIterator = Some(new ResponseIterator(warcStream.get))
          currentWarcFile = warcPath
        }

        processRecords(docRecords, currentDocument.get)

        currentWarcDocument = docid
        docRecords = new ArrayBuffer[FaccAnnotationRecord]()
        currentDocument = Some(new Document(
          new String(readDocumentFromWarc(warcIterator.get, docid).get.content, encoding)))

      }

      docRecords.append(FaccAnnotationRecord(docid, encoding, name, mid, start.toInt, end.toInt, conf.toFloat))
    }
  }
}
