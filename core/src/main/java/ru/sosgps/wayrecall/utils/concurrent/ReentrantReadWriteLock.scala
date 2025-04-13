package ru.sosgps.wayrecall.utils.concurrent



/**
 * Created by nickl on 16.12.14.
 */
class ReentrantReadWriteLock(fairness: Boolean = false) extends java.util.concurrent.locks.ReentrantReadWriteLock(fairness){

  def withRead[T](f: => T): T = {
    val l = this.readLock()
    l.lock()
    try {
      f
    }
    finally {
      l.unlock()
    }
  }

  def withWrite[T](f: => T): T = {
    val l = this.writeLock()
    l.lock()
    try {
      f
    }
    finally {
      l.unlock()
    }
  }

}

class ReentrantLock(fairness: Boolean = false) extends java.util.concurrent.locks.ReentrantLock(fairness){

  def locked[T](f: => T): T = {
    lock()
    try {
      f
    }
    finally {
      unlock()
    }
  }

  def tryLocked[T](f: => T): Option[T] = {
    if (tryLock()) {
      try {
        Some(f)
      }
      finally {
        unlock()
      }
    }
    else
      None
  }

}