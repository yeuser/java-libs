package org.nise.ux.asl.run;

interface QueueFace<T> {
  T pop();

  boolean push(T t);
}