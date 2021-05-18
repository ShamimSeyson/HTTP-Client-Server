


import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class HttpClient {

	private static final Logger logger = Logger.getLogger("HttpClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public HttpClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void get(String url) {
		try {
			
			//Step 1: Parse the input url to extract server address and object path
			String serverName;
			int serverPort = 80;
			String filepath;
			String parts[] = url.split("/", 2);						//Splits host name (with optional port number) and file path
			//Checks to see if url contains the optional port number
			if (parts[0].contains(":")) {
				String hostAndPort[] = parts[0].split(":", 2);
				serverName = hostAndPort[0];
				serverPort = Integer.parseInt(hostAndPort[1]);		//Extracts server port number converts to integer
				filepath = "/" + parts[1];							//Adds "/" to beginning of file path string
			}
			//if url does not contain the optional port number
			else {
				serverName = parts[0];
				filepath = "/" + parts [1];							//Adds "/" to beginning of file path string
			}

			//Step 2: Establish a TCP connection with the server
			Socket socket = new Socket(serverName, serverPort);
			
			//Step 3: Send a GET request for the specified object
			OutputStream socketOutput = socket.getOutputStream();
			String getReqString = "GET " + filepath + " HTTP/1.1\r\n" + "Host: " + serverName + ":" + serverPort + "\r\n\r\n";
			byte[] getReqBytes = getReqString.getBytes("US-ASCII");		//Converts get request string into bytes in a byte array
			
			System.out.println(getReqString);						//Print get request string
			socketOutput.write(getReqBytes);						//Writes get request bytes to socket output
			socketOutput.flush();

			
			//Step 4: Read the server response status and header lines
			InputStream socketInput = socket.getInputStream();
			String header = "";
			byte[] oneByte = new byte[1];
			//Run loop while there is not an empty line stored in header string
			while(header.contains("\r\n\r\n") == false) {
				socketInput.read(oneByte);							//Reads a byte from the socket input
				String byteString = new String(oneByte,"US-ASCII");	//Converts byte to string
				header = header + byteString;						//Appends the string of the byte to the end of the header string
			}
			//Print header of response message
			System.out.println(header);
			
			
			//Step 5: if response status is OK then
			String headerParts[] = header.split("\r\n");			//Split header string into individual header lines
			//Check if first line of header contains "200 OK" message
			if (headerParts[0].contains("200 OK")) {
				
				//Step 6: Create the local file with the object name
				String[] pathArray = filepath.split("/");			//Splits filepath by individual directories
				String objectName = pathArray[pathArray.length - 1];	//Takes file name from last element of pathArray
				FileOutputStream localOutput = new FileOutputStream(objectName);	//Create local output stream with file name
				
				
				int objectSize = 0;
				//Iterate through each line of the header
				for (String line: headerParts) {
					//Identify Content-Length line
					if (line.contains("Content-Length:")) {
						String contentLength[] = line.split(" ");
						//Extract the object size from the Content-Length line
						objectSize = Integer.parseInt(contentLength[1]);
					}
				}
				
				//Step 7: while not end of input stream do
				
				byte[] bufferBytes = new byte[32 * 1024];
				int numBytes;
				int downloadedBytes = 0;
				//While loop in case there is no Content-Length in header
				if (objectSize == 0) {
					//Step 8: Read from the socket and write to the local file
					//loop condition checks if bytes read from socket are not the end of file
					while ((numBytes = socketInput.read(bufferBytes)) != -1) {
						//writes bytes from buffer to file output stream
						localOutput.write(bufferBytes, 0, numBytes);
						localOutput.flush();
					}
				}
				//While loop in case there is Content-Length in header, which performs faster download
				else {
					while (downloadedBytes < objectSize) {
						//Step 8: Read from the socket and write to the local file
						numBytes = socketInput.read(bufferBytes);	//reads bytes from socket and puts them in buffer
						localOutput.write(bufferBytes, 0, numBytes);	//writes bytes from buffer to the file output stream
						localOutput.flush();
						downloadedBytes += numBytes;				//Updates downloaded bytes number by adding bytes from current loop iteration
					}
					//Exception handling in case not all bytes of file were downloaded 
					if (downloadedBytes != objectSize) {
						System.out.println("The file has not downloaded correctly.");
						System.exit(1);
					}
				}
				//Step 9: end while
				
				
				
				//Step: Clean up (e.g., close the streams and socket)
				socket.shutdownOutput();
				socketInput.close();
				localOutput.close();
				socket.close();
				
				
			}
			
			
			
			//Exception handling for when file does not exist in a url file path
			else {
				System.out.println("An exception has occurred. Perhaps the file path in the url provided does not exist. Try again.");
				System.exit(1);
			}
			
		}
		//Exception handling
		catch(Exception e) {
			System.out.println("An exception has occurred: " + e.getMessage());
			System.exit(1);
		}
	}

}
