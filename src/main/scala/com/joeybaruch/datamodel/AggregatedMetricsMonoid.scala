package com.joeybaruch.datamodel

trait AggregatedMetricsMonoid[A]  {
  def empty: A

  def combine(aggMetrics1: A, aggMetrics2: A): A

  def aggregate(seq: Seq[A]): A = seq.foldLeft(empty)(combine)
}
