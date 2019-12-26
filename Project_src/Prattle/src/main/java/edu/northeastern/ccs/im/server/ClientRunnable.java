package edu.northeastern.ccs.im.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.PrintNetNB;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.dao.GroupMessageDao;
import edu.northeastern.ccs.im.dao.GroupsDao;
import edu.northeastern.ccs.im.dao.MessageDao;
import edu.northeastern.ccs.im.dao.UserDao;
import edu.northeastern.ccs.im.model.GroupMessage;
import edu.northeastern.ccs.im.model.Groups;
import edu.northeastern.ccs.stemmer.DiceCoefficient;
import edu.northeastern.ccs.stemmer.Stemmer;
import edu.northeastern.ccs.im.MyLogger;


/**
 * Instances of this class handle all of the incoming communication from a
 * single IM client. Instances are created when the client signs-on with the
 * server. After instantiation, it is executed periodically on one of the
 * threads from the thread pool and will stop being run only when the client
 * signs off.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 *
 * @version 1.3
 */
public class ClientRunnable implements Runnable {
	/**
	 * Number of milliseconds that special responses are delayed before being sent.
	 */
	private static final int SPECIAL_RESPONSE_DELAY_IN_MS = 5000;

	/**
	 * Number of milliseconds after which we terminate a client due to inactivity.
	 * This is currently equal to 5 hours.
	 */
	private static final long TERMINATE_AFTER_INACTIVE_BUT_LOGGEDIN_IN_MS = 18000000;

	/**
	 * Number of milliseconds after which we terminate a client due to inactivity.
	 * This is currently equal to 5 hours.
	 */
	private static final long TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS = 600000;

	private static final String SUCCESS = "success";
	private static final String ERROR = "Error : ";
	private static final String SERVER_SUCCESS = "Success : ";
	private static final String SUCCESSFULLY = " successfully";

	/**
	 * String message for error.
	 */
	private static final String EXCEPTION_MESSAGE = "Exception Occurred";

	/**
	 * Time at which we should send a response to the (private) messages we were
	 * sent.
	 */
	private Date sendResponses;

	/** Time at which the client should be terminated due to lack of activity. */
	private GregorianCalendar terminateInactivityTime;

	/** Queue of special Messages that we must send immediately. */
	private Queue<Message> immediateResponse;

	/** Queue of special Messages that we will need to send. */
	private Queue<Message> specialResponse;

	/** Socket over which the conversation with the single client occurs. */
	private final SocketChannel socket;

	/**
	 * Utility class which we will use to receive communication from this client.
	 */
	private ScanNetNB input;

	/** Utility class which we will use to send communication to this client. */
	private PrintNetNB output;

	/** Id for the user for whom we use this ClientRunnable to communicate. */
	private int userId;

	/** Name that the client used when connecting to the server. */
	private String name;

	/**
	 * Whether this client has been initialized, set its user name, and is ready to
	 * receive messages.
	 */
	private boolean initialized;

	/**
	 * The future that is used to schedule the client for execution in the thread
	 * pool.
	 */
	private ScheduledFuture<ClientRunnable> runnableMe;

	/** Collection of messages queued up to be sent to this client. */
	private Queue<Message> waitingList;

	/**
	 * Create a new thread with which we will communicate with this single client.
	 *
	 * @param client SocketChannel over which we will communicate with this new
	 *               client
	 * @throws IOException Exception thrown if we have trouble completing this
	 *                     connection
	 */
	public ClientRunnable(SocketChannel client) throws IOException {
		// Set up the SocketChannel over which we will communicate.
		socket = client;
		socket.configureBlocking(false);

		// Create the class we will use to receive input
		input = new ScanNetNB(socket);

		// Create the class we will use to send output
		output = new PrintNetNB(socket);

		// Mark that we are not initialized
		initialized = false;

		// Create our queue of special messages
		specialResponse = new LinkedList<>();

		// Create the queue of messages to be sent
		waitingList = new ConcurrentLinkedQueue<>();

		// Create our queue of message we must respond to immediately
		immediateResponse = new LinkedList<>();

		// Mark that the client is active now and start the timer until we
		// terminate for inactivity.
		terminateInactivityTime = new GregorianCalendar();
		terminateInactivityTime.setTimeInMillis(
				terminateInactivityTime.getTimeInMillis() + TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS);
	}

	/**
	 * Determines if this is a special message which we handle differently. It will
	 * handle the messages and return true if msg is "special." Otherwise, it
	 * returns false.
	 *
	 * @param msg Message in which we are interested.
	 * @return True if msg is "special"; false otherwise.
	 */
	private boolean broadcastMessageIsSpecial(Message msg) {
		boolean result = false;
		String text = msg.getText();
		if (text != null) {
			List<Message> responses = ServerConstants.getBroadcastResponses(text);
			if (responses != null) {
				for (Message current : responses) {
					handleSpecial(current);
				}
				result = true;
			}
		}
		return result;
	}

