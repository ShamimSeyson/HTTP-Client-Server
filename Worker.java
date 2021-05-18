import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Worker extends Thread {
	Socket clientSocket;
	
	
	public Worker(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
	
	public void run() {
		try {
			
			//Create IO streams
			InputStream socketInput = clientSocket.getInputStream();
			OutputStream socketOutput = clientSocket.getOutputStream();
			

			//Strings for http responses
			String string400 = "";
			String string404 = "";
			String string200 = "";
			
			//Server name
			String serverName = "Shamim's Wonderful Server";
			
			//Print Client information (IP address and port number)
			System.out.println("Accepted client with IP address of " + clientSocket.getInetAddress().getHostAddress() + " and port number " + clientSocket.getPort() );
			
			//Create a date string
			Date currentDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
			String dateString = dateFormat.format(currentDate);
			
			
			//Program 2 Step 1: Parse the HTTP request
			
			String getReq = "";
			byte[] oneByte = new byte[1];
			String slashFile = "";
			String file = "";
			
																	//Run loop while there is not an empty line stored in getReq string
			while(getReq.contains("\r\n\r\n") == false) {
				socketInput.read(oneByte);							//Reads a byte from the socket input
				String byteString = new String(oneByte,"US-ASCII");	//Converts byte to string
				getReq = getReq + byteString;						//Appends the string of the byte to the end of the getReq string
			}
			
			System.out.println(getReq);								//Print get request
			
			String getReqLines[] = getReq.split("\r\n");			//Splits get request lines into string array
			String firstLineParts[] = getReqLines[0].split(" ");	//Splits first line into parts in string array
			//Program 2 Step 2: if format error then
			//if there is not "GET" in get request
			//or if HTTP version is not one of 1.0, 1.1 or 2.0
			//if there is no empty line in get request
			//if there is no Host line in get request
			if (getReqLines[0].contains("GET") == false
				|| (!firstLineParts[2].equals("HTTP/1.0")) && (!firstLineParts[2].equals("HTTP/1.1")) && (!firstLineParts[2].equals("HTTP/2.0"))
				|| getReq.contains("\r\n\r\n") == false
				|| getReq.contains("Host: ") == false) {

				
				//Program 2 Step 3: Send bad request error response
				string400 = "HTTP/1.1 400 Bad Request" + "\r\n"
						+ "Date: " + dateString + "\r\n"
						+ serverName + "\r\n"
						+ "Connection: close" + "\r\n"
						+ "\r\n";
				
				byte[] responseBytes = string400.getBytes("US-ASCII");		//Converts response string into bytes in a byte array
				
				System.out.println(string400);						//Print response string
				socketOutput.write(responseBytes);					//Writes response bytes to socket output
				socketOutput.flush();
			}

			else {
				slashFile = firstLineParts[1];						//string of file path with slash in front
				file = slashFile.substring(1);						//string of file path without slash in front
				Path filePath = Paths.get(file);
				//Program 2 Step 4: else if non-existence object then
				if(Files.notExists(filePath)) {
					//Program 2 Step 5: Send not found error response
					string404 = "HTTP/1.1 404 Not Found" + "\r\n"
								+ "Date: " + dateString + "\r\n"
								+ serverName + "\r\n"
								+ "Connection: close" + "\r\n"
								+ "\r\n";
					
					byte[] responseBytes = string404.getBytes("US-ASCII");		//Converts response string into bytes in a byte array
					
					System.out.println(string404);						//Print response string
					socketOutput.write(responseBytes);					//Writes response bytes to socket output
					socketOutput.flush();
				}
				//Program 2 Step 6: else
				else {
					
					long fileSize = Files.size(filePath);
					FileTime lastMod = Files.getLastModifiedTime(filePath);
					String contentType = Files.probeContentType(filePath);
					
					//Program 2 Step 7: Send Ok response header lines
					string200 = "HTTP/1.1 200 OK" + "\r\n"
							+ "Date: " + dateString + "\r\n"
							+ serverName + "\r\n"
							+ "Last-Modified: " + lastMod + "\r\n"
							+ "Content-Length: " + fileSize + "\r\n"
							+ "Content-Type: " + contentType + "\r\n"
							+ "Connection: close" + "\r\n"
							+ "\r\n";
					byte[] responseBytes = string200.getBytes("US-ASCII");		//Converts response string into bytes in a byte array
					
					System.out.println(string200);						//Print response string
					socketOutput.write(responseBytes);					//Writes response bytes to socket output
					socketOutput.flush();

					BufferedInputStream localInput = new BufferedInputStream(new FileInputStream(file)); //Create input stream for local file
					
					//Program 2 Step 8: Send the object content

					//Create buffers for output and for input
					boolean endOfBytes = false;
					int readBytes;
					int bufferSize = 10000;
					byte[] outputBuffer = new byte[bufferSize];
					
					//Read the local input file and write to the socket
					while(endOfBytes == false) {
						//Takes number of bytes read from buffer and checks that it is not end of file 
						if ((readBytes = localInput.read(outputBuffer)) != -1) {
							socketOutput.write(outputBuffer, 0, readBytes); //writes bytes from buffer to socketOutput
							socketOutput.flush();
						}
						//if end of file has been reached, sets endOfBytes to true
						else {
							endOfBytes = true;
							
							//Close the socket and clean up
							clientSocket.shutdownOutput();
							localInput.close();
							socketOutput.close();
							clientSocket.close();
						}
					}
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
