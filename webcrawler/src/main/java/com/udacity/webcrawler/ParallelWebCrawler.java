package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.parallelCrawlInternal;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;

  private final PageParserFactory parserFactory;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @MaxDepth int maxDepth,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls;
    this.maxDepth = maxDepth;
    this.parserFactory = parserFactory;
  }
  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> counts = Collections.synchronizedMap(new HashMap<>());
    Set<String> visitedUrls = new HashSet<>();

    for (String url : startingUrls) {
      pool.invoke(new parallelCrawlInternal(url, deadline, maxDepth, counts, visitedUrls,clock,parserFactory,ignoredUrls));

    }
    if (counts.isEmpty()) {
      return new CrawlResult.Builder().setWordCounts(counts).setUrlsVisited(visitedUrls.size()).build();
    }
    return new CrawlResult.Builder().setWordCounts(WordCounts.sort(counts, popularWordCount)).setUrlsVisited(visitedUrls.size()).build();
  }
  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

}
