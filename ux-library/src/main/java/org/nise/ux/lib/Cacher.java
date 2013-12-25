package org.nise.ux.lib;

import java.util.EmptyStackException;
import java.util.HashMap;

public class Cacher<K, V> {
  // This Class uses:
  // 1. A HashMap to give best cache hit performance
  // 2. A rounded queue for history; to overwrite data objects after cache has been full!
  // >>>>> we handle changing results (like hot-trends) this way.
  private HashMap<K, V>                dataSet = new HashMap<K, V>();
  private RoundQueue<CacheHistoryData> history;

  public Cacher(int size, int segment) {
    history = new RoundQueue<CacheHistoryData>(size, segment);
  }

  public synchronized void putIntoCacher(V data, K query) {
    if (history.isFull()) {
      CacheHistoryData historyData = history.pop();
      dataSet.remove(historyData.query);
    }
    dataSet.put(query, data);
    history.push(new CacheHistoryData(query));
  }

  public V hitCacher(K query) {
    return dataSet.get(query);
  }

  public int getInCache() {
    return history.getCount();
  }

  private class CacheHistoryData {
    K    query;
    long time = System.currentTimeMillis();

    CacheHistoryData(K query) {
      this.query = query;
    }
  }

  public void clear() {
    dataSet.clear();
    history.clear();
  }

  class CacheTimeoutChecker extends Living {
    private long timeout;

    public CacheTimeoutChecker(long timeout) {
      super("CacheTimeoutChecker");
      this.timeout = timeout;
    }

    @Override
    protected void runtimeBehavior() throws Throwable {
      syncCheck();
      Thread.yield();
    }

    private synchronized void syncCheck() throws Throwable {
      long headTime = 0;
      try {
        headTime = System.currentTimeMillis() - history.head().time;
      } catch (EmptyStackException empse) {
        // Do Nothing. Queue is empty...
        Thread.sleep(timeout);
      }
      if (headTime > 0 && headTime > timeout) {
        CacheHistoryData historyData = history.pop();
        dataSet.remove(historyData.query);
      }
    }
  }
}