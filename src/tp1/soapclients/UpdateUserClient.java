package tp1.soapclients;

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.SoapException;


public class UpdateUserClient {

	public final static String USERS_WSDL = "/users/?wsdl";
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;

	public static void main(String[] args) throws IOException {

		if( args.length != 6) {
			System.err.println( "Use: java sd2021.aula2.clients.UpdateUserClient url userId oldpwd fullName email password");
			return;
		}

		String serverUrl = args[0];
		String userId = args[1];
		String oldpwd = args[2];
		String fullName = args[3];
		String email = args[4];
		String password = args[5];

		User u = new User( userId, fullName, email, password);

		//Obtaining s stub for the remote soap service
		
		SoapUsers users = null;
		
		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create( new URL(serverUrl + USERS_WSDL), QNAME );
			users = service.getPort( SoapUsers.class );
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		}
		
		//Set timeouts for executing operations
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
	
		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while(!success && retries < MAX_RETRIES) {

			try {
				User u2 = users.updateUser(userId, oldpwd, u);
				System.out.println("Successfully updated information for user " + u2.getUserId());
				success = true;
			} catch (SoapException e) {
				System.out.println("Cound not update user: " + e.getMessage());
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
	}

}
