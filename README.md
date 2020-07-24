# INSTRUCTIONS
The use of our search engine is straightforward. There are two main parts--the indexer and querier--although most users will probably only interact with the querier.

- **Indexer:** First make sure you have three plain text files: titles.txt,
    words.txt, and docs.txt. You should also have a corpus stored in an XML file.
    To use the indexer, run the "main" method in the Indexing object within the
    Indexer file. The arguments to main, all in absolute filepath form, should be
    in the following order: the corpus file, titles.txt, words.txt, and docs.txt.
    Indexing will fill the three text files with the information needed to run
    queries.

  QUERIER: To run querier, run the main method in Query object within the
    QueryStencil file. The arguments should be in the following order:
    "--pagerank", followed by the absolute paths to titles.txt, docs.txt, and
    words.txt. "--pagerank" should only be included if you want the querier to
    consider the rank of the corpus pages. The query should result in up to 10
    results, a list of pages in order of relevance to the query.

OVERVIEW:
  Our program consists of two separate parts: the indexer and querier. The
  indexer is run before any queries, and extracts information from the corpus
  to make querying faster and more effective.

  INDEXER: The indexer removes stop words and stems all remaining words to
    ensure that only relevant terms are left over for the querier. The indexer
    then writes to three text files: titles.txt, words.txt, and docs.txt.

    Titles.txt contains a list of the ids of all pages in the corpus with the
    corresponding titles. Words.txt contains all words found in the corpus, with
    the number of times each word appears in each document. Docs.txt contains the
    maximum word count of each page as well as the page's rank.

  QUERIER: The querier reads from the three text files and records their data
    in several hashmaps. The querier takes in a string from the user, and splits
    it into separate terms, removing stop words and stemming. It then computes
    the relevance of each page in the corpus to each term in the query, using an
    algorithm based on the number of times each term appears in each page. It
    then combines this relevance score with each page's rank to compute its
    overall score.

    The querier records each page's overall score in a hashmap and prints a
    list of the top ten scoring pages to the user. If there are fewer than 10
    relevant pages, the querier prints fewer. If there are none, the querier
    prints "No results, sorry!"

FEATURES AND BUGS: The only "bug" we know of is that that our Indexer does not process BigWiki in under
    1 gigabyte of space(we think, we're not entirely certain if we allocated our Indexer enough space).
    No additional features.

TESTING: To test our program, we created several artifical wiki files.
    PageRankWiki: this file is in the src folder, and we ran the Indexer on this file to test if pageRank worked
    correctly, assigning document 100 a high score and the rest very small scores.

    Test1Wiki: this file also tested pageRank. It is just three docs which all link to each other (cyclically), and as
    a result they all have essentially the same score

    Test2Wiki: this file tested our relevance scoring in the Querier. After indexing this file, and searching the query
    "jazz", the results should be "most", "second most", "third most", least.

    Test3Wiki: The same as PageRankWiki, but in this case 7 links to itself many times. This should not change a thing
    in the calculation of ranks, and it doesn't.

    Test4Wiki: this file tested our handling of the words in links. It has the same words as Test2Wiki, but some of them
    are in pipe links, and some in normal links. This does not change the relevance scoring.

COLLABORATORS: Galen Winsor, Daniel Silverston


