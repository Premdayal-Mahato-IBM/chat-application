package com.ibm.chat.app.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.chat.app.server.handler.ConnectionHandler;

/**
 * The main class for the chat server. It listens for incoming client
 * connections on a specified port and spawns a new thread for each client
 * connection.
 */
public class ChatServer {
	/**
     * Server port, default is 9999.
     */
	private static int serverPort = 8888;

	/**
	 * A map to store client names and their corresponding PrintWriters for one-to-one communication.
	 */
	private static final Map<String, PrintWriter> clientsMap = new ConcurrentHashMap<>();

	/**
	 * Main method to start the chat server.
	 * 
	 * @throws IOException if an I/O error occurs while starting the server.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			try {
				serverPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("The entered port is invaild. The default port: %d will be used.".formatted(serverPort));
			}
		}
		System.out.println("Chat server started on port " + serverPort);
		try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new ConnectionHandler(clientSocket, clientsMap).start();
			}
		}
	}
}
