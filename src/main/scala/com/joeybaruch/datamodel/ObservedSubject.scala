package com.joeybaruch.datamodel

trait ObservedSubject[S] {
  this: S =>
  private var observers: List[Observer[S]] = Nil

  def addObserver(observer: Observer[S]): Unit = observers = observer :: observers

  def notifyObservers(): Unit = observers.foreach(_.receiveUpdate(this))
}