	/**
	 * Check to see for an initialization attempt and process the message sent.
	 * @throws SQLException
	 */
	private void checkForInitialization() throws SQLException {
		// Check if there are any input messages to read
		if (input.hasNextMessage()) {
			// If a message exists, try to use it to initialize the connection
			Message msg = input.nextMessage();
			String[] response = msg.getText().split(" ",2);
			if(response[0].equalsIgnoreCase("Error")) {
				initialized = false;
			}
			else {
				if (setUserName(msg.getName())) {
					// Update the time until we terminate this client due to inactivity.
					terminateInactivityTime.setTimeInMillis(
							new GregorianCalendar().getTimeInMillis() + TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS);
					// Set that the client is initialized.
					initialized = true;
				} else {
					initialized = false;
				}
			}

			enqueueMessage(msg);

			while (!waitingList.isEmpty()) {
				Message message = waitingList.remove();
				if(input.getPa_control() != null && input.getPa_control().equals("1"))

				{
					message = filterMessage2(message);
				}
				sendMessage(message);
			}
		}
	}

	/**
	 * Process one of the special responses
	 *
	 * @param msg Message to add to the list of special responses.
	 */
	private void handleSpecial(Message msg) {
		if (specialResponse.isEmpty()) {
			sendResponses = new Date();
			sendResponses.setTime(sendResponses.getTime() + SPECIAL_RESPONSE_DELAY_IN_MS);
		}
		specialResponse.add(msg);
	}

	/**
	 * Check if the message is properly formed. At the moment, this means checking
	 * that the identifier is set properly.
	 *
	 * @param msg Message to be checked
	 * @return True if message is correct; false otherwise
	 */
	private boolean messageChecks(Message msg) {
		// Check that the message name matches.
		return (msg.getName() != null) && (msg.getName().compareToIgnoreCase(getName()) == 0);
	}

	/**
	 * Immediately send this message to the client. This returns if we were
	 * successful or not in our attempt to send the message.
	 *
	 * @param message Message to be sent immediately.
	 * @return True if we sent the message successfully; false otherwise.
	 */
	private boolean sendMessage(Message message) {
		return output.print(message);
	}

	/**
	 * Try allowing this user to set his/her user name to the given username.
	 *
	 * @param userName The new value to which we will try to set userName.
	 * @return True if the username is deemed acceptable; false otherwise
	 */
	private boolean setUserName(String userName) {

		if (userName != null) {
			setName(userName);
			userId = userName.hashCode();
			return true;
		} else {
			// Clear this name; we cannot use it. *sigh*
			userId = -1;
			return false;
		}
	}

	/**
	 * Add the given message to this client to the queue of message to be sent to
	 * the client.
	 *
	 * @param message Complete message to be sent.
	 */
	public void enqueueMessage(Message message) {
		waitingList.add(message);
	}

	/**
	 * Get the name of the user for which this ClientRunnable was created.
	 *
	 * @return Returns the name of this client.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the user for which this ClientRunnable was created.
	 *
	 * @param name The name for which this ClientRunnable.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of the user for which this ClientRunnable was created.
	 *
	 * @return Returns the current value of userName.
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Return if this thread has completed the initialization process with its
	 * client and is read to receive messages.
	 *
	 * @return True if this thread's client should be considered; false otherwise.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Check if this instance of ClientRunnable is an Agency or a User.
	 * @return True is this is an Agency, false otherwise.
	 */
	public boolean isAgency() {
		String agentName = name;
		if (agentName.length() > 6) {
			return (agentName.substring(0, 6).equals("AGENCY"));
		}
		else {
			return false;
		}
	}

	/**
	 * Check if this instance of ClientRunnable is an Admin.
	 * @return True is this is an Admin, false otherwise.
	 */
	public boolean isAdmin() {
		return (name.equals("ADMIN"));
	}

	/**
	 * Get target name of Agency.
	 * @return name of Agency's target
	 */
	public String getTarget() {
		String target = "";
		if (isAgency()) {
			target = name.substring(6, name.length());
		}
		return target;
	}


