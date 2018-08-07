/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package passwordpagemonitor;

/**
 *
 * @author Matthew
 */
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Thread-safe "Windows authenticator" class for some web services.
 * 
 * @source http://stefanfrings.de/bfUtilities/javadoc/de/butterfly/auth/ThreadLocalAuthenticator.html
 * @author obssdeveloper
 *
 */
public class ThreadLocalAuthenticator extends Authenticator {
	private static final Logger logger = Logger.getLogger(ThreadLocalAuthenticator.class.getName());

	private static final ThreadLocal<String> proxy_password = new ThreadLocal<String>();
	private static final ThreadLocal<String> proxy_username = new ThreadLocal<String>();
	private static final ThreadLocal<String> server_password = new ThreadLocal<String>();
	private static final ThreadLocal<String> server_username = new ThreadLocal<String>();

	private static final ThreadLocalAuthenticator threadAuthenticator = new ThreadLocalAuthenticator();

	public static ThreadLocalAuthenticator getAuthenticator() {
		return threadAuthenticator;
	}

	public static void setAsDefault() {
		setDefault(threadAuthenticator);
	}

	public static void setProxyAuth(String username, String password) {
		proxy_username.set(username);
		proxy_password.set(password);
	}

	public static void setServerAuth(String username, String password) {
		server_username.set(username);
		server_password.set(password);
	}

	private ThreadLocalAuthenticator() {}

	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		String username = null;
		String password = null;
		if (getRequestorType() == RequestorType.PROXY) {
			username = proxy_username.get();
			password = proxy_password.get();
			String[] params = { this.getRequestingHost(), username };
			logger.log(Level.FINER, "Proxy auth for {0}: username={1}", params);
		}
		else if (getRequestorType() == RequestorType.SERVER) {
			username = server_username.get();
			password = server_password.get();
			String[] params = { this.getRequestingHost(), username };
			logger.log(Level.FINER, "Server auth for {0}: username={1}", params);
		}
		if (username == null || password == null) {
			return null;
		}
		else {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}
}