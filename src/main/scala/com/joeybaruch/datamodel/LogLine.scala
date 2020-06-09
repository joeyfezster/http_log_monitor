package com.joeybaruch.datamodel


trait LogLine {}

case class Headers(headers: List[String]) extends LogLine

case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)


trait LogEvent extends LogLine with Ordered[LogEvent] {
  val host: String
  val rfc931: String
  val authUser: String
  val timestamp: Long
  val request: Request
  val status: String
  val bytes: Int

  def as[T](implicit f: LogEvent => T): T = f(this)

  // negating the compareTo result of two positive numbers under the this compared to that pattern will result in ascending order
  override def compare(that: LogEvent): Int = -this.timestamp.compareTo(that.timestamp)
}


case class LogEventImpl(host: String,
                        rfc931: String,
                        authUser: String,
                        timestamp: Long,
                        request: Request,
                        status: String,
                        bytes: Int) extends LogEvent

object LogEvent {

  case object SentinelEOFEvent extends LogEvent {
    //todo: review this paradigm - pattern matching vs implementing unnecessary functions
    private val emptyStr = ""
    private val emptyReq = Request(emptyStr, emptyStr, None, emptyStr)
    override val host: String = emptyStr
    override val rfc931: String = emptyStr
    override val authUser: String = emptyStr
    override val timestamp: Long = 0L
    override val request: Request = emptyReq
    override val status: String = emptyStr
    override val bytes: Int = 0
  }

}

case class UnparsableEvent(string: Seq[String]) extends LogLine
