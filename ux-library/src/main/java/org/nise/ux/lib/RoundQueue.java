package org.nise.ux.lib;

import java.util.EmptyStackException;
import java.util.concurrent.Semaphore;

/**
 * اين‫ thread يک Round Queue را پياده‌سازی می‌کند.
 * 
 * @param <V>
 */
public class RoundQueue<V> {
  private Object[][] objectQueue;
  private int        limit  = 10000;
  private int        row_length;
  private int        offset = 0;
  private int        end    = 0;
  private Semaphore  full, empty, lock = new Semaphore(1);

  /**
   * يک ‫Round Queue ايجاد می‌کند
   * 
   * @param limit
   * @param row_length
   */
  public RoundQueue(int limit, int row_length) {
    if (limit <= 0)
      throw new IllegalArgumentException("'limit' should be a nonzero positive value.");
    objectQueue = new Object[(limit - 1) / row_length + 1][row_length];
    // initial static value
    this.limit = limit;
    this.row_length = row_length;
    full = new Semaphore(limit);
    empty = new Semaphore(limit);
    try {
      empty.acquire(limit);
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(-100);
    }
  }

  public RoundQueue(int limit) {
    this(limit, 300);
  }

  /**
   * برای وارد کردن داده به صف استفاده می‌شود‫.
   * 
   * @param data
   */
  public void push(V data) {
    if (isFull())
      throw new StackOverflowError();
    objectQueue[(end % limit) / row_length][(end % limit) % row_length] = data;
    end++;
  }

  public int getCount() {
    return end - offset;
  }

  /**
   * برای واکشی داده به صورت ‫sync از صف استفاده می‌شود‫.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public V syncPop() {
    try {
      empty.acquire();
      lock.acquire();
      Object obj = objectQueue[(offset % limit) / row_length][(offset % limit) % row_length];
      offset++;
      lock.release();
      full.release();
      return (V) obj;
    } catch (InterruptedException e) {
      return null;
    }
  }

  /**
   * برای وارد کردن داده به صورت ‫sync به صف استفاده می‌شود‫.
   * 
   * @param data
   * @return
   */
  public boolean syncPush(V data) {
    try {
      full.acquire();
      lock.acquire();
      objectQueue[(end % limit) / row_length][(end % limit) % row_length] = data;
      end++;
      lock.release();
      empty.release();
      return true;
    } catch (InterruptedException e) {
    }
    return false;
  }

  /**
   * برای واکشی داده از صف استفاده می‌شود‫.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public V pop() {
    if (isEmpty())
      throw new EmptyStackException();
    Object obj = objectQueue[(offset % limit) / row_length][(offset % limit) % row_length];
    offset++;
    return (V) obj;
  }

  /*
   * Returns first object to be popped using pop() function.
   */
  @SuppressWarnings("unchecked")
  public V head() {
    if (isEmpty())
      throw new EmptyStackException();
    Object obj = objectQueue[(offset % limit) / row_length][(offset % limit) % row_length];
    return (V) obj;
  }

  public boolean isEmpty() {
    return (end == offset);
  }

  public boolean isFull() {
    return (end == offset + limit);
  }

  public void clear() {
    offset = end = 0;
  }

  public int getLimit() {
    return limit;
  }
}