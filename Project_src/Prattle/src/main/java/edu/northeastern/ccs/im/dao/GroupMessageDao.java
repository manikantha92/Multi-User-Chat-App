package edu.northeastern.ccs.im.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import edu.northeastern.ccs.im.model.GroupMessage;
import edu.northeastern.ccs.im.model.GroupReceiverMapping;

public class GroupMessageDao {

	protected ConnectionManager connectionManager;

	// Single pattern: instantiation is limited to one object.
	private static GroupMessageDao instance = null;
	protected GroupMessageDao() {
		connectionManager = new ConnectionManager();
	}
	public static GroupMessageDao getInstance() {
		if(instance == null) {
			instance = new GroupMessageDao();
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


	public GroupMessage createGroupMessage(Map<String, Boolean> recipientsAvailable, String message,
			String senderName, String groupName,
			UserDao usersDao, GroupsDao groupsDao) throws SQLException
	{

		if(usersDao.checkUserNameExists(senderName) && groupsDao.checkGroupExists(groupName)) {
			int senderId = usersDao.findUserIdByName(senderName);
			int groupId = groupsDao.findGroupIdByName(groupName);
			String insertMessage = "INSERT INTO groupmessage(msgText,groupId,senderId) VALUES(?,?,?);";
			ResultSet resultKey = null;

			try{
				PreparedStatement insertStmt = initialiseConnection(insertMessage);
				insertStmt.setString(1, message);
				insertStmt.setInt(2, groupId);
				insertStmt.setInt(3, senderId);
				insertStmt.executeUpdate();
				resultKey = insertStmt.getGeneratedKeys();
				GroupMessage messageModel = new GroupMessage(senderId,groupId,message);
				int messageId = -1;
				if(resultKey.next()) {
					messageId = resultKey.getInt(1);
				} else {
					throw new SQLException("Unable to retrieve auto-generated key.");
				}
				messageModel.setGrpMsgId(messageId);

				if(!recipientsAvailable.isEmpty()) {

					for(String user : recipientsAvailable.keySet()) {
						int userId = usersDao.findUserIdByName(user);
						if(userId != 0) {
							addMsgToReceiver(messageId, userId, recipientsAvailable.get(user));
						}
					}
				}
				return messageModel;
			}  finally {
				if(resultKey != null) {
					resultKey.close();
				}
			}
		}
		return null;
	}
	private GroupReceiverMapping addMsgToReceiver(int groupMsgId, int userId, boolean isread) throws SQLException {

		String insertMessage = "INSERT INTO groupreceivermapping(GroupMsgId,receiverId, isread) VALUES(?,?,?);";
		ResultSet resultKey = null;

		try{
			PreparedStatement insertStmt = initialiseConnection(insertMessage);
			insertStmt.setInt(1, groupMsgId);
			insertStmt.setInt(2, userId);
			insertStmt.setBoolean(3, isread);
			insertStmt.executeUpdate();
			resultKey = insertStmt.getGeneratedKeys();

			return new GroupReceiverMapping(groupMsgId,userId);
		} finally {
			if(resultKey != null) {
				resultKey.close();
			}
		}

	}
	public List<String> getUnreadGroupMessages(String sender, UserDao userDao) throws SQLException {
		String selectUser =
				"select  groupname, gm.senderId, msgText "
						+ "from groups g inner join groupmessage gm on g.groupid = gm.groupid "
						+ "inner join groupreceivermapping gr on gm.grpMsgId=gr.groupMsgId "
						+ "inner join user u on u.userid = gr.receiverid "
						+ "where gr.isread=? and username =? and isRecalled = ?;";

		ResultSet results = null;
		ArrayList<String> msgList = new ArrayList<>();
		try {
			PreparedStatement selectStmt = initialiseConnection(selectUser);
			selectStmt.setBoolean(1, false);
			selectStmt.setString(2, sender);
			selectStmt.setBoolean(3,false);
			results = selectStmt.executeQuery();
			while(results.next()) {
				String msgText = results.getString("msgText");
				int senderid = results.getInt("senderId");
				String senderName = userDao.findUserNameById(senderid);
				String groupName = results.getString("groupname");

				msgList.add(senderName +" in group " +groupName + " : "+msgText + "\n");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return msgList;
	}

	public String updateReadReceipt(String sender, UserDao userDao) throws SQLException {
		String updateUser = "UPDATE groupreceivermapping set isread = ? where receiverid = ? and isread = ? and isRecalled = ?;";

		int receiverIdUnread = userDao.findUserIdByName(sender);
		try {
			PreparedStatement updateStmt = initialiseConnection(updateUser);
			updateStmt.setBoolean(1, true);
			updateStmt.setInt(2, receiverIdUnread);
			updateStmt.setBoolean(3, false);
			updateStmt.setBoolean(4, false);
			updateStmt.executeUpdate();
			return "success";
		}finally {
			
		}
	}
	public String createRecallGroupMessage( String senderName,
			String groupName, UserDao usersDao, GroupsDao groupsDao) throws SQLException
	{


		if(usersDao.checkUserNameExists(senderName) && groupsDao.checkGroupExists(groupName)) {
			int senderId = usersDao.findUserIdByName(senderName);
			int groupId = groupsDao.findGroupIdByName(groupName);
			String getMaxGrpId = 	"select  MAX(grpMsgId) as groupMessageId\r\n" + 
					"from groups g inner join groupmessage gm on g.groupid = gm.groupid \r\n" + 
					"inner join groupreceivermapping gr on gm.grpMsgId=gr.groupMsgId \r\n" + 
					"inner join user u on u.userid = gr.receiverid \r\n" + 
					"where g.groupId=? and gm.senderId =?;";
			ResultSet results = null;
			try{

				PreparedStatement selectStmt = initialiseConnection(getMaxGrpId);
				selectStmt.setInt(1, groupId);
				selectStmt.setInt(2, senderId);

				results = selectStmt.executeQuery();

				while(results.next()) {

					return (updateRecallStatus(results.getInt("groupMessageId")));
				}

			}  finally {
				if(results != null) {
					results.close();
				}
			}
		}
		return null;
	}

	
	public String createRecallAnyGroupMessageId(String senderName, String msgId, UserDao userDao) throws SQLException {

		if( (userDao.checkUserNameExists(senderName)) && (checkMessageExists(Integer.parseInt(msgId))))
		{
			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = "UPDATE groupmessage g " + 
					"inner join groupreceivermapping gr " + 
					"on g.grpMsgId=gr.groupMsgId " + 
					"set isRecalled=? " + 
					"where grpMsgId=? and senderId=?;";

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

	public Boolean checkMessageExists(int msgId) throws SQLException {
		String selectMsg =
				"SELECT msgText from groupmessage where grpMsgId = ?";
		ResultSet results = null;
		try{
			PreparedStatement selectStmt = initialiseConnection(selectMsg);
			selectStmt.setInt(1, msgId);
			results = selectStmt.executeQuery();
			if(results.next()) {
				return true;
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return false;
	}

	private String updateRecallStatus(int msgId) throws SQLException {

		String updateRecallStatus = "UPDATE groupreceivermapping set isRecalled = ? WHERE groupMsgId=? ;";

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

	public List<String> createRecallAnyGroupMessage(String senderName,
			UserDao userDao) throws SQLException {

		if(userDao.checkUserNameExists(senderName)) 
		{

			int senderId = userDao.findUserIdByName(senderName);
			String latestMsgId = " SELECT grpMsgId, msgText, sentTime, groupName FROM groupmessage g " + 
					"inner join groupreceivermapping gr " + 
					"on g.grpMsgId=gr.groupMsgId " + 
					"inner join user u on gr.receiverId = u.userid " + 
					"inner join groups grp on grp.groupId = g.groupId "+
					"where senderId=? and isRecalled=? " + 
					"group by grpMsgId;";

			ResultSet results = null;
			List<String> msgList = new ArrayList<>();
			try{
				PreparedStatement selectStmt = initialiseConnection(latestMsgId);
				selectStmt.setInt(1, senderId);
				selectStmt.setBoolean(2, false);
				results = selectStmt.executeQuery();
				while(results.next()) {
					String msgText = results.getString("msgText");
					int msgId = results.getInt("grpMsgId");
					String receiverName = results.getString("groupName");

					Timestamp d = results.getTimestamp("sentTime");
					msgList.add(d+" : " +msgText + " has message id "+msgId+" received by group "+receiverName);
				}

				return msgList;
			} finally {
				if(results != null) {
					results.close();
				}
			}

		}
		return null;
	}
	public List<String> createSearchReceiverGroupMessage(int senderId, int receiverId,
			UserDao userDao) throws SQLException {

		String selectUser =
				"SELECT  msgText, sentTime, username, groupName FROM groupmessage g " + 
						"inner join groupreceivermapping gr " + 
						"on g.grpMsgId=gr.groupMsgId " + 
						"inner join user u on gr.receiverId = u.userid " + 
						"inner join groups grp on grp.groupId = g.groupId " + 
						"where senderId=? and receiverId = ? and isRecalled=? " + 
						"group by grpMsgId;";

		ResultSet results = null;
		ArrayList<String> msgList = new ArrayList<>();
		try{
			PreparedStatement selectStmt = initialiseConnection(selectUser);
			selectStmt.setInt(1, senderId);
			selectStmt.setInt(2, receiverId);
			selectStmt.setBoolean(3,false);
			results = selectStmt.executeQuery();
			while(results.next()) {
				String msgText = results.getString("msgText");
				Timestamp sentTime = results.getTimestamp("sentTime");
				String receiverName = results.getString("username");
				String senderName = userDao.findUserNameById(senderId);
				String groupName = results.getString("groupname");

				msgList.add(senderName +" at "+sentTime+" in group " +groupName + " : "+msgText + " received by "+receiverName+"\n");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return msgList;
	}

	public List<String> createSearchTimeGroupMessage(int senderId, Timestamp timeStamp, GroupMessageDao instance2,
			UserDao userDao) throws SQLException {

		String selectUser =
				"SELECT  msgText, sentTime, username, groupName FROM groupmessage g " + 
						"inner join groupreceivermapping gr " + 
						"on g.grpMsgId=gr.groupMsgId " + 
						"inner join user u on gr.receiverId = u.userid " + 
						"inner join groups grp on grp.groupId = g.groupId " + 
						"where senderId=? or receiverId = ? and isRecalled=? and sentTime < ? " + 
						"group by grpMsgId;";
    
		ResultSet results = null;
		ArrayList<String> msgList = new ArrayList<>();
		try{
			PreparedStatement selectStmt = initialiseConnection(selectUser);
			selectStmt.setInt(1, senderId);
			selectStmt.setInt(2, senderId);
			selectStmt.setBoolean(3,false);
			selectStmt.setTimestamp(4, timeStamp);
			results = selectStmt.executeQuery();
			while(results.next()) {
				String msgText = results.getString("msgText");
				Timestamp sentTime = results.getTimestamp("sentTime");
				String receiverName = results.getString("username");
				String senderName = userDao.findUserNameById(senderId);
				String groupName = results.getString("groupname");

				msgList.add(senderName +" at "+sentTime+" in group " +groupName + " : "+msgText + " received by "+receiverName+"\n");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return msgList;
	}
}
