package com.joeybaruch.datamodel

trait WindowedEventsMonoid[A]  {
  def empty: A

  def combine(AggregatedMetrics1: A, AggregatedMetrics2: A): A

  def aggregate(seq: Seq[A]): A = seq.foldLeft(empty)(combine)
}
