package search.sol

import java.io._

import search.src.StopWords.isStopWord
import search.src.{FileIO, PorterStemmer}

import scala.collection.mutable.HashMap


/**
 * Represents a query REPL built off of a specified index
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if page rank is to be incorporated into scoring
 */
class Query(titleIndex: String, documentIndex: String, wordIndex: String,
            usePageRank: Boolean) {

  // Maps the document ids to the title for each document
  private val idsToTitle = new HashMap[Int, String]

  // Maps the document ids to the euclidean normalization for each document
  private val idsToMaxFreqs = new HashMap[Int, Double]

  // Maps the document ids to the page rank for each document
  private val idsToPageRank = new HashMap[Int, Double]

  // Maps each word to its inverse document frequency
  private val wordToInvFreq = new HashMap[String, Double]

  // Maps each word to a map of document IDs and frequencies of documents that
  // contain that word
  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int, Double]]


  /**
   * Handles a single query and prints out results
   *
   * @param userQuery - the query text
   */
  private def query(userQuery: String) {
    // all of this is done in readfiles:
    //FileIO.readTitles(this.titleIndex, idsToTitle)
    //FileIO.readWords(this.wordIndex, wordsToDocumentFrequencies)
    //FileIO.readDocuments(this.documentIndex, idsToMaxFreqs, idsToPageRank)

    // split into words
    var listQuery = userQuery.split(" ")

    // stop and stem
    listQuery =
      listQuery.filter(a => !isStopWord(a)).map(a => PorterStemmer.stem(a).toLowerCase()).filter(a => !isStopWord(a))

    val idsToScores = new HashMap[Int, Double]

    for (word <- listQuery) {

      if (wordsToDocumentFrequencies.contains(word)) {

        for (doc <- wordsToDocumentFrequencies(word).keysIterator) {

          val tf: Double = wordsToDocumentFrequencies(word)(doc) / idsToMaxFreqs(doc)
          val idf: Double = {
            if (wordToInvFreq.contains(word)) {
              wordToInvFreq(word)
            } else {
              val n = Math.log(idsToTitle.size.toDouble / wordsToDocumentFrequencies(word).keys.size)
              wordToInvFreq(word) = n
              n
            }
          }

          if (idsToScores.contains(doc)) {
            idsToScores(doc) = idsToScores(doc) + (tf * idf)
          } else {
            idsToScores(doc) = tf * idf
          }

          if (this.usePageRank) {
            idsToScores(doc) = idsToScores(doc) * idsToPageRank(doc)
          }
        }
      }
    }

    // sort the final rankings
    val finalRankings: Array[Int] = idsToScores.keys.toArray.sortWith(idsToScores(_) > idsToScores(_))

    if (finalRankings.isEmpty) {
      println("No results, sorry!")
    } else {
      printResults(finalRankings)
    }
  }

  /**
   * Format and print up to 10 results from the results list
   *
   * @param results - an array of all results
   */
  private def printResults(results: Array[Int]) {
    for (i <- 0 until Math.min(10, results.size)) {
      println("\t" + (i + 1) + " " + idsToTitle(results(i)))
    }
  }

  def readFiles(): Unit = {
    FileIO.readTitles(titleIndex, idsToTitle)
    FileIO.readDocuments(documentIndex, idsToMaxFreqs, idsToPageRank)
    FileIO.readWords(wordIndex, wordsToDocumentFrequencies)
  }

  /**
   * Starts the read and print loop for queries
   */
  def run() {
    val inputReader = new BufferedReader(new InputStreamReader(System.in))

    // Print the first query prompt and read the first line of input
    print("search> ")
    var userQuery = inputReader.readLine()

    // Loop until there are no more input lines (EOF is reached)
    while (userQuery != null) {
      // If ":quit" is reached, exit the loop
      if (userQuery == ":quit") {
        inputReader.close()
        return
      }

      // Handle the query for the single line of input
      query(userQuery)

      // Print next query prompt and read next line of input
      print("search> ")
      userQuery = inputReader.readLine()
    }

    inputReader.close()
  }
}

object Query {
  def main(args: Array[String]) {
    try {
      // Run queries with page rank
      var pageRank = false
      var titleIndex = 0
      var docIndex = 1
      var wordIndex = 2
      if (args.size == 4 && args(0) == "--pagerank") {
        pageRank = true;
        titleIndex = 1
        docIndex = 2
        wordIndex = 3
      } else if (args.size != 3) {
        println("Incorrect arguments. Please use [--pagerank] <titleIndex> "
          + "<documentIndex> <wordIndex>")
        System.exit(1)
      }
      val query: Query = new Query(args(titleIndex), args(docIndex), args(wordIndex), pageRank)
      query.readFiles()
      query.run()
    } catch {
      case _: FileNotFoundException =>
        println("One (or more) of the files were not found")
      case _: IOException => println("Error: IO Exception")
    }
  }
}
