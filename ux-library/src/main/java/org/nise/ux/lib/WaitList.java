package org.nise.ux.lib;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

public class WaitList<V> {
  private final Object[][]    objects;
  private final Semaphore[][] locks;
  private final boolean[][]   in_use;
  private final int           limit;
  private final int           row_length;
  private int                 index = 0;
  private final Semaphore     in_use_lock;
  private final Semaphore     index_lock;
  private final Semaphore     reserve_lock;
  private final int           UP_BOUND_LIMIT;
  private final int           CUT_LIMIT;

  public WaitList(int limit, int row_length) {
    if (limit <= 0)
      throw new IllegalArgumentException("'limit' should be a nonzero positive value.");
    int rowCount = (limit - 1) / row_length + 1;
    objects = new Object[rowCount][row_length];
    locks = new Semaphore[rowCount][row_length];
    in_use = new boolean[rowCount][row_length];
    index_lock = new Semaphore(1);
    in_use_lock = new Semaphore(1);
    reserve_lock = new Semaphore(limit);
    UP_BOUND_LIMIT = Math.min(100 * limit, Integer.MAX_VALUE / 2);
    CUT_LIMIT = Math.max(2 * limit / 3, 70);
    for (int i = 0; i < limit; i++) {
      int array_offset = i % limit;
      int row = array_offset / row_length;
      int col = array_offset % row_length;
      locks[row][col] = new Semaphore(0);
      in_use[row][col] = false;
    }
    // initial static value
    this.limit = limit;
    this.row_length = row_length;
  }

  public WaitList(int limit) {
    this(limit, 300);
  }

  @SuppressWarnings("unchecked")
  public V get(int index) {
    try {
      int array_offset = index % limit;
      int row = array_offset / row_length;
      int col = array_offset % row_length;
      locks[row][col].acquire();
      Object obj = objects[row][col];
      objects[row][col] = null;
      Logger.getLogger(WaitList.class).trace("get(index): return=" + obj + " i=" + index + " row=" + row + " col=" + col);
      return (V) obj;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    }
  }

  public int put(V data) throws InterruptedException {
    int index = reserveIndex();
    put(data, index);
    return index;
  }

  public void put(V data, int index) {
    int array_offset = index % limit;
    int row = array_offset / row_length;
    int col = array_offset % row_length;
    objects[row][col] = data;
    locks[row][col].release();
    Logger.getLogger(WaitList.class).trace("put(data,index): data=" + data + " i=" + index + " row=" + row + " col=" + col);
  }

  public void releaseReserve(int index) throws InterruptedException {
    int array_offset = index % limit;
    int row = array_offset / row_length;
    int col = array_offset % row_length;
    in_use_lock.acquire();
    in_use[row][col] = false;
    in_use_lock.release();
    reserve_lock.release();
    Logger.getLogger(WaitList.class).trace("Index_Reserve released: i=" + index + " row=" + row + " col=" + col);
  }

  public int reserveIndex() throws InterruptedException {
    reserve_lock.acquire();
    try {
      int i = 0;
      index_lock.acquire();
      index++;
      int array_offset = index % limit;
      int row = array_offset / row_length;
      int col = array_offset % row_length;
      try {
        in_use_lock.acquire();
        while (in_use[row][col]) {
          index++;
          if (++i % CUT_LIMIT == 0) {
            Thread.yield();
          }
          array_offset = index % limit;
          row = array_offset / row_length;
          col = array_offset % row_length;
        }
        in_use[row][col] = true;
        in_use_lock.release();
      } catch (InterruptedException e) {
        index_lock.release();
        throw e;
      }
      if (index > UP_BOUND_LIMIT) {
        index = index % limit;
      }
      int index = this.index;
      index_lock.release();
      Logger.getLogger(WaitList.class).trace("index reserved: i=" + index + " row=" + row + " col=" + col);
      return index;
    } catch (InterruptedException e) {
      reserve_lock.release();
      throw e;
    }
  }
}