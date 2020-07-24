package search.sol

import search.src.{FileIO, PorterStemmer, StopWords}

import scala.collection.mutable
import scala.util.matching.Regex
import scala.xml.NodeSeq


class Indexer(file: String) {

  // list of titles, to be added to and cleared later
  var titleList: List[String] = List()

  // regex to be used in tokenizing
  val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")

  // maps page ids to the page title
  val idToTitle: scala.collection.mutable.HashMap[Int, String] = scala.collection.mutable.HashMap[Int, String]()

  // maps page ids to the frequency of the most frequent word in the text of the page
  val idToMaxWordCount: scala.collection.mutable.HashMap[Int, Double] = scala.collection.mutable.HashMap[Int, Double]()

  // maps words to hashmaps that maps page ids to the frequency of the word in that page
  val wordToIdToWordFrequency: scala.collection.mutable.HashMap[String, scala.collection.mutable.HashMap[Int, Double]] =
    scala.collection.mutable.HashMap[String, scala.collection.mutable.HashMap[Int, Double]]()

  // maps ids to the titles it links to
  val idsToLinks: scala.collection.mutable.HashMap[Int, List[String]] =
    scala.collection.mutable.HashMap[Int, List[String]]()

  // maps page ids to their page's rank
  val idToRank: scala.collection.mutable.HashMap[Int, Double] = new scala.collection.mutable.HashMap[Int, Double]

  /** pipeLinkProcessor
   * processes pipe links
   *
   * @param link the link
   * @param id   the id of the page its contained in
   * @return the words contained in the link that should be processed by wordHandler
   */
  def pipeLinkProcessor(link: String, id: Int): List[String] = {

    val pipeLinkRegex: Regex = new Regex("""[^\[\|\]]+""")

    // List with first element title, second element text
    val internalWords: List[String] = pipeLinkRegex.findAllMatchIn(link).toList.map { aMatch => aMatch.matched }

    val linkTitle: String = internalWords(0)

    if (!linkTitle.equals(idToTitle(id)) && titleList.contains(linkTitle) && !idsToLinks(id).contains(linkTitle)) {
      idsToLinks(id) = linkTitle :: idsToLinks(id)
    }

    internalWords(1).split(" ").toList
  }

  /** normalLinkProcessor
   * processes normal links
   *
   * @param link the link
   * @param id   the id of the page its contained in
   * @return the words contained in the link that should be processed by wordHandler
   */
  def normalLinkProcessor(link: String, id: Int): List[String] = {

    val linkRegex: Regex = new Regex("""[^\[\]]+""")

    val textInAList: List[String] = linkRegex.findAllMatchIn(link).toList.map { aMatch => aMatch.matched }

    val text: String = textInAList(0)

    if (!text.equals(idToTitle(id)) && titleList.contains(text) && !idsToLinks(id).contains(text)) {
      idsToLinks(id) = text :: idsToLinks(id)
    }

    text.split(" ").toList
  }

  /** metaLinkProcessor
   * processes metaLinks
   *
   * @param link the link
   * @param id   the id of the page its contained in
   */
  def metaLinkProcessor(link: String, id: Int): Unit = {

    val linkRegex: Regex = new Regex("""[^\[\]]+""")

    val newTitle: List[String] = linkRegex.findAllMatchIn(link).toList.map { aMatch => aMatch.matched }

    val text: String = newTitle(0)

    if (!text.equals(idToTitle(id)) && titleList.contains(text) && !idsToLinks(id).contains(text)) {
      idsToLinks(id) = text :: idsToLinks(id)
    }

  }

  /** wordhandler
   * handles the preprocessing and hasmap-adding for a single word
   *
   * @param word the word
   * @param id   the id of the page it's contained in
   * @param map  the freqMap, map of words to wordFrequencies that is local to indexerHelper
   */
  def wordHandler(word: String, id: Int, map: scala.collection.mutable.HashMap[String, Int]): Unit = {
    if (StopWords.isStopWord(word)) {
    } else {
      val stemmed = PorterStemmer.stem(word).toLowerCase
      if (StopWords.isStopWord(stemmed)) {} else {

        if (map.contains(stemmed)) {
          map(stemmed) = map(stemmed) + 1
        } else {
          map(stemmed) = 1
        }

        if (wordToIdToWordFrequency.contains(stemmed)) {
          if (wordToIdToWordFrequency(stemmed).contains(id)) {
            wordToIdToWordFrequency(stemmed)(id) = wordToIdToWordFrequency(stemmed)(id) + 1
          } else {
            wordToIdToWordFrequency(stemmed)(id) = 1
          }
        } else {
          wordToIdToWordFrequency(stemmed) = scala.collection.mutable.HashMap(id -> 1)
        }

      }
    }
  }

