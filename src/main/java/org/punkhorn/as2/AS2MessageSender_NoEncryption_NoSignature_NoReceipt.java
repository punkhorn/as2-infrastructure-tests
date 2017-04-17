package org.punkhorn.as2;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpClientConnectionFactory;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestDate;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;

public class AS2MessageSender_NoEncryption_NoSignature_NoReceipt {

    private static final String USER_AGENT = "AS2 Client";
    private static final String TARGET_HOSTNAME = "localhost";
    private static final int TARGET_PORT = 8888;
    private static final String AS2_NAME = "878051556";
    
    public static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            +"UNH+00000000000117+INVOIC:D:97B:UN'\n"
            +"BGM+380+342459+9'\n"
            +"DTM+3:20060515:102'\n"
            +"RFF+ON:521052'\n"
            +"NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            +"NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            +"CUX+1:USD'\n"
            +"LIN+1++157870:IN'\n"
            +"IMD+F++:::WIDGET'\n"
            +"QTY+47:1020:EA'\n"
            +"ALI+US'\n"
            +"MOA+203:1202.58'\n"
            +"PRI+INV:1.179'\n"
            +"LIN+2++157871:IN'\n"
            +"IMD+F++:::DIFFERENT WIDGET'\n"
            +"QTY+47:20:EA'\n"
            +"ALI+JP'\n"
            +"MOA+203:410'\n"
            +"PRI+INV:20.5'\n"
            +"UNS+S'\n"
            +"MOA+39:2137.58'\n"
            +"ALC+C+ABG'\n"
            +"MOA+8:525'\n"
            +"UNT+23+00000000000117'\n"
            +"UNZ+1+00000000000778'\n";
    
    private static HttpProcessor httpProcessor;
    private static HttpCoreContext httpcontext;
    private static DefaultBHttpClientConnection conn;
    private static HttpHost targetHost;
    private static BasicHttpEntityEnclosingRequest request;
    private static HttpResponse response;

    public static void main(String[] args) {
        sendAS2Message(args);
    }    
        
    public static void sendAS2Message(String[] args) {
        try {
            // Build HTTP Processor.
            buildHttpProcessor();
            
            // Build HTTP Context
            buildHttpContext();
            
            // Build HTTP Connection
            buildHttpConnection();
            
            // Create Request
            buildAS2RequestMessage();
            
            // Execute Request
            executeRequest();
            
            // Process Response
            processResponse();
            
        } catch (UnknownHostException e) {
            System.err.println("Attempted request to unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (InvalidAS2NameException e) {
            System.err.println("Invalid AS2 name: " + e.getMessage());
        } catch (HttpException e) {
            System.err.println("Unrecoverable HTTP protocol violation: " + e.getMessage());
        } finally {
            if (conn != null) { 
                try {
                    conn.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    public static void buildHttpProcessor() {
        httpProcessor = HttpProcessorBuilder.create()
                .add(new RequestTargetHost())
                .add(new RequestUserAgent(USER_AGENT))
                .add(new RequestDate())
                .add(new RequestContent())
                .add(new RequestConnControl())
                .add(new RequestExpectContinue(true)).build();
    }
    
    public static void buildHttpContext() {
        httpcontext = HttpCoreContext.create();
        targetHost = new HttpHost(TARGET_HOSTNAME, TARGET_PORT);
        httpcontext.setTargetHost(targetHost);
    }
    
    public static void buildHttpConnection() throws UnknownHostException, IOException {
        // Configure Connection
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setBufferSize(8 * 1024)
                .build();
        DefaultBHttpClientConnectionFactory connectionFactory = new DefaultBHttpClientConnectionFactory(connectionConfig);

        // Create Socket
        Socket socket = new Socket(targetHost.getHostName(), targetHost.getPort());

        // Create Connection
        conn = connectionFactory.createConnection(socket);
        
    }
    
    public static void executeRequest() throws HttpException, IOException {
        // Execute Request
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        httpexecutor.preProcess(request, httpProcessor, httpcontext);
        response = httpexecutor.execute(request, conn, httpcontext);
        httpexecutor.postProcess(response, httpProcessor, httpcontext);
    }
    
    public static void buildAS2RequestMessage() throws InvalidAS2NameException {
        request = new BasicHttpEntityEnclosingRequest("POST", "/");

        // Set Message Headers
        
        /* Host header */
        // Set by RequestTargetHost intercepter 
        
        /* User-Agent header */
        // Set by RequestUserAgent intercepter
        
        /* Date header */
        // Set by RequestDate intercepter
        // SHOULD be set to aid MDN in identifying the original message
        
        /* From header */
        // SHOULD be email address for user who controls user agen
        // Note: this is Optional header and should be enabled/disabled by user
        
        /* AS2-Version header */
        request.addHeader("AS2-Version", "1.1");

        /* AS2-From header */
        Util.validateAS2Name(AS2_NAME);
        request.addHeader("AS2-From", "878051556");

        /* AS2-To header */
        Util.validateAS2Name(AS2_NAME);
        request.addHeader("AS2-To", "878051556");
        
        /* Subject header */
        // SHOULD be set to aid MDN in identifying the original messaged
        request.addHeader("Subject", "Test Case");
        
        /* Message-Id header*/
        // SHOULD be set to aid in message reconciliation
        request.addHeader("Message-Id", Util.createMessageId("punkhorn.org"));
        
        /* Disposition-Notification-To header */
        //
        
        /* Disposition-Notification-Options header */
        //
        
        
        /* Content-Type header (Application/EDIFACT) */   
        request.addHeader("Content-Type", "Application/EDIFACT");

        /* Content-Length header added by request intercepter */
        // Set by RequestContent intercepter

        /* Content-Transfer-Encoding header */
        // Not required but permissible with encoding values of binary or 8-bit. 
        
        // Create Message Body
        /* EDI Message is Message Body */
        HttpEntity entity = new StringEntity(EDI_MESSAGE, ContentType.create("Application/EDIFACT", Consts.UTF_8));
        request.setEntity(entity);
    }
    
    public static void processResponse() throws ParseException, IOException {
        // Process Response
        int statusCode = response.getStatusLine().getStatusCode();
        String reasonPhrase = response.getStatusLine().getReasonPhrase();
        System.out.println("Received response with status code '" + statusCode + "' and reason phrase '" + reasonPhrase + "' : ");
        System.out.println("===================================================================================");

        // Print out headers
        for (Header header : response.getAllHeaders()) {
            System.out.println(header.getName() + " : " + header.getValue());
        }
        System.out.println();

        // Print Response Entity if any.
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String content = EntityUtils.toString(entity);
            System.out.println(content);
        }
        
        System.out.println("===================================================================================");
    }

}
