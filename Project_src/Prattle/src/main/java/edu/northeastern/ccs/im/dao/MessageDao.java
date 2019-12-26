package edu.northeastern.ccs.im.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


import edu.northeastern.ccs.im.model.Message;



public class MessageDao {

	protected ConnectionManager connectionManager;

	// Single pattern: instantiation is limited to one object.
	private static MessageDao instance = null;
	protected MessageDao() {
		connectionManager = new ConnectionManager();
	}
	public static MessageDao getInstance() {
		if(instance == null) {
			instance = new MessageDao();
		}
		return instance;
	}

	public PreparedStatement initialiseConnection(String str) throws SQLException {
		PreparedStatement selectStmt = null;
		try {
			Connection connection = connectionManager.getConnection();
			selectStmt = connection.prepareStatement(str, Statement.RETURN_GENERATED_KEYS);
		} catch (SQLException e) {
			throw new SQLException("Unable to retrieve auto-generated key.");
		}
		return selectStmt;
	}


	public Message createMessage(String receiverName, String message, String senderName, boolean receiverAvailable, UserDao usersDao, String senderIPAddress, String recvIp) throws SQLException {

		if(usersDao.checkUserNameExists(senderName) && usersDao.checkUserNameExists(receiverName))
		{
			int receiverId = usersDao.findUserIdByName(receiverName);
			int senderId = usersDao.findUserIdByName(senderName);
			String insertMessage = "INSERT INTO message(msgText,receiverId,senderId,isread, senderAddress, receiverAddress) VALUES(?,?,?,?,?,?);";
			ResultSet resultKey = null;
			try{
				PreparedStatement insertStmt = initialiseConnection(insertMessage);
				insertStmt.setString(1, message);
				insertStmt.setInt(2, receiverId);
				insertStmt.setInt(3, senderId);
				insertStmt.setBoolean(4, receiverAvailable);
				insertStmt.setString(5, senderIPAddress);
				insertStmt.setString(6, recvIp);
				insertStmt.executeUpdate();
				resultKey = insertStmt.getGeneratedKeys();
				Message messageModel = new Message(senderId,receiverId,message);
				int messageId = -1;
				if(resultKey.next()) {
					messageId = resultKey.getInt(1);
				}
				messageModel.setMsgId(messageId);
				return messageModel;
			} finally {
				if(resultKey != null) {
					resultKey.close();
				}
			}
		}
		return null;

	}
	public String createRecallMessage(String receiverName, String senderName,MessageDao msgDao, UserDao userDao) throws SQLException {

		if(userDao.checkUserNameExists(senderName) && userDao.checkUserNameExists(receiverName))
		{
			int receiverId = userDao.findUserIdByName(receiverName);
			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = " SELECT MAX(msgId) as messageId from message where "
					+ "receiverId = ? and senderId = ?;";
			ResultSet results = null;

			try{
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setInt(1, receiverId);
				selectStmt.setInt(2, senderId);
				results = selectStmt.executeQuery();
				while(results.next()) {

					return (updateRecallStatus(results.getInt("messageId"), msgDao));
				}
			} finally {
				if(results != null) {
					results.close();
				}
			}

		}
		return null;
	}




	public List<String> createRecallAnyMessage(String senderName,MessageDao msgDao, GroupMessageDao groupMessageDao, UserDao userDao) throws SQLException {
		List<String> msgList = new ArrayList<>();
		if(userDao.checkUserNameExists(senderName))
		{
			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = " select msgId, username, msgText, sentTime from message m inner join "
					+ "user u "+ 
					"on m.receiverId = u.userid " + 
					"where senderId = ? and isRecalled=?;";
			ResultSet results = null;
			List<String> grpMsgList = groupMessageDao.createRecallAnyGroupMessage(senderName,userDao);
			msgList.addAll(grpMsgList);
			try{
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setInt(1, senderId);
				selectStmt.setBoolean(2, false);
				results = selectStmt.executeQuery();
				while(results.next()) {
					String msgText = results.getString("msgText");
					int msgId = results.getInt("msgId");
					String receiverName = results.getString("username");

					Timestamp d = results.getTimestamp("sentTime");
					msgList.add(d+" : " +msgText + " has message id "+msgId+" received by user "+receiverName);
				}

				return msgList;
			} finally {
				if(results != null) {
					results.close();
				}
			}

		}
		return msgList;


	}

