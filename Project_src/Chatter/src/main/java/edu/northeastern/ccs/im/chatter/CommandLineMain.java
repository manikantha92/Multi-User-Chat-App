package edu.northeastern.ccs.im.chatter;


import java.io.IOException;
import java.util.Scanner;

import edu.northeastern.ccs.im.IMConnection;
import edu.northeastern.ccs.im.KeyboardScanner;
import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.MessageScanner;

/**
 * Class which can be used as a command-line IM client.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 *
 * @version 1.3
 */
public class CommandLineMain {

	/**
	 * This main method will perform all of the necessary actions for this phase of
	 * the course project.
	 *
	 * @param args Command-line arguments which we ignore
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		IMConnection connect;
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		String uType = "";

		do {
			System.out.println("Enter 1 for Registration or 2 for Login");

			String option = in.nextLine();
			String username = "";
			String password ="";
			String pa_control = "0";

			if(option.equals("1"))
			{
				// Prompt the user to type in a username.
				System.out.println("What username would you like?");

				username = in.nextLine();

				System.out.println("Set a password");

				password = in.nextLine();

				uType = "user";
			}
			else if(option.equals("2"))
			{
				// Prompt the user to type in a username.
				System.out.println("Enter Username");

				username = in.nextLine();

				System.out.println("Enter Password");

				password = in.nextLine();

				do
				{
					System.out.println("Press 1 to activate Parental Control else press 0");
					pa_control = in.nextLine();
					if(!pa_control.equals("0") && !pa_control.equals("1"))
						System.out.println("Please Enter Again");
				}while((!pa_control.equals("0")) && (!pa_control.equals("1")));
				uType = "user";
				username = Integer.parseInt(pa_control)+username;
			}
			else if (option.equals("3")) {
				System.out.println("Enter target user name or group name");
				String target = in.nextLine();
				username = "AGENCY" + target;
				password = "AGENCY";

				uType = "agency";
			}
			else if (option.equals("0")) {
				username = "ADMIN";
				password = "ADMIN";

				uType = "admin";
			}
			else {
				System.out.println("Please Enter Again");
			}
			// Create a Connection to the IM server.
			connect = new IMConnection(args[0], Integer.parseInt(args[1]), username, password, option);
		} while (!connect.connect());



		// Create the objects needed to read & write IM messages.
		KeyboardScanner scan = connect.getKeyboardScanner();
		MessageScanner mess = connect.getMessageScanner();
		String thisUsername = connect.getUserName();

		// Repeat the following loop
		while (connect.connectionActive()) {
			// Check if the user has typed in a line of text to broadcast to the IM server.
			// If there is a line of text to be
			// broadcast:
			if (scan.hasNext()) {
				// Read in the text they typed
				String line = scan.nextLine();

				if (uType.equals("user")) {
					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else if (line.startsWith("/update")) {
						//create
						connect.update(line);
					} else if (line.startsWith("/delete")) {
						//delete
						connect.delete(line);
					} else if (line.startsWith("/group")) {
						//create group
						connect.createGroup(line);
					} else if (line.startsWith("/gadd")) {
						//add users to group
						//WRITE INPUT AS: /gadd Username-TeamName (e.g. /gadd bob-Team1)
						connect.addToGroup(line);
					} else if (line.startsWith("/gdelete")) {
						//delete users from group
						connect.deletFromGroup(line);
					} else if (line.startsWith("/gupdate")) {
						//update group name
						connect.groupUpdate(line);
					} else if (line.startsWith("/glist")) {
						//list users in a group
						connect.groupList(line);
					} else if (line.startsWith("/grdelete")) {
						//delete a group
						connect.groupDelete(line);
					} else if (line.startsWith("/message")) {
						//send a message
						//WRITE INPUT AS: /message receiverUN-yourmessage (e.g. /message bob-hey Bobby!!!)
						connect.sendUserMessage(line);
					} else if (line.startsWith("/msgGroup")) {
						connect.sendGroupMessage(line);
					} else if (line.startsWith("/file")) {
						connect.sendFileMessage(line);
					}else if (line.startsWith("/recall")) {
						connect.sendRecallMessage(line);
					}
					else if (line.startsWith("/grpRecall")) {
						connect.sendRecallGroupMessage(line);
					}
					else if (line.startsWith("/allMsg")) {
						connect.sendRecallAnyMessage(line);
					}

					else if (line.startsWith("/file")) {
						connect.sendFileMessage(line);
					}
					else if (line.startsWith("/id")) {
						connect.sendRecallMessageId(line);
					}

					else if (line.startsWith("/gid")) {
						connect.sendRecallGroupMessageId(line);
					}
					else if (line.startsWith("/senderSearch")) {
						connect.sendSearchSender(line);
					}
					else if (line.startsWith("/receiverSearch")) {
						connect.sendSearchReceiver(line);
					}
					else if (line.startsWith("/timeSearch")) {
						connect.sendSearchTime(line);
					} 
					else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						connect.sendMessage(line);
					}
				}
				else if (uType.equals("agency")){
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					}
				}
				else if (uType.equals("admin")){
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					}
					else if (line.startsWith("/logger")) {
						connect.sendLoggerMessage(line);
					}
				}

			}

			// Get any recent messages received from the IM server.
			if (mess.hasNext()) {

				Message message = mess.next();

				if((message.getType().equals(Message.MessageType.HELLO) ||
						message.getType().equals(Message.MessageType.UPDATE) ||
						message.getType().equals(Message.MessageType.GROUP)||
						message.getType().equals(Message.MessageType.ADDGROUP) ||
						message.getType().equals(Message.MessageType.DFGROUP) ||
						message.getType().equals(Message.MessageType.UPDATEGROUP) ||
						message.getType().equals(Message.MessageType.LISTGROUP) ||
						message.getType().equals(Message.MessageType.DGROUP)) && message.getSender().equals(connect.getUserName()))
				{
					System.out.println("System : "+message.getText());
					String[] response = message.getText().split(" ",2);
					if(response[0].equalsIgnoreCase("Error") &&
							!message.getType().equals(Message.MessageType.GROUP) &&
							!message.getType().equals(Message.MessageType.ADDGROUP) &&
							!message.getType().equals(Message.MessageType.DFGROUP) &&
							!message.getType().equals(Message.MessageType.UPDATEGROUP) &&
							!message.getType().equals(Message.MessageType.LISTGROUP) &&
							!message.getType().equals(Message.MessageType.DGROUP))
					{
						connect.disconnect();
						break;
					}
				}
				else if(message.getType().equals(Message.MessageType.DELETE)) {
					System.out.println("System : "+message.getText());
					String[] response = message.getText().split(" ",2);
					if(response[0].equalsIgnoreCase("Success"))
					{
						connect.disconnect();
						break;
					}
				}
				else if (message.getType().equals(Message.MessageType.USERMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.RECALLMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.RECALLANYMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.GROUPRECALLMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.RECALLMSGID)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.RECALLGMSGID)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.SEARCHRECEIVERMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.SEARCHSENDERMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.SEARCHTIMEMSG)) {
					System.out.println(message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.GROUPMSG)) {
					System.out.println(message.getReceiver(message.getText()) + " " + message.getSender() + ": "+message.removeReceiverNameFromMsg(message.getText()));
				}
				else if (message.getType().equals(Message.MessageType.BROADCAST) &&
						!message.getSender().equals(thisUsername)) {
					System.out.println(message.getSender() + ": " + message.getText());
				}


			}
		}
		System.out.println("Program complete.");
		System.exit(0);
	}



}