	/**
	 * This function helps the user to update its username
	 * @param msg contains the updated name
	 * @param usersDao is the DAO of the user class
	 */
	public void userUpdate(Message msg, UserDao usersDao) {
		String update = msg.getText();
		update = update.replace("/update ", "");
		String serverMessage = "";

		try {
			String result = usersDao.updateUserName(name,update);
			if(result.equals(SUCCESS)) {
				name = update;
				serverMessage = SERVER_SUCCESS + "User  " + name + " updated successfully";
			} else {
				serverMessage = "Error : User  " + name + " " + "doesn't exist";
			}
			Message newMsg = Message.makeMessage("UPD", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
	}

	//Function for deleting a user
	public void userDelete(UserDao usersDao) {
		String serverMessage;

		try {
			String result = usersDao.delete(name, GroupsDao.getInstance());
			if(result.equals(SUCCESS)) {
				serverMessage = SERVER_SUCCESS + "User  " + name + " deleted successfully";
			} else {
				serverMessage = ERROR + name + " couldn't be deleted";
			}
			Message newMsg = Message.makeMessage("DEL", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
	}

	public void createGroup(Message msg, GroupsDao groupsDao) {

		String createGroup = msg.getText();
		createGroup = createGroup.replace("/group ", "");
		String serverMessage = "";

		try {
			Groups group = groupsDao.createGroup(createGroup, name, UserDao.getInstance());
			if(group != null) {
				serverMessage = SERVER_SUCCESS + "Group  " + createGroup + " created successfully";
			} else {
				serverMessage = ERROR + createGroup + " already exists !!";
			}
			Message newMsg = Message.makeMessage("GRP", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
	}

	public void addUserToGroup(Message msg, GroupsDao groupsDao) {

		String serverMessage = "";
		String addGroup = msg.getText();
		addGroup = addGroup.replace("/gadd ", "");
		String[] listUsers = addGroup.split("-");
		String groupName = listUsers[listUsers.length - 1];

		for(int i = 0; i< listUsers.length - 1; i++) {
			try {
				Groups group = groupsDao.addtoGroup(groupName, listUsers[i], UserDao.getInstance());
				if(group != null) {
					serverMessage = SERVER_SUCCESS + listUsers[i] + " inserted into "
							+ groupName + SUCCESSFULLY;
				} else {
					serverMessage = ERROR + listUsers[i] + " already exists in " + groupName
							+ " or not a user at all or "+groupName+" doesn't exist!!";
				}

				Message newMsg = Message.makeMessage("AGR", name, serverMessage);
				enqueueMessage(newMsg);
			} catch (SQLException e) {
				MyLogger.log(Level.SEVERE, e.getMessage());
			}
		}

	}

	public void deleteUserFromGroup(Message msg, GroupsDao groupsDao) {
		String serverMessage = "";
		String deleteFromGroup = msg.getText();
		deleteFromGroup = deleteFromGroup.replace("/gdelete ", "");
		String[] listUsers = deleteFromGroup.split("-");
		String groupName = listUsers[listUsers.length - 1];

		for(int i = 0; i< listUsers.length - 1; i++) {
			try {
				String result = groupsDao.deleteUserFromGroup(groupName, listUsers[i], UserDao.getInstance());
				if(result.equals(SUCCESS)) {
					serverMessage = SERVER_SUCCESS + listUsers[i] + " removed from " + groupName +
							SUCCESSFULLY;
				}
				else {
					serverMessage = ERROR + listUsers[i] + " doesn't exists in " + groupName
							+ " or not a user at all or " + groupName + " doesn't exist!!";
				}
				Message newMsg = Message.makeMessage("DGR", name, serverMessage);
				enqueueMessage(newMsg);
			} catch (SQLException e) {
				MyLogger.log(Level.SEVERE, e.getMessage());
			}
		}

	}

	public void updateGroupName(Message msg, GroupsDao groupsDao) {

		String serverMessage = "";
		String updateGroup = msg.getText();
		updateGroup = updateGroup.replace("/gupdate ", "");
		String[] groupNames = updateGroup.split("-");
		try {
			String result = groupsDao.updateGroupName(groupNames[0],groupNames[1]);
			if(result.equals(SUCCESS))
			{
				serverMessage = SERVER_SUCCESS + groupNames[0] + " changed to " + groupNames[1]
						+ SUCCESSFULLY;
			}
			else {
				serverMessage = ERROR + groupNames[0] + " doesn't exist";
			}
			Message newMsg = Message.makeMessage("UGR", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}


	public void listUsersInGroup(Message msg, GroupsDao groupsDao) {

		String serverMessage = "";
		String listGroup = msg.getText();
		listGroup = listGroup.replace("/glist ", "");
		try {
			List<String> users = groupsDao.listGroupName(listGroup);
			if(!users.isEmpty())
			{
				for(String str : users)
				{
					serverMessage = str;
					Message newMsg = Message.makeMessage("LGR", name, serverMessage);
					enqueueMessage(newMsg);
				}
			}
			else {
				serverMessage = ERROR + listGroup + " doesn't exist or no users in " + listGroup;
				Message newMsg = Message.makeMessage("LGR", name, serverMessage);
				enqueueMessage(newMsg);
			}
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}

	/**
	 * Get a List of users in a Group
	 * @param msg Message containing group Name
	 * @return List of users in Group
	 */
	public List<String> getListUsersInGroup(Message msg, GroupsDao groupsDao) {

		String listGroup = msg.getText();

		listGroup = listGroup.replace("/msgGroup ", "");
		String groupName = msg.getReceiver(listGroup);

		List<String> users = new ArrayList<>();
		try {
			List<String> listUsers = groupsDao.listGroupName(groupName);
			if((!listUsers.isEmpty())) {
				for (String uname : listUsers) {
					users.add(uname);
				}
			}

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
		return users;
	}

	/**
	 * Get a List of users in a Group
	 * @param msg Message containing group Name
	 * @return List of users in Group
	 */
	public List<String> getListUsersInRecallGroup(Message msg, GroupsDao groupsDao) {

		String listGroup = msg.getText();

		listGroup = listGroup.replace("/grpRecall ", "");
		String groupName = msg.getReceiver(listGroup);

		List<String> users = new ArrayList<>();
		try {
			List<String> listUsers = groupsDao.listGroupName(groupName);
			if((!listUsers.isEmpty())) {
				for (String uname : listUsers) {
					users.add(uname);
				}
			}

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
		return users;
	}

	public void deleteGroup(Message msg, GroupsDao groupsDao) {

		String serverMessage = "";
		String deleteGroup = msg.getText();
		deleteGroup = deleteGroup.replace("/grdelete ", "");
		try {
			String result = groupsDao.deleteGroup(deleteGroup);
			if(result.equals(SUCCESS)) {
				serverMessage = SERVER_SUCCESS + deleteGroup + " deleted successfully";
			}
			else {
				serverMessage = ERROR + deleteGroup + " doesn't exist";
			}
			Message newMsg = Message.makeMessage("DGP", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}

	public void saveMessage(Message msg, boolean userAvailable, MessageDao msgDao, String senderIp, String recvIp) {

		String[] senderAdd = senderIp.split(":");
		String senderIPAddress = senderAdd[0].substring(1, senderAdd[0].length());

		String recvIPAddress = "";
		if(!recvIp.equals(""))
		{
			String[] recvAdd = recvIp.split(":");
			recvIPAddress = recvAdd[0].substring(1, recvAdd[0].length());
		}

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/message ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			UserDao userdao = UserDao.getInstance();
			edu.northeastern.ccs.im.model.Message message = msgDao.createMessage(breakMessage[0],
					breakMessage[1],name, userAvailable,userdao ,senderIPAddress,recvIPAddress);
			if(message != null)
			{
				serverMessage = "Success : Message  "+saveMsg+" saved successfully";
			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("UMG", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());

		}
	}


	public void saveGroupMessage(Message msg, Map<String, Boolean> recipientsAvailable, GroupMessageDao msgDao) {
		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/msgGroup ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			GroupMessage message = msgDao.createGroupMessage(recipientsAvailable,breakMessage[1],name,
					breakMessage[0], UserDao.getInstance(), GroupsDao.getInstance());
			if(message != null)
			{
				serverMessage = "Success : Message  sent";
			}
			else {
				serverMessage = "Error : is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("UMG", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}


	public void recallGroupMessage(Message msg,GroupMessageDao msgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/grpRecall ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			String message = msgDao.createRecallGroupMessage(name,
					breakMessage[0], UserDao.getInstance(), GroupsDao.getInstance());
			if(message != null)
			{
				serverMessage = "Success : "+saveMsg+" Message  recalled";
			}
			else {
				serverMessage = "Error : is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("GRL", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
	}


	public void searchReceiverMessage(Message msg, MessageDao msgDao, GroupMessageDao grpMsgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/receiverSearch ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			List<String> message = msgDao.createReceiverSearchMessage(breakMessage[0],
					name, msgDao, grpMsgDao, userDao);
			if(message != null)
			{

				for (int i = 0; i < message.size(); i++) {
					serverMessage = message.get(i);
					Message newMsg = Message.makeMessage("SRM", "System", serverMessage);
					enqueueMessage(newMsg);
				}

			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
				Message newMsg = Message.makeMessage("SRM", name, serverMessage);
				enqueueMessage(newMsg);
			}

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}

	public void searchTimeMessage(Message msg, MessageDao msgDao, GroupMessageDao grpMsgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/timeSearch ", "");
		String breakMessage[] = saveMsg.split("@");
		String serverMessage = "";
		try {
			List<String> message = msgDao.createTimeSearchMessage(breakMessage[0],
					name, msgDao, grpMsgDao, userDao);
			if(message != null)
			{

				for (int i = 0; i < message.size(); i++) {
					serverMessage = message.get(i);
					Message newMsg = Message.makeMessage("STM", "System", serverMessage);
					enqueueMessage(newMsg);
				}

			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
				Message newMsg = Message.makeMessage("STM", name, serverMessage);
				enqueueMessage(newMsg);
			}

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}


	public void recallAnyMessage(Message msg, MessageDao msgDao, GroupMessageDao groupMessageDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/allMsg ", "");
		saveMsg = saveMsg.replaceAll("/senderSearch", "");
		String serverMessage = "";
		try {
			List<String> message = msgDao.createRecallAnyMessage(name, msgDao,groupMessageDao, userDao);
			if(message != null)
			{
				for (int i = 0; i < message.size(); i++) {
					serverMessage = message.get(i);
					Message newMsg = Message.makeMessage("RCA", "System", serverMessage);
					enqueueMessage(newMsg);
				}

			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
				Message newMsg = Message.makeMessage("RCA", name, serverMessage);
				enqueueMessage(newMsg);
			}

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}

	public void recallMessage(Message msg, MessageDao msgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/recall ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			String message = msgDao.createRecallMessage(breakMessage[0],
					name, msgDao,userDao);
			if(message != null)
			{

				serverMessage = "Success : "+saveMsg+" Message  recalled";
			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("RCL", name, serverMessage);
			enqueueMessage(newMsg);
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}

	public void recallAnyMessageId(Message msg, MessageDao msgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/id ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			String message = msgDao.createRecallAnyMessageId(name, breakMessage[0], msgDao,userDao);
			if(message != null)
			{
				serverMessage = "Success : "+saveMsg+" Message  recalled";

			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("RID", name, serverMessage);
			enqueueMessage(newMsg);

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}



	public void recallAnyGroupMessageId(Message msg, GroupMessageDao msgDao, UserDao userDao) {

		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/gid ", "");
		String breakMessage[] = saveMsg.split("-");
		String serverMessage = "";
		try {
			String message = msgDao.createRecallAnyGroupMessageId(name, breakMessage[0],userDao);
			if(message != null)
			{
				serverMessage = "Success : "+saveMsg+" Message  recalled";

			}
			else {
				serverMessage = "Error : "+saveMsg+" is null you cannot send null messages !!";
			}
			Message newMsg = Message.makeMessage("RGD", name, serverMessage);
			enqueueMessage(newMsg);

		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}

	}
	
	public void userMessageFunc(Message msg) {
		String userMsg = msg.getText();
		userMsg = userMsg.replace("/message ", "");
		Message newUserMsg = Message.makeMessage("UMG", name, userMsg);
		boolean userAvailable = Prattle.sendOutUserMessage(newUserMsg);
		String saveMsg = msg.getText();
		saveMsg = saveMsg.replace("/message ", "");
		String breakMessage[] = saveMsg.split("-");
		String recvName = breakMessage[0];
		String senderIp = "";
		String recvIp = "";
		try {
			for (ClientRunnable tt : Prattle.getActiveConnection()) {
				if(tt.isInitialized() && tt.name.equals(recvName))
				{
					recvIp = tt.input.getChannel().getRemoteAddress().toString();
				}
			}
			senderIp = input.getChannel().getRemoteAddress().toString();
		} catch (IOException e) {
			MyLogger.log(Level.SEVERE,EXCEPTION_MESSAGE + " " + e);
		}
		saveMessage(msg, userAvailable,MessageDao.getInstance(),senderIp,recvIp);
	}
	
	public void grpMsgFunc(Message msg)
	{
		String groupMsg = msg.getText();
		groupMsg = groupMsg.replace("/msgGroup ", "");;
		List<String> userList = getListUsersInGroup(msg, GroupsDao.getInstance());
		Message newGroupMsg = Message.makeMessage("GMG", name, groupMsg);
		HashMap<String, Boolean> recipientsAvailable = Prattle.sendOutGroupMessage(newGroupMsg, userList);
		saveGroupMessage(msg, recipientsAvailable, GroupMessageDao.getInstance());
	}
	public void recallMsgFunc(Message msg)
	{
		String recallMsg = msg.getText();
		recallMsg = recallMsg.replace("/recall","");
		Message recallMessage = Message.makeMessage("RCL", name, recallMsg);
		recallMessage(msg, MessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void recallAnyMsgFunc(Message msg)
	{
		String recallMsg = msg.getText();
		recallMsg = recallMsg.replace("/allMsg","");
		Message recallMessage = Message.makeMessage("RCA", name, recallMsg);
		recallAnyMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void recallMessageId(Message msg)
	{
		String recallMsg = msg.getText();
		recallMsg = recallMsg.replace("/id","");
		Message recallMessage = Message.makeMessage("RID", name, recallMsg);
		recallAnyMessageId(msg, MessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void recallGrpMessageId(Message msg)
	{
		String recallMsg = msg.getText();
		recallMsg = recallMsg.replace("/gid","");
		Message recallMessage = Message.makeMessage("RGD", name, recallMsg);
		recallAnyGroupMessageId(msg, GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void recallGrpMessage(Message msg)
	{
		String recallMsg = msg.getText();
		recallMsg = recallMsg.replace("/grpRecall","");
		List<String> userList = getListUsersInRecallGroup(msg, GroupsDao.getInstance());
		Message recallMessage = Message.makeMessage("GRL", name, recallMsg);
		HashMap<String, Boolean> recipientsAvailable = Prattle.sendOutGroupMessage(recallMessage, userList);
		recallGroupMessage(msg, GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void searchRecvMesg(Message msg)
	{
		String searchMsg = msg.getText();
		searchMsg = searchMsg.replace("/receiverSearch","");
		Message recallMessage = Message.makeMessage("SRM", name, searchMsg);
		searchReceiverMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void searchSenderMsg(Message msg)
	{
		String searchMsg = msg.getText();
		searchMsg = searchMsg.replace("/senderSearch","");
		Message recallMessage = Message.makeMessage("SSM", name, searchMsg);
		recallAnyMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	
	public void searchTimeMsg(Message msg)
	{
		String searchMsg = msg.getText();
		searchMsg = searchMsg.replace("/timeSearch","");
		Message recallMessage = Message.makeMessage("STM", name, searchMsg);
		searchTimeMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
	}
	

	public void messageFunctions(Message msg) {
		//The control goes here when there is a request from user to update its username
		if (msg.update()) {
			userUpdate(msg, UserDao.getInstance());
		}

		//The control goes here when there is a request from user to delete himself
		else if (msg.delete()) {
			userDelete(UserDao.getInstance());
		}
		//to create group
		else if(msg.createGroup()) {
			createGroup(msg, GroupsDao.getInstance());
		}

		else if(msg.addGroup())
		{
			addUserToGroup(msg, GroupsDao.getInstance());
		}

		else if(msg.deleteFromGroup())
		{
			deleteUserFromGroup(msg, GroupsDao.getInstance());
		}

		else if (msg.updateGroup()) {
			updateGroupName(msg, GroupsDao.getInstance());
		}

		else if (msg.listGroup()) {
			listUsersInGroup(msg, GroupsDao.getInstance());
		}

		else if (msg.deleteGroup()) {
			deleteGroup(msg, GroupsDao.getInstance());
		}

		else if (msg.isUserMessage()) {
			//module
			userMessageFunc(msg);
		}

		else if (msg.isGroupMessage()) {
			grpMsgFunc(msg);
		}


		else if(msg.isRecallMessage()) {
			recallMsgFunc(msg);
		}

		else if(msg.isRecallAnyMessage()) {
			recallAnyMsgFunc(msg);
		}

		else if(msg.isRecallMessageId()) {

			recallMessageId(msg);
		}

		else if(msg.isRecallGroupMessageId()) {

			recallGrpMessageId(msg);
		}

		else if(msg.isRecallGroupMessage()) {

			recallGrpMessage(msg);

		}
		else if(msg.isSearchReceiverMessage()) {

			searchRecvMesg(msg);

		}
		else if(msg.isSearchSenderMessage()) {

			searchSenderMsg(msg);
		}

		else if(msg.isSearchTimeMessage()) {

			searchTimeMsg(msg);
		}
	}
	
	public void broadcastMsgFunc(Message msg) {
		if ((msg.getText() != null)
				&& (msg.getText().compareToIgnoreCase(ServerConstants.BOMB_TEXT) == 0)) {
			initialized = false;
			Prattle.broadcastMessage(Message.makeQuitMessage(name));
		} else {
			Prattle.broadcastMessage(msg);
		}
	}
	
	/**
	 * Perform the periodic actions needed to work with this client.
	 *
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		boolean terminate = false;
		// The client must be initialized before we can do anything else
		if (!initialized) {
			try {
				checkForInitialization();
			} catch (SQLException e) {

				MyLogger.log(Level.SEVERE, e.getMessage());

			}
		} else {
			try {
				// Client has already been initialized, so we should first check
				// if there are any input
				// messages.
				if (input.hasNextMessage()) {
					// Get the next message
					Message msg = input.nextMessage();

					// Update the time until we terminate the client for
					// inactivity.
					terminateInactivityTime.setTimeInMillis(
							new GregorianCalendar().getTimeInMillis() + TERMINATE_AFTER_INACTIVE_BUT_LOGGEDIN_IN_MS);
					// If the message is a broadcast message, send it out
					if (msg.isDisplayMessage()) {
						// Check if the message is legal formatted
						if (messageChecks(msg)) {

							// Check for our "special messages"
							if ((msg.isBroadcastMessage()) && (!broadcastMessageIsSpecial(msg))) {
								// Check for our "special messages"								
								broadcastMsgFunc(msg);
							}
						} else {
							Message sendMsg;
							sendMsg = Message.makeBroadcastMessage(ServerConstants.BOUNCER_ID,
									"Last message was rejected because it specified an incorrect user name.");
							enqueueMessage(sendMsg);
						}
					}

					else if (msg.terminate()) {
						// Stop sending the poor client message.
						terminate = true;
						// Reply with a quit message.
						enqueueMessage(Message.makeQuitMessage(name));
					}


					/*					else if (msg.update()) {
						userUpdate(msg, UserDao.getInstance());
					}

					//The control goes here when there is a request from user to delete himself
					else if (msg.delete()) {
						userDelete(UserDao.getInstance());
					}

					else if(msg.createGroup()) {
						createGroup(msg, GroupsDao.getInstance());
					}

					else if(msg.addGroup())
					{
						addUserToGroup(msg, GroupsDao.getInstance());
					}

					else if(msg.deleteFromGroup())
					{
						deleteUserFromGroup(msg, GroupsDao.getInstance());
					}

					else if (msg.updateGroup()) {
						updateGroupName(msg, GroupsDao.getInstance());
					}

					else if (msg.listGroup()) {
						listUsersInGroup(msg, GroupsDao.getInstance());
					}

					else if (msg.deleteGroup()) {
						deleteGroup(msg, GroupsDao.getInstance());
					}

					else if (msg.isUserMessage()) {
						String userMsg = msg.getText();
						userMsg = userMsg.replace("/message ", "");
						Message newUserMsg = Message.makeMessage("UMG", name, userMsg);

						boolean userAvailable = Prattle.sendOutUserMessage(newUserMsg);
						saveMessage(msg, userAvailable,MessageDao.getInstance(), userMsg, userMsg);
					}

					else if (msg.isGroupMessage()) {
						String groupMsg = msg.getText();
						groupMsg = groupMsg.replace("/msgGroup ", "");;
						List<String> userList = getListUsersInGroup(msg, GroupsDao.getInstance());
						Message newGroupMsg = Message.makeMessage("GMG", name, groupMsg);
						HashMap<String, Boolean> recipientsAvailable = Prattle.sendOutGroupMessage(newGroupMsg, userList);
						saveGroupMessage(msg, recipientsAvailable, GroupMessageDao.getInstance());
					}


					else if(msg.isRecallMessage()) {

						String recallMsg = msg.getText();
						recallMsg = recallMsg.replace("/recall","");
						Message recallMessage = Message.makeMessage("RCL", name, recallMsg);
						recallMessage(msg, MessageDao.getInstance(), UserDao.getInstance());

					}

					else if(msg.isRecallAnyMessage()) {

						String recallMsg = msg.getText();
						recallMsg = recallMsg.replace("/allMsg","");
						Message recallMessage = Message.makeMessage("RCA", name, recallMsg);
						recallAnyMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
					}

					else if(msg.isRecallMessageId()) {

						String recallMsg = msg.getText();
						recallMsg = recallMsg.replace("/id","");
						Message recallMessage = Message.makeMessage("RID", name, recallMsg);
						recallAnyMessageId(msg, MessageDao.getInstance(), UserDao.getInstance());
					}

					else if(msg.isRecallGroupMessageId()) {

						String recallMsg = msg.getText();
						recallMsg = recallMsg.replace("/gid","");
						Message recallMessage = Message.makeMessage("RGD", name, recallMsg);
						recallAnyGroupMessageId(msg, GroupMessageDao.getInstance(), UserDao.getInstance());
					}

					else if(msg.isRecallGroupMessage()) {

						String recallMsg = msg.getText();
						recallMsg = recallMsg.replace("/grpRecall","");
						List<String> userList = getListUsersInRecallGroup(msg, GroupsDao.getInstance());
						Message recallMessage = Message.makeMessage("GRL", name, recallMsg);
						HashMap<String, Boolean> recipientsAvailable = Prattle.sendOutGroupMessage(recallMessage, userList);
						recallGroupMessage(msg, GroupMessageDao.getInstance(), UserDao.getInstance());

					}
					else if(msg.isSearchReceiverMessage()) {

						String searchMsg = msg.getText();
						searchMsg = searchMsg.replace("/receiverSearch","");
						Message recallMessage = Message.makeMessage("SRM", name, searchMsg);
						searchReceiverMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());


					}
					else if(msg.isSearchSenderMessage()) {

						String searchMsg = msg.getText();
						searchMsg = searchMsg.replace("/senderSearch","");
						Message recallMessage = Message.makeMessage("SSM", name, searchMsg);
						recallAnyMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());

					}

					else if(msg.isSearchTimeMessage()) {

						String searchMsg = msg.getText();
						searchMsg = searchMsg.replace("/timeSearch","");
						Message recallMessage = Message.makeMessage("STM", name, searchMsg);
						searchTimeMessage(msg, MessageDao.getInstance(), GroupMessageDao.getInstance(), UserDao.getInstance());
					}*/

					else {
						messageFunctions(msg);
					}

					// Otherwise, ignore it (for now).
				}
				if (!immediateResponse.isEmpty()) {
					while (!immediateResponse.isEmpty()) {
						sendMessage(immediateResponse.remove());
					}
				}

				// Check to make sure we have a client to send to.
				boolean processSpecial = !specialResponse.isEmpty()
						&& ((!initialized) || (!waitingList.isEmpty()) || sendResponses.before(new Date()));
				boolean keepAlive = !processSpecial;
				// Send the responses to any special messages we were asked.
				if (processSpecial) {
					// Send all of the messages and check that we get valid
					// responses.
					while (!specialResponse.isEmpty()) {
						keepAlive |= sendMessage(specialResponse.remove());
					}
				}
				if (!waitingList.isEmpty()) {
					if (!processSpecial) {
						keepAlive = false;
					}
					// Send out all of the message that have been added to the
					// queue.
					do {
						Message msg = waitingList.remove();
						if(input.getPa_control() != null && input.getPa_control().equals("1"))

						{
							msg = filterMessage(msg);
						}
						boolean sentGood = sendMessage(msg);
						keepAlive |= sentGood;
					} while (!waitingList.isEmpty());
				}
				terminate |= !keepAlive;
			} catch (SQLException e) {
				MyLogger.log(Level.SEVERE,e.getMessage());
			} finally {
				// When it is appropriate, terminate the current client.
				if (terminate) {
					System.out.println("TERMINATE");
					terminateClient();
				}
			}
		}
		// Finally, check if this client have been inactive for too long and,
		// when they have, terminate
		// the client.
		if (!terminate && terminateInactivityTime.before(new GregorianCalendar())) {
			String terminateErrMessage = "Timing out or forcing off a user " + name;
			MyLogger.log(Level.SEVERE,terminateErrMessage);
			terminateClient();
		}
	}
	private Message filterMessage(Message msg) {
		String userMsg = msg.getText();
		//tokenize the message
		String saveMsg = userMsg;
		String incomingMsg[] = saveMsg.split("-");
		String actualMessage = incomingMsg[incomingMsg.length-1];
		String filterMsg[] = actualMessage.split("\\s*(,|\\s|=>)\\s*");
		//filter on each string
		for(String str : filterMsg)
		{
			InputStream in = new ByteArrayInputStream(str.getBytes());
			//function used for stemming the incoming string
			String stemmedWord = testContent(in);
			//function to test if the stemmed word matches with the inappropriate word
			try {
				if(isMatch(stemmedWord))
				{
					//replace all bad Strings
					userMsg = userMsg.replaceAll(str,"*****");
				}
			} catch (IOException e) {
				MyLogger.log(Level.SEVERE,e.getMessage());
			}
		}

		return Message.makeMessage(msg.getMessageType(), msg.getName(), userMsg);
	}




	private boolean isMatch(String stemmedWord) throws IOException {
		File file = new File("Bad_Words_Stemmed.txt");
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String badWord;
			while((badWord = br.readLine()) != null)
			{
				if(stemmedWord.equals(badWord) ||
						DiceCoefficient.diceCoefficientOptimized(badWord,stemmedWord) >= 0.95)
				{
					return true;
				}
			}
		}
		catch(Exception e)
		{
			MyLogger.log(Level.SEVERE,EXCEPTION_MESSAGE + " " + e);
		}
		return false;
	}



	private Message filterMessage2(Message msg) {
		String userMsg = msg.getText();
		//tokenize the message
		String filterMsg[] = userMsg.split("\\s*(,|\\s|=>)\\s*");
		//filter on each string
		for(String str : filterMsg)
		{
			InputStream in = new ByteArrayInputStream(str.getBytes());
			//function used for stemming the incoming string
			String stemmedWord = testContent(in);
			//function to test if the stemmed word matches with the inappropriate word
			try {
				if(isMatch(stemmedWord))
				{
					//replace all bad Strings
					userMsg = userMsg.replaceAll(str,"*****");
				}
			} catch (IOException e) {
				MyLogger.log(Level.SEVERE,e.getMessage());
			}
		}

		return Message.makeMessage("UMG", msg.getName(), userMsg);
	}

	private String testContent(InputStream in) {
		Stemmer s = new Stemmer();
		char[] w = new char[501];
		StringBuilder stemmedWord = new StringBuilder();
		try
		{
			while(true)
			{
				int ch = in.read();
				if (Character.isLetter((char) ch))
				{
					ch = testContentInner(ch,w,in,s,stemmedWord);
				}
				if (ch < 0) break;
				stemmedWord.append((char)ch);
			}
		}
		catch (IOException e)
		{
			MyLogger.log(Level.SEVERE,EXCEPTION_MESSAGE + " " + e);
		}
		return stemmedWord.toString();
	}

	private int testContentInner(int ch, char[] w, InputStream in, Stemmer s, StringBuilder stemmedWord) {

		int j = 0;
		while(true)
		{
			ch = Character.toLowerCase((char) ch);
			w[j] = (char) ch;
			if (j < 500)
				j++;
			try {
				ch = in.read();
				if (!Character.isLetter((char) ch))
				{
					for (int c = 0; c < j; c++) s.add(w[c]);
					s.stem();
					{
						String u;
						u = s.toString();
						stemmedWord.append(u);
					}
					break;
				}
			} catch (IOException e) {
				MyLogger.log(Level.SEVERE,EXCEPTION_MESSAGE + " " + e);
			}
		}
		return ch;
	}

	/**
	 * Store the object used by this client runnable to control when it is scheduled
	 * for execution in the thread pool.
	 *
	 * @param future Instance controlling when the runnable is executed from within
	 *               the thread pool.
	 */
	public void setFuture(ScheduledFuture<ClientRunnable> future) {
		runnableMe = future;
	}

	/**
	 * Terminate a client that we wish to remove. This termination could happen at
	 * the client's request or due to system need.
	 */
	public void terminateClient() {
		try {
			// Once the communication is done, close this connection.
			input.close();
			socket.close();
		} catch (IOException e) {
			// If we have an IOException, ignore the problem
			MyLogger.log(Level.SEVERE,"Exception Occured " + e);
		} finally {
			// Remove the client from our client listing.
			Prattle.removeClient(this);
			// And remove the client from our client pool.
			if(runnableMe != null) {
				runnableMe.cancel(false);
			}
		}
	}
}