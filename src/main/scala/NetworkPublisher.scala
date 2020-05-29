import java.io._
import java.net.{InetAddress, ServerSocket, Socket, SocketException}
import java.util.Random

object NetworkPublisher {

  def main(args: Array[String]) {
    try {
      val ia = InetAddress.getByName("localhost")
      val socket = new Socket(ia, 9999)
      val out = new ObjectOutputStream(new DataOutputStream(socket.getOutputStream))
      val in = new DataInputStream(socket.getInputStream)

      (1 to 100).foreach { i =>
        Thread.sleep(1000)
        out.writeObject(i)
        out.flush()
      }

      out.close()
      in.close()
      socket.close()
    }
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

}
