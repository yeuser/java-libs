package org.nise.ux.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class DoubleValuedSortedList<E> implements Iterable<E> {
  private Vector<DoubleValued<E>> vec = new Vector<DoubleValued<E>>();

  public void add(E e, double value) {
    DoubleValued<E> lv = new DoubleValued<E>(e, value);
    int i = findPlaceDsc(value);
    vec.add(i, lv);
  }

  public void clear() {
    vec.clear();
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return vec.size() > index;
      }

      @Override
      public E next() {
        return vec.elementAt(index++).e;
      }

      @Override
      public void remove() {
        // Do nothing.
      }
    };
  }

  private int findPlaceDsc(double value) {
    int i1 = 0, i2 = vec.size();
    while (true) {
      if (i2 == i1)
        return i1;
      if (i2 == i1 + 1) {
        if (vec.elementAt(i1).value > value)
          i1++;
        return i1;
      }
      int idx = (i1 + i2) / 2;
      if (idx > vec.size())
        return vec.size();
      if (vec.elementAt(idx).value < value)
        i2 = idx;
      else if (vec.elementAt(idx).value > value)
        i1 = idx + 1;
      else
        return idx;
    }
  }

  public E remove(int index) {
    return vec.remove(index).e;
  }

  public int size() {
    return vec.size();
  }

  public E elementAt(int index) {
    return vec.elementAt(index).e;
  }

  public double valueAt(int index) {
    return vec.elementAt(index).value;
  }

  public E firstElement() {
    return vec.firstElement().e;
  }

  public E lastElement() {
    return vec.lastElement().e;
  }

  public double maxValue() {
    return vec.firstElement().value;
  }

  public double minValue() {
    return vec.lastElement().value;
  }

  private class DoubleValued<T> {
    T      e;
    double value;

    public DoubleValued(T e, double value) {
      this.e = e;
      this.value = value;
    }

    @Override
    public String toString() {
      return e.toString() + ":" + value;
    }
  }

  public List<E> maxList(int n) {
    List<E> list = new ArrayList<E>();
    for (int i = 0; i < n && i < vec.size(); i++) {
      list.add(vec.get(i).e);
    }
    return list;
  }

  public List<E> minList(int n) {
    List<E> list = new ArrayList<E>();
    for (int i = vec.size() - 1; i >= 0 && i > vec.size() - n; i--) {
      list.add(vec.get(i).e);
    }
    return list;
  }
}