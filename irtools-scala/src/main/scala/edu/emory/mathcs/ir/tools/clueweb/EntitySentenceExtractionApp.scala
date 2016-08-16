package edu.emory.mathcs.ir.tools.clueweb

import java.io._
import java.util.zip.GZIPInputStream

import edu.stanford.nlp.simple.Document
import lemur.nopol.ResponseIterator
import lemur.nopol.ResponseIterator.WarcEntry

import collection.JavaConverters._
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

case class FaccAnnotationRecord(docid: String,
                                encoding: String,
                                name: String,
                                mid: String,
                                start: Int,
                                end: Int,
                                conf: Float)

/**
  * Created by dsavenk on 8/8/16.
  */
object EntitySentenceExtractionApp {

  final val PADDING = 150

  @tailrec
  def readDocumentFromWarc(warcIterator: ResponseIterator, currentWarcDocumentId: String): Option[WarcEntry] = {
    if (warcIterator.hasNext) {
      val rec = warcIterator.next()
      if (currentWarcDocumentId == rec.trecId) {
        Some(rec)
      } else {
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

    //s"/home/dsavenk/Mounts/$cluewebDir/$disk/ClueWeb12_$cluewebPathStr/${parts(1)}/${parts(1)}-${parts(2)}.warc.gz"
    s"/$cluewebDir/$disk/ClueWeb12_$cluewebPathStr/${parts(1)}/${parts(1)}-${parts(2)}.warc.gz"
  }

  def normalizeSpanStr(str: String): String = {
    str.replaceAll("&[a-zA-Z#0-9]{1,6};", " ")
      .replaceAll("<.*?>","")  // Remove full and partial tags.
      .replaceAll("^.*?>","")
      .replaceAll("<.*?$","")
      .replaceAll("\\s+", " ")
      .replace("\n", ".0 ")
      .replace("\t", " ")
      .replaceAll("^[^\\s>]*?(\\s|>)","")   // Strip incomplete words at the beginning and at the end
      .replaceAll("(\\s|<)[^\\s<]*?$","")
  }

  def processRecords(docRecords: Seq[FaccAnnotationRecord], warcRecord: WarcEntry): Unit = {
    if (docRecords.nonEmpty) {
      val content = new String(warcRecord.content, docRecords.head.encoding).getBytes("UTF-8")
      val headerOffset = warcRecord.httpHeader.length
      var spans = new collection.mutable.ListBuffer[(Int, Int, Set[(String, String)])]()
      val docid = warcRecord.trecId
      for (record <- docRecords) {
        spans.append((math.max(0, record.start - headerOffset - PADDING),
          math.min(content.length, record.end - headerOffset + PADDING), Set((record.name, record.mid))))
      }
      spans = spans.sortBy(s => (s._1, s._2))

      val spansStr = new collection.mutable.ListBuffer[(String, Set[(String, String)])]()

      var currentSpan = spans.head
      for (span <- spans.tail) {
        if (span._1 >= currentSpan._2) {
          // finish span
          val passage = normalizeSpanStr(new String(content.slice(currentSpan._1, currentSpan._2), docRecords.head.encoding))
          spansStr.append((passage, currentSpan._3))
          currentSpan = span
        } else {
          currentSpan = (currentSpan._1, span._2, currentSpan._3.union(span._3))
        }
      }
      val passage = normalizeSpanStr(new String(content.slice(currentSpan._1, currentSpan._2), docRecords.head.encoding))
      spansStr.append((passage, currentSpan._3))

      for ((passage, entities) <- spansStr) {
        val doc = new Document(passage)
        val sentences = doc.sentences()
        for (sent <- sentences.asScala) {
          val text = sent.text()
          var firstMatch = true
          for ((name, mid) <- entities) {
            if (text.matches("(?i).*\\b" + Regex.quote(name) + "\\b.*")) {
              if (firstMatch) {
                firstMatch = false
                print("\n" + docid + "\t" + text)
              }
              print("\t" + name + "\t" + mid)
            }
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    var currentWarcDocument = ""
    var currentWarcFile = ""
    var warcStream: Option[FileInputStream] = None
    var warcIterator: Option[ResponseIterator] = None
    var currentDocument: Option[WarcEntry] = None
    var docRecords = new ArrayBuffer[FaccAnnotationRecord]()

    val input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args(0)))))
    var line = input.readLine()
    var docCount = 0
    val startTime = System.currentTimeMillis()
    while (line != null) {
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

        if (docRecords.nonEmpty && currentDocument.isDefined) processRecords(docRecords, currentDocument.get)

        currentWarcDocument = docid
        docRecords = new ArrayBuffer[FaccAnnotationRecord]()

        docCount += 1
        if (docCount % 1000 == 0) {
          val curTime = System.currentTimeMillis()
          System.err.println(s"$docCount documents processed... ${1.0 * (curTime - startTime) / docCount} sec per 1000 documents")
        }


        currentDocument = Some(readDocumentFromWarc(warcIterator.get, docid).get)
      }

      docRecords.append(FaccAnnotationRecord(docid, encoding, name, mid, start.toInt, end.toInt, conf.toFloat))

      line = input.readLine()
    }

    if (docRecords.nonEmpty) processRecords(docRecords, currentDocument.get)
  }
}
