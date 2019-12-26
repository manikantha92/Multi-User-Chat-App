package edu.northeastern.ccs.im;
import java.util.List;
import java.io.File;
import java.util.Arrays;

/**
 * Each instance of this class represents a single transmission by our IM
 * clients.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 * 
 * @version 1.3
 */
public class Message {
	/**
	 * List of the different possible message types.
	 */
	public enum MessageType {
		/**
		 * Message sent by the user attempting to login using a specified username.
		 */
		HELLO("HLO"),
		/** Message sent by the server acknowledging a successful log in. */
		ACKNOWLEDGE("ACK"),
		/** Message sent by the server rejecting a login attempt. */
		NO_ACKNOWLEDGE("NAK"),
		/**
		 * Message sent by the user to start the logging out process and sent by the
		 * server once the logout process completes.
		 */
		QUIT("BYE"),
		/** Message whose contents is broadcast to all connected users. */
		BROADCAST("BCT"),

		UPDATE("UPD"),

		DELETE("DEL"),

		GROUP("GRP"),

		ADDGROUP("AGR"),

		DFGROUP("DGR"),

		UPDATEGROUP("UGR"),

		LISTGROUP("LGR"),

		DGROUP("DGP"),

		/** Message to another User **/
		USERMSG("UMG"),
		/** Message to group **/
		GROUPMSG("GMG"),

		FILEMSG("FIL"),
		RECALLMSG("RCL"),
		GROUPRECALLMSG("GRL"),
		RECALLANYMSG("RCA"),
		RECALLMSGID("RID"),
		RECALLGMSGID("RGD"),
		SEARCHSENDERMSG("SSM"),
		SEARCHRECEIVERMSG("SRM"),
		LOGGERMSG("LOG"),
		SEARCHTIMEMSG("STM");

		/** Store the short name of this message type. */
		private String tla;

		/**
		 * Define the message type and specify its short name.
		 * 
		 * @param abbrev Short name of this message type, as a String.
		 */
		private MessageType(String abbrev) {
			tla = abbrev;
		}

		/**
		 * Return a representation of this Message as a String.
		 * 
		 * @return Three letter abbreviation for this type of message.
		 */
		@Override
		public String toString() {
			return tla;
		}
	}

	/** The string sent when a field is null. */
	private static final String NULL_OUTPUT = "--";

	/** The handle of the message. */
	private MessageType msgType;

	/**
	 * The first argument used in the message. This will be the sender's identifier.
	 */
	private String msgSender;

	/**
	 * The second argument used for sender's password during initial connection
	 */
	private String option;

	/**
	 * The third argument used for sender's file
	 */
	private static byte[] fileByte;

	/**
	 * The fourth argument used for sender's file
	 */
	private static String extension;

	public byte[] getFileByte() {
		return fileByte;
	}


	public String getExtension() {
		return extension;
	}



	/** The second argument used in the message. */
	private String msgText;



	/**
	 * Create a new message that contains actual IM text. The type of distribution
	 * is defined by the handle and we must also set the name of the message sender,
	 * message recipient, and the text to send.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Name of the individual sending this message
	 * @param text    Text of the instant message
	 */
	private Message(MessageType handle, String srcName, String text) {
		msgType = handle;
		// Save the properly formatted identifier for the user sending the
		// message.
		msgSender = srcName;
		// Save the text of the message.
		msgText = text;
	}

	/**
	 * Create a new message that contains actual IM text. The type of distribution
	 * is defined by the handle and we must also set the name of the message sender,
	 * message recipient, and the text to send.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Name of the individual sending this message
	 * @param text    Text of the instant message
	 */
	private Message(MessageType handle, String srcName, String text, String option) {
		msgType = handle;
		// Save the properly formatted identifier for the user sending the
		// message.
		msgSender = srcName;
		// Save the text of the message.
		msgText = text;
		//The option of the user
		this.option = option;

	}

	/**
	 * Create a new message that contains actual IM text. The type of distribution
	 * is defined by the handle and we must also set the name of the message sender,
	 * message recipient, and the text to send.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Name of the individual sending this message
	 * @param fileByte file in the form of a byte array
	 * @param extension file's extension in string
	 * 
	 */
	private Message(MessageType handle, String srcName, byte[] fileByte, String extension ) {

		msgType = handle;
		msgSender = srcName;
		this.fileByte = fileByte;
		this.extension = extension;
	}

	/**
	 * Create simple command type message that does not include any data.
	 * 
	 * @param handle Handle for the type of message being created.
	 */
	private Message(MessageType handle) {
		this(handle, null, null);
	}