  /** indexerHelper
   * indexes an individual page in the corpus
   *
   * @param i a page
   */
  def indexerHelper(i: NodeSeq) {
    // gets the title
    val title: String = (i \ "title").text.trim()

    // gets the id
    val id: Int = (i \ "id").text.trim().toInt

    // add to the idToTitle Map
    idToTitle(id) = title

    // add the id to the ids to links map
    idsToLinks(id) = List()

    // Call findAllMatchIn to get an iterator of Matches
    val matchesIterator = regex.findAllMatchIn((i \ "text").text)

    // Convert the Iterator to a List and extract the matched substrings, creating a tokenized list
    val matchesList: List[String] = matchesIterator.toList.map { aMatch => aMatch.matched }

    // map with word frequencies
    val freqMap: scala.collection.mutable.HashMap[String, Int] = scala.collection.mutable.HashMap[String, Int]()

    for (word <- matchesList) {
      // see if it's a link
      if (word.matches("""\[\[[^\[]+?\]\]""")) {

        // see if it's a pipe link, do stuff
        if (word.matches("""\[\[[^\[]+?\|[^\[]+?\]\]""")) {

          val wordList: List[String] = pipeLinkProcessor(word, id)
          wordList.map(w => wordHandler(w, id, freqMap))

        } else if (word.matches("""\[\[[^\[]+?\:[^\[]+?\]\]""")) {
          metaLinkProcessor(word, id)

        } else {
          val wordList = normalLinkProcessor(word, id)
          wordList.map(w => wordHandler(w, id, freqMap))
        }

      } else {
        wordHandler(word, id, freqMap)
      }
    }

    if (idsToLinks(id).isEmpty) {
      idsToLinks(id) = titleList.filter(x => x != title)
    }

    if (freqMap.isEmpty) {
      idToMaxWordCount(id) = 0
    } else {
      idToMaxWordCount(id) = freqMap.valuesIterator.max
    }
  }

  /**
   * indexer
   * indexes the corpus, i.e. fills out all the relevant hashmaps (idToTitle, idToMaxWordCount,
   * wordToIdToWordFrequency, idsToLinks)
   */
  def indexer(): Unit = {

    val pageSeq: NodeSeq = xml.XML.loadFile(this.file) \ "page"

    titleList = {
      var result = List[String]()
      for (i <- pageSeq \ "title") {
        result = i.text.trim() :: result
      }
      result
    }

    for (i <- pageSeq) {
      indexerHelper(i)
    }

    // clear titleList
    titleList = List()

  }

  /** pagerank
   * computes the rank of each page based on links in the corpus
   */
  def pagerank() {

    val weights: mutable.HashMap[Int, mutable.HashMap[Int, Double]] =
      new mutable.HashMap[Int, mutable.HashMap[Int, Double]]()

    val eps = 0.15

    val n = idToTitle.size

    // initialize weights: weights(i)(j) is the weight that j gives to i
    for (j <- idToTitle.keysIterator) {

      for (i <- idToTitle.keysIterator) {

        if (weights.contains(i)) {} else {
          weights(i) = mutable.HashMap[Int, Double]()
        }

        if (idsToLinks(j).contains(idToTitle(i))) {
          weights(i)(j) = (eps / n) + ((1.0 - eps) * (1.0 / idsToLinks(j).size))
        } else {
          weights(i)(j) = (eps / n)
        }
      }

      idsToLinks.remove(j)

    }

    idsToLinks.clear()

    val idList: List[Int] = idToTitle.keysIterator.toList

    var r: Array[Double] = Array.fill[Double](n)(0)
    var rp: Array[Double] = Array.fill[Double](n)(1.0 / n)

    def distance(array1: Array[Double], array2: Array[Double]): Double = {
      var sum: Double = 0.0
      for (i <- 0 until n) {
        val x: Double = array2(i) - array1(i)
        sum = sum + (x * x)
      }
      Math.sqrt(sum)
    }

    while (distance(r, rp) > 0.001) {
      r = rp
      for (j <- 0 to n - 1) {
        rp(j) = 0
        for (k <- 0 to n - 1) {
          rp(j) = rp(j) + (weights(idList(j))(idList(k)) * r(k))
        }
      }
    }

    for (i <- 0 to n - 1) {
      idToRank(idList(i)) = rp(i)
    }

  }


}

/*

arg(0) is filepath of the corpus
arg(1) is filepath of titles.txt
arg(2) is filepath of words.txt
arg(3) is filepath of docs.txt

*/

/** Indexing
 * main object
 */
object Indexing {
  /**
   * main method, indexes a corpus
   *
   * @param args : in order, the filepath of the corpus, the filepath of titles.txt, the filepath of words.txt,
   *             the file path of docs.txt
   */
  def main(args: Array[String]) = {

    val index = new Indexer(args(0))
    index.indexer()
    index.pagerank()

    FileIO.printTitleFile(args(1),
      index.idToTitle)

    FileIO.printWordsFile(args(2),
      index.wordToIdToWordFrequency)

    FileIO.printDocumentFile(args(3),
      index.idToMaxWordCount, index.idToRank)
  }
}