	public Boolean checkMessageExists(int msgId) throws SQLException {
		ResultSet results = null;
		String queryMsg ="SELECT msgText from message where msgId = ?";
		try{
			PreparedStatement queryStmt = initialiseConnection(queryMsg);
			queryStmt.setInt(1, msgId);
			results = queryStmt.executeQuery();
			if(results.next())
				return true;
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return false;
	}


	public String createRecallAnyMessageId(String senderName, String msgId, MessageDao msgDao, UserDao userDao) throws SQLException {

		if( (userDao.checkUserNameExists(senderName)) && (checkMessageExists(Integer.parseInt(msgId))))
		{
			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = " UPDATE message set isRecalled = ? WHERE msgId = ? AND senderId = ?;";

			try{
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setBoolean(1, true);
				selectStmt.setInt(2, Integer.parseInt(msgId));
				selectStmt.setInt(3, senderId);
				selectStmt.executeUpdate();
				return "Success";
			}
			finally {

			}
		}
		return null;
	}


	private String updateRecallStatus(int msgId, MessageDao msgDao) throws SQLException {

		String updateRecallStatus = "UPDATE message set isRecalled = ? WHERE msgId=? ;";

		try{
			PreparedStatement updateStmt = initialiseConnection(updateRecallStatus);
			updateStmt.setBoolean(1, true);
			updateStmt.setInt(2, msgId);
			updateStmt.executeUpdate();
			return "success";
		}
		finally {

		}
	}
	public List<String> createTimeSearchMessage(String timeStamp, String senderName, MessageDao msgDao, GroupMessageDao groupMessageDao,
			UserDao userDao) throws SQLException {
		List<String> msgList = new ArrayList<>();
		if(userDao.checkUserNameExists(senderName))
		{
			java.sql.Timestamp timeStampDate = null;
			try {
				DateFormat formatter;
				formatter = new SimpleDateFormat("dd-MM-yyyy");
				// you can change format of date
				java.util.Date date = formatter.parse(timeStamp);
				timeStampDate = new Timestamp(date.getTime());


			} catch (ParseException e) {
				return null;   
			}

			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = " select msgId, username, msgText, receiverId, senderId, sentTime from message m inner join "
					+ "user u "+ 
					"on m.receiverId = u.userid " + 
					"where senderId = ? or receiverId = ? and isRecalled=? and sentTime < ?;";
			ResultSet results = null;
			List<String> grpMsgList = groupMessageDao.createSearchTimeGroupMessage(senderId, timeStampDate, GroupMessageDao.getInstance(),userDao);
			msgList.addAll(grpMsgList);
			try{
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setInt(1, senderId);
				selectStmt.setInt(2,senderId);
				selectStmt.setBoolean(3, false); 
				selectStmt.setTimestamp(4, timeStampDate);
				results = selectStmt.executeQuery();
				while(results.next()) {
					String msgText = results.getString("msgText");
					int receiverId = results.getInt("receiverId");
					int senderId2 = results.getInt("senderId");
					String senderName2 = userDao.findUserNameById(senderId2);
					String receiverName = userDao.findUserNameById(receiverId);
					Timestamp d = results.getTimestamp("sentTime");
					msgList.add(d+" : " +msgText + " sent by "+senderName2+" and received by user "+receiverName);
				}

				return msgList;
			} finally {
				if(results != null) {
					results.close();
				}
			}

		}
		return msgList;
	}

	public List<String> createReceiverSearchMessage(String receiverName, String senderName, MessageDao msgDao, GroupMessageDao groupMessageDao,
			UserDao userDao) throws SQLException {
		List<String> msgList = new ArrayList<>();
		if(userDao.checkUserNameExists(senderName) && userDao.checkUserNameExists(receiverName))
		{
			int senderId = userDao.findUserIdByName(senderName);
			int receiverId = userDao.findUserIdByName(receiverName);
			String latestMsgId = " select msgId, username, msgText, sentTime from message m inner join "
					+ "user u "+ 
					"on m.receiverId = u.userid " + 
					"where senderId = ? and receiverId = ? and isRecalled=?;";
			ResultSet results = null;

			List<String> grpMsgList = groupMessageDao.createSearchReceiverGroupMessage(senderId, receiverId,userDao);
			msgList.addAll(grpMsgList);
			try {
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setInt(1, senderId);
				selectStmt.setInt(2,receiverId);
				selectStmt.setBoolean(3, false);
				results = selectStmt.executeQuery();
				while(results.next()) {
					String msgText = results.getString("msgText");
					int msgId = results.getInt("msgId");



					Timestamp d = results.getTimestamp("sentTime");
					msgList.add(d+" : " +msgText + " has message id "+msgId+" received by user "+receiverName);
				}

				return msgList;
			} finally {
				if(results != null) {
					results.close();
				}
			}

		}
		return msgList;
	}


	public String updateRecvIp(String sender, UserDao userDao, String ip) throws SQLException {
		String insertMessage = "update message set receiverAddress = ? where receiverId= ? and isread = ?;";
		if(userDao.checkUserNameExists(sender)) {
			int senderId = userDao.findUserIdByName(sender);

			String[] recvAdd = ip.split(":");
			String recvIPAddress = recvAdd[0].substring(1, recvAdd[0].length());
			PreparedStatement insertStmt= null;
			try
			{
				insertStmt = initialiseConnection(insertMessage);
				insertStmt.setString(1, recvIPAddress);
				insertStmt.setInt(2, senderId);
				insertStmt.setInt(3, 0);
				insertStmt.executeUpdate();
				return "success";
			} 
			finally {

		
			}
		}
		else return "invalid";
	}
}
