package com.ibm.chat.app.server.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

/**
 * A thread class to handle client connections for a chat server. It reads
 * incoming messages, processes them, and sends them to the appropriate
 * recipients. It also handles user disconnections and cleanup.
 */
public class ConnectionHandler extends Thread {
	/**
	 * The client socket.
	 */
	private Socket cSocket;

	/**
	 * A map to store PrintWriter objects for each connected user.
	 */
	private Map<String, PrintWriter> cMappings; // extra

	/**
	 * PrintWriter for writing to the client socket.
	 */
	private PrintWriter cpWriter;

	/**
	 * BufferedReader for reading from the client socket.
	 */
	private BufferedReader cbReader;

	/**
	 * The username of the connected client.
	 */
	private String cUsername;

	/**
	 * Constructs a new ConnectionHandler with the given socket and mappings.
	 *
	 * @param cSocket   the client socket
	 * @param cMappings a map of usernames to PrintWriter objects
	 */
	public ConnectionHandler(Socket cSocket, Map<String, PrintWriter> cMappings) {
		this.cSocket = cSocket;
		this.cMappings = cMappings;
	}

	/**
	 * The main method run by the ConnectionHandler thread. It reads the username,
	 * registers it, processes messages, and handles cleanup on disconnection.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void run() {
		try {
			cbReader = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
			cpWriter = new PrintWriter(cSocket.getOutputStream(), true);

			// Step 1: Register cUsername to server.
			cUsername = cbReader.readLine();
			if (isNull(cUsername) || cUsername.isEmpty()) {
				return;
			}
			cMappings.put(cUsername, cpWriter);
			System.out.println(cUsername + " is availble for chat.");

			// Step 2: Process sent message to send to the right recipient.
			processSentMessage();

		} catch (IOException e) {
			System.out.println("Error handling client " + cUsername + ": " + e.getMessage());
		} finally {
			// 3. Clean up on disconnect
			performCleanup();
		}
	}

	/**
	 * Processes incoming messages, identifying recipients and forwarding messages
	 * accordingly.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	private void processSentMessage() throws IOException {
		String processSentMsg;
		while (nonNull((processSentMsg = cbReader.readLine()))) {
			System.out.println("Processing message: %s".formatted(processSentMsg));
			if (processSentMsg.startsWith("@") && processSentMsg.indexOf(':') > -1) {
				String recipientName = processSentMsg.substring(1, processSentMsg.indexOf(':'));
				String actualMessage = processSentMsg.substring(processSentMsg.indexOf(':') + 1);
				sendMessageToRecipient(recipientName, actualMessage);
			} else if (processSentMsg.contains("online users")) {
				sendOnlineUsersToRecipient(cUsername,
						cMappings.keySet().stream().filter(s -> !s.equals(cUsername)).collect(joining(", ")));
			} else {
				cpWriter.println(
						"[Warning]: Invalid message format. Please correct the format[eg: @recipientUsername: <Your Message>] "
								+ "and resend for delivery.");
			}
		}
	}

	/**
	 * Performs cleanup tasks when a client disconnects.
	 */
	private void performCleanup() {
		if (nonNull(cUsername)) {
			cMappings.remove(cUsername);
			System.out.println(cUsername + " has disconnected.");
		}
		try {
			cbReader.close();
			cpWriter.close();
			cSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a message to a specified recipient.
	 *
	 * @param recipientName the username of the recipient
	 * @param message       the message to send
	 */
	private void sendMessageToRecipient(String recipientName, String message) {
		PrintWriter recipientOut = cMappings.get(recipientName);
		if (nonNull(recipientOut)) {
			if (!cUsername.equals(recipientName)) {
				recipientOut.println(cUsername + ":" + message);
			} else {
				cpWriter.println("You can’t send messages to yourself.");
			}
		} else {
			cpWriter.println("[Warning]: User " + recipientName + " not found or offline.");
		}
	}

	/**
	 * Sends list of online users to a recipient.
	 *
	 * @param recipientName the username of the recipient
	 * @param message       the online users
	 */
	private void sendOnlineUsersToRecipient(String recipientName, String onlineUsers) {
		PrintWriter recipientOut = cMappings.get(recipientName);
		if (nonNull(recipientOut)) {
			cpWriter.println(onlineUsers);
		}
	}
}
