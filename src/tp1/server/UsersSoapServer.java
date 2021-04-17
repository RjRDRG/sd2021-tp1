package tp1.server;

import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;
import tp1.server.resources.UsersResource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class UsersSoapServer {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "UsersService";
    public static final String SOAP_USERS_PATH = "/soap/users";

    public static void main(String[] args) {
        try {
            String domain = args.length > 1 ? args[0] : "OutdatedPieceOfSht";

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format("http://%s:%s/soap", ip, PORT);

            HttpServer server = HttpServer.create(new InetSocketAddress(ip, PORT), 0);

            server. setExecutor(Executors.newCachedThreadPool());
            Endpoint soapUsersEndpoint = Endpoint.create(new UsersResource(domain, WebServiceType.SOAP));
            soapUsersEndpoint.publish(server.createContext (SOAP_USERS_PATH));
            server.start();
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
