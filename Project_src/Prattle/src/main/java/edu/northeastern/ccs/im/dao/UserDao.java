package edu.northeastern.ccs.im.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import edu.northeastern.ccs.im.model.User;

public class UserDao {
	protected ConnectionManager connectionManager;

	// Single pattern: instantiation is limited to one object.
	private static UserDao instance = null;
	private UserDao() {
		connectionManager = new ConnectionManager();
	}
	public static UserDao getInstance() {
		if(instance == null) {
			instance = new UserDao();
		}
		return instance;
	}

	//The function creates user in the database
	public User create(User user) throws SQLException {
		String insertUser = "INSERT INTO user(username,password) VALUES(?,?);";
		ResultSet resultKey = null;

		try (Connection connection = connectionManager.getConnection();
				PreparedStatement insertStmt = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
		){
			insertStmt.setString(1, user.getUserName());
			insertStmt.setString(2, user.getPassword());
			insertStmt.executeUpdate();
			resultKey = insertStmt.getGeneratedKeys();
			int userId;
			if(resultKey.next()) {
				userId = resultKey.getInt(1);
			} else {
				throw new SQLException("Unable to retrieve auto-generated key.");
			}
			user.setUserId(userId);
			return user;
		} finally {
			if(resultKey != null) {
				resultKey.close();
			}
		}
	}

	//This function checks if the user exists in the system taking both username and password into account.
	public boolean checkUserExists(String userName, String password) throws SQLException {
		String selectUser =
				"SELECT * from user where username = ?";
		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement selectStmt = connection.prepareStatement(selectUser);
				){
			selectStmt.setString(1, userName);
			results = selectStmt.executeQuery();
			if(results.next()) {
				String encryptedPassword = results.getString("password");
				//the encrypted password is matched and checked with the original password.
				return BCrypt.checkpw(password, encryptedPassword);
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return false;
	}

	//This function checks if the user exists in the system taking username into account.
	public boolean checkUserNameExists(String userName) throws SQLException {
		String selectUser =
				"SELECT username from user where username = ?";
		ResultSet results = null;

		try (Connection connection = connectionManager.getConnection();
				PreparedStatement selectStmt = connection.prepareStatement(selectUser);
				){
			selectStmt.setString(1, userName);
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

	//This function finds the userid of the user by its username
	public int findUserIdByName(String userName) throws SQLException {
		String selectUser =
				"SELECT userid from user where username = ?";
		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement selectStmt = connection.prepareStatement(selectUser);
				){
			selectStmt.setString(1, userName);
			results = selectStmt.executeQuery();
			if(results.next()) {
				return results.getInt("userid");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return 0;
	}

	//This function finds the username of the user by its id
	public String findUserNameById(int userid) throws SQLException {
		String selectUser =
				"SELECT username from user where userid = ?";
		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectUser)
    ){
			selectStmt.setInt(1, userid);
			results = selectStmt.executeQuery();
			if(results.next()) {
				return results.getString("username");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return null;
	}

	/**
	 * Function updates the username
	 * @param userName old username
	 * @param newName new username
	 * @return success if operation is success
	 * @throws SQLException
	 */
	public String updateUserName(String userName, String newName) throws SQLException {
		boolean exists = checkUserNameExists(userName);
		if(exists) {
			String updateUser = "UPDATE user SET username=? WHERE username=?;";

			try (Connection connection = connectionManager.getConnection();
					PreparedStatement updateStmt = connection.prepareStatement(updateUser);
					){
				updateStmt.setString(1, newName);
				updateStmt.setString(2, userName);
				updateStmt.executeUpdate();
				return "success";
			}
		}
		else {
			return "Invalid";
		}
	}

	//This function helps the the user delete himself from system
	public String delete(String username, GroupsDao groupsDao) throws SQLException {
		String deleteUser = "DELETE FROM user WHERE username=?;";
		try (Connection connection = connectionManager.getConnection();
				PreparedStatement deleteStmt = connection.prepareStatement(deleteUser);
				){
			deleteStmt.setString(1, username);
			int userId = findUserIdByName(username);
			int affectedRows = deleteStmt.executeUpdate();
			groupsDao.deleteUserFromGroup(userId);
			if (affectedRows == 0) {
				throw new SQLException("No records available to delete");
			}
			return "success";
		}
	}

	public List<String> getUnreadMessages(String sender, GroupMessageDao groupMessageDao) throws SQLException {
		String selectUser =
				"SELECT msgText,senderId from user "
				+ "inner join message on user.userid = message.receiverid where username = ? "
				+ "and isread = ? and isRecalled = ? order by sentTime desc;";

		ResultSet results = null;
		List<String> msgList = new ArrayList<>();
		msgList.addAll(groupMessageDao.getUnreadGroupMessages(sender, UserDao.getInstance()));

		try(Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectUser);
    ) {
			selectStmt.setString(1, sender);
			selectStmt.setBoolean(2, false);
			selectStmt.setBoolean(3, false);
			results = selectStmt.executeQuery();
			while(results.next()) {
				String msgText = results.getString("msgText");
				int senderid = results.getInt("senderId");
				String senderName = findUserNameById(senderid);
				msgList.add(senderName +" : " +msgText + "\n");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return msgList;
	}

	public String updateReadReceipt(String sender, GroupMessageDao groupMessageDao) throws SQLException {

		String updateUser = "UPDATE message set isread = ? where receiverid = ? and isread = ? and isRecalled = ?;";

		int receiverIdUnread = findUserIdByName(sender);
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement updateStmt = connection.prepareStatement(updateUser);
    ){
			updateStmt.setBoolean(1, true);
			updateStmt.setInt(2, receiverIdUnread);
			updateStmt.setBoolean(3, false);
			updateStmt.setBoolean(4, false);
			updateStmt.executeUpdate();
			groupMessageDao.updateReadReceipt(sender, UserDao.getInstance());
			return "success";
		}
	}

}
