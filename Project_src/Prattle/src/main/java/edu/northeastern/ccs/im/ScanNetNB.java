package edu.northeastern.ccs.im;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import org.mindrot.jbcrypt.BCrypt;
import edu.northeastern.ccs.im.dao.GroupMessageDao;
import edu.northeastern.ccs.im.dao.MessageDao;
import edu.northeastern.ccs.im.dao.UserDao;
import edu.northeastern.ccs.im.model.User;

/**
 * This class is similar to the java.util.Scanner class, but this class's
 * methods return immediately and does not wait for network input (it is
 * &quot;non-blocking&quot; in technical parlance).
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 *
 * @version 1.3
 */
public class ScanNetNB {
	private static final int BUFFER_SIZE = 64 * 1024;
	private static final int DECIMAL_RADIX = 10;
	private static final int HANDLE_LENGTH = 3;
	private static final int MIN_MESSAGE_LENGTH = 7;
	private static final String CHARSET_NAME = "ISO-8859-1";
	private SocketChannel channel;
	private Selector selector;
	private SelectionKey key;
	private ByteBuffer buff;
	private Queue<Message> messages;
	private String handlee;
	private String pa_control;

	public String getPa_control() {
		return pa_control;
	}

	private static final String EXCEPTION_MESSAGE = "Exception Occurred";
	String extension = "";
	/**
	 * Creates a new instance of this class. Since, by definition, this class takes
	 * in input from the network, we need to supply the non-blocking Socket instance
	 * from which we will read.
	 *
	 * @param sockChan Non-blocking SocketChannel from which we will receive
	 *                 communications.
	 */
	public ScanNetNB(SocketChannel sockChan) {
		// Create the queue that will hold the messages received from over the network
		messages = new ConcurrentLinkedQueue<>();
		// Allocate the buffer we will use to read data
		buff = ByteBuffer.allocate(BUFFER_SIZE);
		// Remember the channel that we will be using.
		channel = sockChan;
		try {
			// Open the selector to handle our non-blocking I/O
			selector = Selector.open();
			// Register our channel to receive alerts to complete the connection
			key = channel.register(selector, SelectionKey.OP_READ);
		} catch (IOException e) {
			// For the moment we are going to simply cover up that there was a problem.
			MyLogger.log(Level.SEVERE, EXCEPTION_MESSAGE + " " + e);
			assert false;
		}
	}
	/**
	 * Creates a new instance of this class. Since, by definition, this class takes
	 * in input from the network, we need to supply the non-blocking Socket instance
	 * from which we will read.
	 *
	 * @param connection Non-blocking Socket instance from which we will receive
	 *                   communications.
	 */
	public ScanNetNB(SocketNB connection) {
		// Get the socket channel from the SocketNB instance and go.
		this(connection.getSocket());
	}
	/**
	 * Read in a new argument from the IM server.
	 *
	 * @param charBuffer Buffer holding text from over the network.
	 * @return String holding the next argument sent over the network.
	 */
	private String readArgument(CharBuffer charBuffer) {
		// Compute the current position in the buffer
		int pos = charBuffer.position();
		// Compute the length of this argument
		int length = 0;
		// Track the number of locations visited.
		int seen = 0;
		// Assert that this character is a digit representing the length of the first argument
		assert Character.isDigit(charBuffer.get(pos));
		// Now read in the length of the first argument
		while (Character.isDigit(charBuffer.get(pos))) {
			// My quick-and-dirty numeric converter
			length = length * DECIMAL_RADIX;
			length += Character.digit(charBuffer.get(pos), DECIMAL_RADIX);
			// Move to the next character
			pos += 1;
			seen += 1;
		}
		seen += 1;
		if (length == 0) {
			// Update our position
			charBuffer.position(pos);
			// If the length is 0, this argument is null
			return null;
		}
		String result = charBuffer.subSequence(seen, length + seen).toString();
		charBuffer.position(pos + length);
		return result;
	}
	/**
	 * This function helps the user to register into the system
	 * @param sender is the user trying to login
	 * @param message is the message send by the user who is trying to login
	 * @param serverMessage response given by server
	 * @param handle handle of the message
	 * @param usersDao is the DAO of the user class
	 */
	public void userRegister(String sender, String message, String serverMessage, String handle, UserDao usersDao)
	{
		//Password is encrypted here using BCrypt( Password from Chatter is also encrypted before coming here)
		message = BCrypt.hashpw(message, BCrypt.gensalt());
		User user = new User(sender,message);
		try {
			if(!userExists(usersDao,user))
			{
				usersDao.create(user);
				serverMessage = "Success : User  "+sender+" registered successfully";
			}
			else {
				serverMessage = "Error : User already exists. Try giving the password on which user "+sender+" was registered";
			}
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
		Message newMsg = Message.makeMessage(handle, sender, serverMessage);
		messages.add(newMsg);
	}
	/**
	 * This function helps the user to login into the system
	 * @param sender is the user trying to login
	 * @param message is the message send by the user who is trying to login
	 * @param serverMessage response given by server
	 * @param handle handle of the message
	 * @throws SQLException
	 */

	public void login(String sender, String message, String serverMessage, String handle, UserDao usersDao) throws SQLException
	{
		this.pa_control = sender.substring(0,1);
		sender = sender.substring(1, sender.length());
		User user = new User(sender,message);
		if(userExists(usersDao,user))
		{
			String successMessage = "Success : User "+sender+" logged in";
			if(!getUnreadMessages(sender, usersDao).isEmpty()) {

				serverMessage = successMessage+ "\n You have the below messages unread " +getUnreadMessages(sender, usersDao).toString();
				updateRecvIp(sender,usersDao);
				usersDao.updateReadReceipt(sender, GroupMessageDao.getInstance());
				messages.add(Message.makeMessage(handle, sender, serverMessage));
			}
			else {
				messages.add(Message.makeMessage(handle, sender, successMessage + "\n You have no unread messages"));
			}
		}
		else {
			messages.add(Message.makeMessage(handle, sender, "Error : Invalid credentials for user "+sender));
		}
		Message newMsg = Message.makeMessage(handle, sender, serverMessage);
		messages.add(newMsg);
	}

	public void updateRecvIp(String sender, UserDao userDao) throws SQLException {
		try {
			MessageDao.getInstance().updateRecvIp(sender,userDao,channel.getRemoteAddress().toString());
		} catch (IOException e) {
			MyLogger.log(Level.SEVERE, EXCEPTION_MESSAGE + " " + e);
		}
	}

	public List<String> getUnreadMessages(String sender, UserDao userDao) throws SQLException {

		return userDao.getUnreadMessages(sender, GroupMessageDao.getInstance());
	}


	/**
	 * This function helps login an Agency into the system
	 * @param sender is the user trying to login
	 * @param message is the message send by the user who is trying to login
	 * @param handle handle of the message
	 */
	public void loginAgency(String sender, String message, String handle) {
		String serverMessage = "You are now an Agency";
		Message newMsg = Message.makeMessage(handle, sender, serverMessage);
		messages.add(newMsg);
	}

	/**
	 * This function helps login an Admin into the system
	 * @param sender is the user trying to login
	 * @param handle handle of the message
	 */
	public void loginAdmin(String sender, String message, String handle) {
		String serverMessage = "You are now an Administrator";
		Message newMsg = Message.makeMessage(handle, sender, serverMessage);
		messages.add(newMsg);
	}

	/**
	 * This function handles the login and registration of the user
	 * @param handle handle of the message
	 * @param sender is the user trying to login
	 * @param message is the message send by the user who is trying to login
	 * @param serverMessage response given by server
	 * @param charBuffer is the buffer containing the message
	 * @throws SQLException is the exception if there is any failure in operations to database
	 */
	public void loginRegisterFunc(String handle, String sender, String message, String serverMessage, CharBuffer charBuffer) throws SQLException{

		if(handle.equals("HLO"))
		{
			Character option = charBuffer.charAt(charBuffer.length() -1);
			if(option == '1')
			{
				userRegister(sender,message,serverMessage,handle, UserDao.getInstance());
			}
			else if (option == '2'){
				login(sender,message,serverMessage,handle, UserDao.getInstance());
			}
			else if (option == '3') {
				loginAgency(sender,message,handle);
			}
			else if (option == '0') {
				loginAdmin(sender, message,handle);
			}
			charBuffer.position(charBuffer.position() + 4);
		}
		else {
			Message newMsg = Message.makeMessage(handle, sender, message);
			messages.add(newMsg);
		}
	}
	/**
	 * Returns true if there is another line of input from this instance. This
	 * method will NOT block while waiting for input. This class does not advance
	 * past any input.
	 *
	 * @return True if and only if this instance of the class has another line of
	 *         input
	 * @throws SQLException
	 * @see java.util.Scanner#hasNextLine()
	 */
	public boolean hasNextMessage() throws SQLException {
		// If we have messages waiting for us, return true.
		if (!messages.isEmpty()) {
			return true;
		}
		try {
			// Otherwise, check if we can read in at least one new message
			if (selector.selectNow() != 0) {
				assert key.isReadable();
				// Read in the next set of commands from the channel.
				channel.read(buff);
				selector.selectedKeys().remove(key);
				buff.flip();
			} else {
				return false;
			}
			// Create a decoder which will convert our traffic to something useful
			Charset charset = Charset.forName(CHARSET_NAME);
			CharsetDecoder decoder = charset.newDecoder();
			// Convert the buffer to a format that we can actually use.
			CharBuffer charBuffer = decoder.decode(buff);
			handlee = charBuffer.subSequence(0, HANDLE_LENGTH).toString();
			// get rid of any extra whitespace at the beginning
			// Start scanning the buffer for any and all messages.
			int start = 0;
			// Scan through the entire buffer; check that we have the minimum message size
			while ((start + MIN_MESSAGE_LENGTH) <= charBuffer.limit()) {
				// If this is not the first message, skip extra space.
				if (start != 0) {
					charBuffer.position(start);
				}
				// First read in the handle
				final String handle = charBuffer.subSequence(0, HANDLE_LENGTH).toString();
				// Skip past the handle
				charBuffer.position(start + HANDLE_LENGTH + 1);
				// Read the first argument containing the sender's name
				final String sender = readArgument(charBuffer);
				// Skip past the leading space
				charBuffer.position(charBuffer.position() + 2);
				// Read in the second argument containing the message
				final String message = readArgument(charBuffer);
				// Add this message into our queue
				String serverMessage = "";
				loginRegisterFunc(handle,sender,message,serverMessage,charBuffer);
				// And move the position to the start of the next character
				start = charBuffer.position() + 1;
			}
			// Move any read messages out of the buffer so that we can add to the end.
			buff.position(start);
			// Move all of the remaining data to the start of the buffer.
			buff.compact();

		} catch (IOException ioe) {
			// For the moment, we will cover up this exception and hope it never occurs.
			MyLogger.log(Level.SEVERE, EXCEPTION_MESSAGE + " " + ioe);
			assert false;
		}
		// Do we now have any messages?
		return !messages.isEmpty();
	}

	public boolean userExists(UserDao usersDao, User user)
	{
		try {
			return usersDao.checkUserExists(user.getUserName(), user.getPassword());
		} catch (SQLException e) {
			MyLogger.log(Level.SEVERE, e.getMessage());
		}
		return false;
	}
	/**
	 * Advances past the current line and returns the line that was read. This
	 * method returns the rest of the current line, excluding any line separator at
	 * the end. The position in the input is set to the beginning of the next line.
	 *
	 * @throws NextDoesNotExistException Exception thrown when hasNextLine returns
	 *                                   false.
	 * @return String containing the line that was skipped
	 * @see java.util.Scanner#nextLine()
	 */
	public Message nextMessage() {
		if (messages.isEmpty()) {
			throw new NextDoesNotExistException("No next line has been typed in at the keyboard");
		}
		//delete
		Message msg = messages.remove();
		return msg;
	}
	public void close() {
		try {
			selector.close();
		} catch (IOException e) {
			MyLogger.log(Level.SEVERE, EXCEPTION_MESSAGE + " " + e);
			assert false;
		}
	}
	public Queue<Message> getMessages() {
		return this.messages;
	}

	public String getHandlee() {
		return handlee;
	}

	public void setHandlee(String handlee) {
		this.handlee = handlee;
	}

	public SocketChannel getChannel() {
		return channel;
	}
}