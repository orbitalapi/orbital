import io.vyne.utils.log
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory


/**
 * Echo websocket server for Testing purposes
 */
class TestWebSocketServer {

    /**
     * Store the messages received by the websocket
     */
    companion object {
        val messages = ArrayList<String>()
    }

    val server: Server = Server(0)
    var port: Int

    init {
        // Configure server
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        server.handler = context

        // Add websocket servlet
        val servlet = object : WebSocketServlet() {
            override fun configure(factory: WebSocketServletFactory) = factory.register(EchoWebSocket::class.java)
        }

        val wsHolder = ServletHolder("echo", servlet)
        context.addServlet(wsHolder, "/cask/*")

        // Finally, start
        server.start()

        // Save the port
        port = (server.connectors[0] as ServerConnector).localPort

    }

    val messagesReceived: ArrayList<String> = messages

    fun clearMessages() = messages.clear()
}

class EchoWebSocket : WebSocketListener {

    private lateinit var outbound: Session

    override fun onWebSocketClose(statusCode: Int, reason: String) {
        log().info("WebSocket Close: {} - {}", statusCode, reason)
    }

    override fun onWebSocketConnect(session: Session) {
        outbound = session
        log().info("WebSocket Connect: {}", session)
        outbound.getRemote().sendString("""{ "result": "SUCCESS", "message": "Good"}""", null)
    }

    override fun onWebSocketError(cause: Throwable) {
        log().warn("WebSocket Error", cause)
    }

    override fun onWebSocketText(message: String) {
        TestWebSocketServer.messages.add(message)

        if (outbound != null && outbound.isOpen()) {
            log().info("Echoing back text message [{}]", message)
            outbound.getRemote().sendString(message, null)
        }
    }

    override fun onWebSocketBinary(arg0: ByteArray?, arg1: Int, arg2: Int) {
        /* ignore */
    }

}

