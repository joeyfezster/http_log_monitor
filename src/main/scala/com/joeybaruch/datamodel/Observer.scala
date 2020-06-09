package com.joeybaruch.datamodel

trait Observer[S] {
  def receiveUpdate(subject: S);
}
