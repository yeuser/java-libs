package org.nise.ux.asl.lib;

public class IndexedData<T> {
  public T   data;
  public int index;

  public IndexedData(T data, int index) {
    this.data = data;
    this.index = index;
  }
}