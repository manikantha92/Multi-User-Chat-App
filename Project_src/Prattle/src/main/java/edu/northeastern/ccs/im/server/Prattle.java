package edu.northeastern.ccs.im.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Queue;
import java.util.List;


import edu.northeastern.ccs.im.Message;

/**
 * A network server that communicates with IM clients that connect to it. This
 * version of the server spawns a new thread to handle each client that connects
 * to it. At this point, messages are broadcast to all of the other clients.
 * It does not send a response when the user has gone off-line.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 *
 * @version 1.3
 */
public abstract class Prattle {

	/** Amount of time we should wait for a signal to arrive. */
	private static final int DELAY_IN_MS = 50;

	/** Number of threads available in our thread pool. */
	private static final int THREAD_POOL_SIZE = 20;

	/** Delay between times the thread pool runs the client check. */
	private static final int CLIENT_CHECK_DELAY = 200;

	/** Collection of threads that are currently being used. */
	private static Queue<ClientRunnable> active;

	/**
	 * String message for error.
	 */
	private static final String EXCEPTION_MESSAGE = "Exception Occurred";

	private static final Logger LOGGER = Logger.getLogger(
			Thread.currentThread().getStackTrace()[0].getClassName() );

	// All of the static initialization occurs in this "method"
	static {
		// Create the new queue of active threads.
		active = new ConcurrentLinkedQueue<>();
	}

	public static Queue<ClientRunnable> getActiveConnection(){
		return active;
	}

	public static void setActiveConnection(ClientRunnable client) {
		active.add(client);
	}

	/**
	 * Broadcast a given message to all the other IM clients currently on the
	 * system. This message _will_ be sent to the client who originally sent it.
	 *
	 * @param message Message that the client sent.
	 */
	public static void broadcastMessage(Message message) {
		// Loop through all of our active threads
		for (ClientRunnable tt : active) {
			// Do not send the message to any clients that are not ready to receive it.
			if (tt.isInitialized() && (!tt.isAgency())) {
				tt.enqueueMessage(message);
			}
			if (userHasAgency(tt.getName())) {
				sentToAgencyGivenTargetName(message, tt.getName());
			}
		}


	}


	/**
				usersAvailable.remove(tt.getName());
				usersAvailable.put(tt.getName(), (tt.isInitialized() && users.contains(tt.getName())));
	 * Send given Group message to all other users in that Group
	 * @param message message to sent to group members
	 * @param users users in group
	 * @return
	 */
	public static HashMap<String, Boolean> sendOutGroupMessage(Message message, List<String> users) {
		// Loop through all active threads to find users in group to send message to
		HashMap<String, Boolean> usersAvailable = new HashMap<String, Boolean>();
		for(String user : users) {
			usersAvailable.put(user, false);
		}

		// Check if Group has an agency, is so, send copy of message to Agency
		if (userHasAgency(message.getReceiver(message.getText()))) {
			sentToAgencyGivenTargetName(message, message.getReceiver(message.getText()));
		}

		for (ClientRunnable tt: active) {
			// Do not send the message to any clients that are not ready to receive it.
			if (tt.isInitialized() && users.contains(tt.getName()) && (!tt.isAgency())) {
				// TO DO: Mani - check if this is correct:*****************************************************************
				usersAvailable.put(tt.getName(), true);

				tt.enqueueMessage(message);
				// Check if active users have an agency, if so, copy of message to Agency
				if (userHasAgency(tt.getName())) {
					sentToAgencyGivenTargetName(message, tt.getName());
				}
			}
		}
		return usersAvailable;
	}

	/**
	 * Send Message to particular receiver.
	 * @param message Message from client to be sent to receiver
	 */
	public static boolean sendOutUserMessage(Message message) {
		// Loop through all of our active threads
		boolean flag=false;
		for (ClientRunnable tt : active) {
			// Do not send the message to any clients that are not ready to receive it.
			flag=(tt.isInitialized() && tt.getName().equals(message.getReceiver(message.getText())) && (!tt.isAgency()));
			if (flag) {
				tt.enqueueMessage(message);
				//return flag;
			}
		}

		if (senderHasAgency(message)) {
			sentToAgencyGivenTargetName(message, message.getName());
		}

		if (receiverHasAgency(message)) {
			sentToAgencyGivenTargetName(message, message.getReceiver(message.getText()));
		}
		return flag;
	}


	/**
	 * Create a hashMap that contains list of Agencies and their target
	 * @return HashMap containing list of Agencies and their target
	 */
	public static HashMap<String, String> getAgencyMap () {
		//Create agency list
		HashMap<String, String> agencyList = new HashMap<>();
		for (ClientRunnable tt : active) {
			if (tt.isAgency()) {
				agencyList.put(tt.getName(), tt.getTarget());
			}
		}
		return agencyList;
	}