	/**
	 * Create a new message that contains a command sent the server that requires a
	 * single argument. This message contains the given handle and the single
	 * argument.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Argument for the message; at present this is the name used to
	 *                log-in to the IM server.
	 */
	private Message(MessageType handle, String srcName) {
		this(handle, srcName, null);
	}

	/**
	 * Create a new message to continue the logout process.
	 * 
	 * @return Instance of Message that specifies the process is logging out.
	 */
	public static Message makeQuitMessage(String myName) {
		return new Message(MessageType.QUIT, myName, null);
	}

	/**
	 * Create a new message broadcasting an announcement to the world.
	 * 
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeBroadcastMessage(String myName, String text) {
		return new Message(MessageType.BROADCAST, myName, text);
	}

	/**
	 * Create a new message stating the name with which the user would like to
	 * login.
	 * 
	 * @param text Name the user wishes to use as their screen name.
	 * @return Instance of Message that can be sent to the server to try and login.
	 */
	protected static Message makeHelloMessage(String text) {
		return new Message(MessageType.HELLO, null, text);
	}

	/**
	 * Given a handle, name and text, return the appropriate message instance or an
	 * instance from a subclass of message.
	 * 
	 * @param handle  Handle of the message to be generated.
	 * @param srcName Name of the originator of the message (may be null)
	 * @param text    Text sent in this message (may be null)
	 * @return Instance of Message (or its subclasses) representing the handle,
	 *         name, & text.
	 */
	protected static Message makeMessage(String handle, String srcName, String text, String srcPwd) {
		Message result = null;
		if (handle.compareTo(MessageType.QUIT.toString()) == 0) {
			result = makeQuitMessage(srcName);
		} else if (handle.compareTo(MessageType.HELLO.toString()) == 0) {
			result = makeHelloMessage(srcName, text);
		}else if (handle.compareTo(MessageType.UPDATE.toString()) == 0) {
			result = makeUpdateMessage(srcName,text);
		}else if (handle.compareTo(MessageType.UPDATEGROUP.toString()) == 0) {
			result = makeUpdateGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.DELETE.toString()) == 0) {
			result = makeDeleteMessage(srcName,text);
		}else if (handle.compareTo(MessageType.GROUP.toString()) == 0) {
			result = makeCreateGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.ADDGROUP.toString()) == 0) {
			result = makeAddToGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.DFGROUP.toString()) == 0) {
			result = makeDeleteFromGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.DGROUP.toString()) == 0) {
			result = makeDeleteGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.USERMSG.toString()) == 0) {
			result = makeUserMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.GROUPMSG.toString()) == 0) {
			result = makeGroupMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.RECALLMSG.toString()) == 0) {
			result = makeRecallMessage(srcName,text);
		}
		
		else if (handle.compareTo(MessageType.GROUPRECALLMSG.toString()) == 0) {
			result = makeRecallGroupMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.RECALLANYMSG.toString()) == 0) {
			result = makeRecallAnyMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.RECALLMSGID.toString()) == 0) {
			result = makeRecallMessageId(srcName,text);
		}
		else if (handle.compareTo(MessageType.RECALLGMSGID.toString()) == 0) {
			result = makeRecallGroupMessageId(srcName,text);
		}
		else if (handle.compareTo(MessageType.SEARCHSENDERMSG.toString()) == 0) {
			result = makeSearchSenderMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.SEARCHRECEIVERMSG.toString()) == 0) {
			result = makeSearchReceiverMessage(srcName,text);
		}
		else if (handle.compareTo(MessageType.SEARCHTIMEMSG.toString()) == 0) {
			result = makeSearchTimeMessage(srcName, text);
		}
		else if (handle.compareTo(MessageType.FILEMSG.toString()) == 0) {
			result = makeFileMessage(srcName, fileByte, extension);
		}
		else if (handle.compareTo(MessageType.LISTGROUP.toString()) == 0) {
			result = makeListGroupMessage(srcName,text);
		}else if (handle.compareTo(MessageType.BROADCAST.toString()) == 0) {
			result = makeBroadcastMessage(srcName, text);
		} else if (handle.compareTo(MessageType.ACKNOWLEDGE.toString()) == 0) {
			result = makeAcknowledgeMessage(srcName);
		} else if (handle.compareTo(MessageType.NO_ACKNOWLEDGE.toString()) == 0) {
			result = makeNoAcknowledgeMessage();
		} else if (handle.compareTo(MessageType.LOGGERMSG.toString()) == 0) {
			result = makeLoggerMessage(srcName, text);
		}
		return result;
	}

	/**
	 * Create a new message to reject the bad login attempt.
	 * 
	 * @return Instance of Message that rejects the bad login attempt.
	 */
	public static Message makeNoAcknowledgeMessage() {
		return new Message(MessageType.NO_ACKNOWLEDGE);
	}

	/**
	 * Create a new message to acknowledge that the user successfully logged as the
	 * name <code>srcName</code>.
	 * 
	 * @param srcName Name the user was able to use to log in.
	 * @return Instance of Message that acknowledges the successful login.
	 */
	public static Message makeAcknowledgeMessage(String srcName) {
		return new Message(MessageType.ACKNOWLEDGE, srcName);
	}

	/**
	 * Create a new message for the early stages when the user logs in without all
	 * the special stuff.
	 * 
	 * @param myName Name of the user who has just logged in.
	 * @return Instance of Message specifying a new friend has just logged in.
	 */
	public static Message makeLoginMessage(String myName, String myPassword, String option) {
		return new Message(MessageType.HELLO, myName, myPassword, option);
	}

	public static Message makeHelloMessage(String myName, String text) {
		return new Message(MessageType.HELLO, myName, text);
	}


	/**
	 * Update a user
	 */
	public static Message makeUpdateMessage(String myName, String text) {
		return new Message(MessageType.UPDATE, myName, text);
	}

	public static Message makeUpdateGroupMessage(String myName, String text) {
		return new Message(MessageType.UPDATEGROUP, myName, text);
	}

	public static Message makeListGroupMessage(String myName, String text) {
		return new Message(MessageType.LISTGROUP, myName, text);
	}

	public static Message makeDeleteMessage(String myName, String text) {
		return new Message(MessageType.DELETE, myName, text);
	}

	public static Message makeCreateGroupMessage(String myName, String text) {
		return new Message(MessageType.GROUP, myName, text);
	}

	public static Message makeAddToGroupMessage(String myName, String text) {
		return new Message(MessageType.ADDGROUP, myName, text);
	}

	public static Message makeDeleteFromGroupMessage(String myName, String text) {
		return new Message(MessageType.DFGROUP, myName, text);
	}

	public static Message makeDeleteGroupMessage(String myName, String text) {
		return new Message(MessageType.DGROUP, myName, text);
	}

	/**
	 * Creates a message that is to be sent to another user
	 * @param myName sender name
	 * @param text text message
	 * @return a Message of USERMSG type
	 */
	public static Message makeUserMessage(String myName, String text) {
		return new Message(MessageType.USERMSG, myName, text);
	}

	/**
	 * Creates a message that is to be sent to a group of users
	 * @param myName sender name
	 * @param text text message
	 * @return a Message of GROUPMSG type
	 */
	public static Message makeGroupMessage(String myName, String text) {
		return new Message(MessageType.GROUPMSG, myName, text);
	}

	/**
	 * Creates a message that is to be sent to another user
	 * @param myName sender name
	 * @param File file
	 * @return a Message of FILEMSG type
	 */
	public static Message makeFileMessage(String myName, byte[] byteFile, String extension) {
		return new Message(MessageType.FILEMSG, myName, byteFile, extension);
	}
	
	
	/**
	 * Creates a message that is to be sent to another user
	 * @param myName sender name
	 * @param text text message
	 * @return a Message of USERMSG type
	 */
	public static Message makeRecallMessage(String myName, String text) {
		return new Message(MessageType.RECALLMSG, myName, text);
	}
	
	public static Message makeRecallAnyMessage(String myName, String text) {
		return new Message(MessageType.RECALLANYMSG, myName, text);
	}
	
	public static Message makeRecallMessageId(String myName, String text) {
		return new Message(MessageType.RECALLMSGID, myName, text);
	}
	
	public static Message makeSearchSenderMessage(String myName, String text) {
		return new Message(MessageType.SEARCHSENDERMSG, myName, text);
	}
	public static Message makeSearchReceiverMessage(String myName, String text) {
		return new Message(MessageType.SEARCHRECEIVERMSG, myName, text);
	}
	public static Message makeRecallGroupMessageId(String myName, String text) {
		return new Message(MessageType.RECALLGMSGID, myName, text);
	}
	
	public static Message makeSearchTimeMessage(String myName, String text) {
		return new Message(MessageType.SEARCHTIMEMSG, myName, text);
	}
	
	public static Message makeRecallGroupMessage(String myName, String text) {
		return new Message(MessageType.GROUPRECALLMSG, myName, text);
	}

	/**
	 * Creates a message that turns on/off logger.
	 * @param myName sender name (admin)
	 * @param text string true or false
	 * @return
	 */
	public static Message makeLoggerMessage(String myName, String text) {
		return new Message(MessageType.LOGGERMSG, myName, text);
	}


	/**
	 * Return the type of this message.
	 * 
	 * @return MessageType for this message.
	 */
	public MessageType getType() {
		return msgType;
	}

	/**
	 * Return the name of the sender of this message.
	 * 
	 * @return String specifying the name of the message originator.
	 */
	public String getSender() {
		return msgSender;
	}

	/**
	 * Return the name of the sender of this message.
	 * 
	 * @return String specifying the name of the message originator.
	 */
	public String getOption() {
		return option;
	}

	/**
	 * Return the username of the receiver
	 * @return username of Receiver
	 */
	public String getReceiver(String text) {

		List<String> textList = Arrays.asList(text.split("-"));
		String receiver = textList.get(0);
		//receiver = receiver.substring(1, receiver.length());

		return receiver;
	}

	/**
	 * Remove the receiver username from text message
	 * @param text text message that contains receiver username
	 * @return message with receiver username removed
	 */
	public String removeReceiverNameFromMsg(String text) {
		int ind = text.indexOf("-");
		return text.substring(ind + 1 , text.length());
	}

	/**
	 * Return the text of this message.
	 * 
	 * @return String equal to the text sent by this message.
	 */
	public String getText() {
		return msgText;
	}

	/**
	 * Determine if this message is an acknowledgement message.
	 * 
	 * @return True if the message is an acknowledgement message; false otherwise.
	 */
	public boolean isAcknowledge() {
		return (msgType == MessageType.ACKNOWLEDGE);
	}

	/**
	 * Determine if this message is broadcasting text to everyone.
	 * 
	 * @return True if the message is a broadcast message; false otherwise.
	 */
	public boolean isBroadcastMessage() {
		return (msgType == MessageType.BROADCAST);
	}

	/**
	 * Determine if this message contains text which the recipient should display.
	 * 
	 * @return True if the message is an actual instant message; false if the
	 *         message contains data
	 */
	public boolean isDisplayMessage() {
		return (msgType == MessageType.BROADCAST);
	}

	/**
	 * Determine if this message is sent by a new client to log-in to the server.
	 * 
	 * @return True if the message is an initialization message; false otherwise
	 */
	public boolean isInitialization() {
		return (msgType == MessageType.HELLO);
	}

	/**
	 * Determine if this message is a message signing off from the IM server.
	 * 
	 * @return True if the message is sent when signing off; false otherwise
	 */
	public boolean terminate() {
		return (msgType == MessageType.QUIT);
	}


	/**
	 * Determine if this message is to a particular User
	 * @return True if message is of type USERMSG, false otherwise.
	 */
	public boolean isUserMessage() {
		return(msgType == MessageType.USERMSG);
	}


	/**
	 * Determine if this message is a group message
	 * @return True if message is of type GROUPMSG, false otherwise.
	 */
	public boolean isGroupMessage() {
		return(msgType == MessageType.GROUPMSG);
	}

	/**
	 * Determine if this message is a file message
	 * @return True if message is of type FILEMSG, false otherwise.
	 */
	public boolean isFileMessage() {
		return(msgType == MessageType.FILEMSG);
	}

	public boolean isSearchTimeMessage() {
		return(msgType == MessageType.SEARCHTIMEMSG);
	}


	/**
	 * Representation of this message as a String. This begins with the message
	 * handle and then contains the length (as an integer) and the value of the next
	 * two arguments.
	 * 
	 * @return Representation of this message as a String.
	 */
	@Override
	public String toString() {
		String result = msgType.toString();

		if (msgSender != null) {
			result += " " + msgSender.length() + " " + msgSender;
		} else {
			result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
		}
		if(msgType.toString() != "FIL")
		{
			if (msgText != null) {
				result += " " + msgText.length() + " " + msgText;
			} else {
				result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
			}
			if (option != null) {
				result += " " + option.length() + " " + option;
			}
		}
		else {
			int extLen = extension.length();
			if(fileByte != null ) {

				int len = fileByte.toString().length();
				result += " " + len + " " +fileByte;
			}
			else {
				result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
			}
			if(extension != null) {

				result += " " + extLen + " "+extension;
			}
			else {
				result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
			}
		}


		return result;
	}


}
