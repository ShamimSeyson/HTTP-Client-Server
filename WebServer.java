

/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * 
 *
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


public class WebServer extends Thread {
	
	// global logger object, configures in the driver class
	private static final Logger logger = Logger.getLogger("WebServer");
	
	ServerSocket serverSocket;
	ExecutorService pool;
	boolean shutdown = false;
	int port;
	
	
    /**
     * Constructor to initialize the web server
     * 
     * @param port 	The server port at which the web server listens > 1024
     * 
     */
	public WebServer(int port) {
		this.port = port;
	
		//Program 1 Step 2: Listen for connection requests from clients
		try {
			serverSocket = new ServerSocket(port);
			//serverSocket.setSoTimeout(5000);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
    /**
	 * Main web server method.
	 * The web server remains in listening mode 
	 * and accepts connection requests from clients 
	 * until the shutdown method is called.
	 *
     */
	public void run() {
		//Thread pool with dynamic pool size
		pool = Executors.newCachedThreadPool();

		try {
			
			//Program 1 Step 1: while not shutdown do
			while(shutdown == false) {

					//Program 1 Step 3: Accept a new connection request & Step 4: Spawn a worker thread to handle the new connection
					pool.execute(new Worker(serverSocket.accept()));
			//Program 1 Step 5: end while
			}
			

			
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}

	}
	

    /**
     * Signals the web server to shutdown.
	 *
     */
	public void shutdown() {
		shutdown = true;
		pool.shutdown();
		//Program 1 Step 6: Wait for worker threads to finish
		try {
			pool.awaitTermination(20, TimeUnit.SECONDS);
			
			pool.shutdownNow();

			//Program 1 Step 7: Close the server socket and clean up
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}
	
}
