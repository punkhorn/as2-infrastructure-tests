package org.punkhorn.as2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronousReceiver {
    
    private static final Logger LOG = LoggerFactory.getLogger(SynchronousReceiver.class);

    private static final String REQUEST_LISTENER_THREAD_NAME_PREFIX = "AS2 Rcvr - ";
    private static final String REQUEST_HANDLER_THREAD_NAME_PREFIX = "AS2 Hdlr - ";

    private static final String ORIGIN_SERVER_NAME = "AS2 Server";
    
    public static void main(String[] args) throws InterruptedException, IOException {
        httpService(args);
    }

    public static void httpService(String[] args) throws IOException {
        final Thread t = new RequestListenerThread(8888);
        t.setDaemon(false);
        t.start();
        
    }

    static class RequestListenerThread extends Thread {

        private final ServerSocket serversocket;
        private final HttpService httpService;
        
        public RequestListenerThread(int port) throws IOException {
            setName(REQUEST_LISTENER_THREAD_NAME_PREFIX + port);
            serversocket = new ServerSocket(port);
            
            // Set up HTTP protocol processor for incoming connections
            final HttpProcessor inhttpproc = new ImmutableHttpProcessor(
                    new HttpResponseInterceptor[] {
                            new ResponseContent(true),
                            new ResponseServer(ORIGIN_SERVER_NAME),
                            new ResponseDate(),
                            new ResponseConnControl()
                    });

            // Set up incoming request handler
            final UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
            reqistry.register("*", new RequestHandler());

            // Set up the HTTP service
            httpService = new HttpService(inhttpproc, reqistry);
        }
        
        @Override
        public void run() {
            LOG.info("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    final int bufsize = 8 * 1024;
                    // Set up incoming HTTP connection
                    final Socket insocket = this.serversocket.accept();
                    final DefaultBHttpServerConnection inconn = new DefaultBHttpServerConnection(bufsize);
                    LOG.info("Incoming connection from " + insocket.getInetAddress());
                    inconn.bind(insocket);

                   // Start worker thread
                    final Thread t = new RequestHandlerThread(this.httpService, inconn);
                    t.setDaemon(true);
                    t.start();
                } catch (final InterruptedIOException ex) {
                    break;
                } catch (final IOException e) {
                    LOG.error("I/O error initialising connection thread: "
                            + e.getMessage());
                    break;
                }
            }
        }
    }
    
    static class RequestHandlerThread extends Thread {
        private HttpService httpService;
        private HttpServerConnection serverConnection;

        public RequestHandlerThread(HttpService httpService, HttpServerConnection serverConnection) {
            if (serverConnection instanceof HttpInetConnection) {
                HttpInetConnection inetConnection = (HttpInetConnection) serverConnection;
                setName(REQUEST_HANDLER_THREAD_NAME_PREFIX + inetConnection.getLocalPort());
            } else {
                setName(REQUEST_HANDLER_THREAD_NAME_PREFIX + getId());
            }
            this.httpService = httpService;
            this.serverConnection = serverConnection;
        }
        
        @Override
        public void run() {
            LOG.info("New connection thread");
            final HttpContext context = new BasicHttpContext(null);

            try {
                while (!Thread.interrupted()) {

                    this.httpService.handleRequest(this.serverConnection, context);

                }
            } catch (final ConnectionClosedException ex) {
                LOG.info("Client closed connection");
            } catch (final IOException ex) {
                LOG.error("I/O error: " + ex.getMessage());
            } catch (final HttpException ex) {
                LOG.error("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.serverConnection.shutdown();
                } catch (final IOException ignore) {}
            }
        }
    }

    static class RequestHandler implements HttpRequestHandler {

        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            String method = request.getRequestLine().getMethod();
            String uri = request.getRequestLine().getUri();

            StringBuffer buffer = new StringBuffer();
            LOG.info("Received " + method + " request for resource '" + uri + "'");
            buffer.append("Request Message: \n");
            buffer.append("===================================================================================\n");
            
            // Print Request Line
            buffer.append(request.getRequestLine() + "\n");
            
            // Print Request Headers
            for (Header header : request.getAllHeaders()) {
                buffer.append(header + "\n");
            }
            buffer.append("\n");

            // Print any Request Entity
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) request;
                HttpEntity entity = entityEnclosingRequest.getEntity();
                String content = EntityUtils.toString(entity);
                buffer.append(content + "\n");
            }
            buffer.append("===================================================================================\n");
            LOG.debug(buffer.toString());
            
            // Build Response
            HttpEntity entity = new StringEntity(method + " response for resource '" + uri + "'", ContentType.create("text/plain", Consts.UTF_8));
            response.setEntity(entity);

        }

    }
}
