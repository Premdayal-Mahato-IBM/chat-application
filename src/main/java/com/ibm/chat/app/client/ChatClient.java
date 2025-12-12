package com.ibm.chat.app.client;

import static java.util.Objects.nonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * The ChatClient class represents a simple chat client that connects to a server,
 * sends and receives messages.
 */
public class ChatClient {
	/**
     * Server address, default is "localhost".
     */
	private static String serverAddress = "localhost";
	
	/**
     * Server port, default is 9999.
     */
	private static int serverPort = 8888;

    /**
     * Main method to start the chat client.
     * 
     * @param args Command line arguments. 
     * 			   The first argument is the server address.
     *             The second (optional) is the server port.
     */
	public static void main(String[] args) {
		if (args.length > 0) {
			serverAddress = args[0];
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("The entered port is invaild. The default port: %d will be used.".formatted(serverPort));
			}
		}
		try (Socket cSocket = new Socket(serverAddress, serverPort);
				BufferedReader cbReader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
				PrintWriter cpWriter = new PrintWriter(cSocket.getOutputStream(), true);
				Scanner cScanner = new Scanner(System.in)) {
			System.out.print("Enter your username: ");
			String cUsername = cScanner.nextLine();
			cpWriter.println(cUsername); // Will be sent to server for registration
			System.out.println(getFormattedMessage());
			ChatClient cc = new ChatClient();
			cc.readMessage(cbReader, cUsername);
			cc.sendMessage(cScanner, cpWriter);
		} catch (Exception e) {
			System.err.println("Client error: " + e.getMessage());
		}
	}
	
	/**
	 * Added to format the help messages in the tabular format.
	 * 
	 * @return formatted string message
	 */
	private static String getFormattedMessage() {		
		StringBuilder message = new StringBuilder();
		message.append("--------------------------------------------------\n");
		message.append("Help:\n");
		message.append("--------------------------------------------------\n");
		message.append("->Type: "); 
		message.append("'online users' to check all the active users.\n");
		message.append("->Accepted Message Format:");
		message.append("[@<recipientUsername>: <Your Message>]");
	    
	    return message.toString();
	}

    /**
     * Main thread sends message from the user to the server.
     * 
     * @param cScanner Scanner to read user input.
     * @param cpWriter PrintWriter to write messages to the server.
     */
	private void sendMessage(Scanner cScanner, PrintWriter cpWriter) {
		String message;
		while (true) {
			message = cScanner.nextLine();
			if (message.equalsIgnoreCase("quit")) {
				break;
			}
			cpWriter.println(message);
		}
	}

    /**
     * Reads messages from the server and prints them.
     * 
     * @param cbReader BufferedReader to read messages from the server.
     * @param cUsername User's username to identify messages.
     */
	private void readMessage(BufferedReader cbReader, String cUsername) {
		new Thread(new MessageReaderTask(cbReader, cUsername)).start();
	}

    /**
     * Inner class to handle message reading in a separate thread.
     */
	private class MessageReaderTask implements Runnable {
		private BufferedReader cbReader;
		private String cUsername;

        /**
         * Constructor for MessageReaderTask.
         * 
         * @param cbReader BufferedReader to read messages from the server.
         * @param cUsername User's username.
         */
		MessageReaderTask(BufferedReader cbReader, String cUsername) {
			this.cbReader = cbReader;
			this.cUsername = cUsername;
		}

		@Override
		public void run() {
			try {
				String message;
				while (nonNull(message = cbReader.readLine())) {
					System.out.println(message);
				}
			} catch (IOException e) {
				System.err.println(cUsername + " has left the chat.");
			}
		}
	}
}