	/**
	 * Check if user has an Agency.
	 * @param userName username of user
	 * @return True if user has an Agnecy.
	 */
	public static boolean userHasAgency(String userName) {
		String agencyName = "AGENCY" + userName;
		HashMap<String, String> agencyList = getAgencyMap();
		return agencyList.containsKey(agencyName);

	}

	/**
	 * Check if message sender has an Agency
	 * @param message message containing sender name
	 * @return True if sender has an Agency, false otherwise.
	 */
	public static boolean senderHasAgency(Message message) {
		HashMap<String, String> agencyList = getAgencyMap();
		String agencyName = "AGENCY" + message.getName();
		return agencyList.containsKey(agencyName);
	}

	/**
	 * Check if message receiver has an Agency
	 * @param message message containing receiver name
	 * @return True if receiver had an Agency, false otherwise.
	 */
	public static boolean receiverHasAgency(Message message) {
		HashMap<String, String> agencyList = getAgencyMap();
		String agencyName = "AGENCY" + message.getReceiver(message.getText());
		return agencyList.containsKey(agencyName);
	}

	/**
	 * If message sender has an Agency, send Copy of message to agency.
	 * @param message
	 */
	public static void sentToAgencyGivenTargetName(Message message, String targetName) {
		String agencyName = "AGENCY" + targetName;
		for (ClientRunnable tt: active) {
			if (tt.getName().equals(agencyName)) {
				tt.enqueueMessage(message);
			}
		}
	}

	/**
	 * Start up the threaded talk server. This class accepts incoming connections on
	 * a specific port specified on the command-line. Whenever it receives a new
	 * connection, it will spawn a thread to perform all of the I/O with that
	 * client. This class relies on the server not receiving too many requests -- it
	 * does not include any code to limit the number of extant threads.
	 *
	 * @param args String arguments to the server from the command line. At present
	 *             the only legal (and required) argument is the port on which this
	 *             server should list.
	 * @throws IOException Exception thrown if the server cannot connect to the port
	 *                     to which it is supposed to listen.
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		// Connect to the socket on the appropriate port to which this server connects.
		try(ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
			serverSocket.configureBlocking(false);
			serverSocket.socket().bind(new InetSocketAddress(ServerConstants.PORT));
			// Create the Selector with which our channel is registered.
			Selector selector = SelectorProvider.provider().openSelector();
			// Register to receive any incoming connection messages.
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			// Create our pool of threads on which we will execute.
			ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
			startListen(selector,serverSocket,threadPool);
		}

	}

	public static void startListen(Selector selector, ServerSocketChannel serverSocket, ScheduledExecutorService threadPool) {
		// Listen on this port until ...
		boolean done = false;
		while (!done) {
			// Check if we have a valid incoming request, but limit the time we may wait.
			try {
				while (selector.select(DELAY_IN_MS) != 0) {
					// Get the list of keys that have arrived since our last check
					Set<SelectionKey> acceptKeys = selector.selectedKeys();
					// Now iterate through all of the keys
					Iterator<SelectionKey> it = acceptKeys.iterator();
					while (it.hasNext()) {
						// Get the next key; it had better be from a new incoming connection
						SelectionKey key = it.next();
						it.remove();
						// Assert certain things I really hope is true
						assert key.isAcceptable();
						if(key.channel() != serverSocket) {
							throw new IllegalStateException("key.channel != serverSocket");
						}
						// Create a new thread to handle the client for which we just received a
						// request.
						newThread(serverSocket, threadPool);
					}
				}

			} catch (IOException e) {
				LOGGER.log(Level.SEVERE,EXCEPTION_MESSAGE, e);

			}
		}
	}


	private static void newThread(ServerSocketChannel serverSocket, ScheduledExecutorService threadPool) {
		try {
			// Accept the connection and create a new thread to handle this client.
			SocketChannel socket = serverSocket.accept();
			// Make sure we have a connection to work with.
			if (socket != null) {
				ClientRunnable tt = new ClientRunnable(socket);
				// Add the thread to the queue of active threads
				active.add(tt);
				// Have the client executed by our pool of threads.
				@SuppressWarnings("rawtypes")
				ScheduledFuture clientFuture = threadPool.scheduleAtFixedRate(tt, CLIENT_CHECK_DELAY,
								CLIENT_CHECK_DELAY, TimeUnit.MILLISECONDS);
				tt.setFuture(clientFuture);
				//write the jdbc logic starting from here

			}
		} catch (AssertionError | Exception ae) {
			LOGGER.log(Level.SEVERE, EXCEPTION_MESSAGE, ae);
		}
	}

	/**
	 * Remove the given IM client from the list of active threads.
	 *
	 * @param dead Thread which had been handling all the I/O for a client who has
	 *             since quit.
	 */
	public static void removeClient(ClientRunnable dead) {
		// Test and see if the thread was in our list of active clients so that we
		// can remove it.
		if (!active.remove(dead)) {
			//delete
			LOGGER.log(Level.SEVERE, "Could not find a thread that I tried to remove!\n");
		}
	}
}