package edu.northeastern.ccs.im.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import edu.northeastern.ccs.im.model.Groups;

public class GroupsDao {

	protected ConnectionManager connectionManager;

	private static final String SUCCESS = "success";
	private static final String ERROR = "error";

	// Single pattern: instantiation is limited to one object.
	private static GroupsDao instance = null;
	protected GroupsDao() {
		connectionManager = new ConnectionManager();
	}
	public static GroupsDao getInstance() {
		if(instance == null) {
			instance = new GroupsDao();
		}
		return instance;
	}

	public Groups createGroup(String groupName, String userName, UserDao usersDao) throws SQLException {
		int userId = usersDao.findUserIdByName(userName);
		if(!checkGroupExists(groupName) && usersDao.checkUserNameExists(userName)
            && !checkUserInGroup(groupName,userId)) {
			String insertGroup = "INSERT INTO groups(groupName,userId) VALUES(?,?);";
      ResultSet resultKey = null;
			try (Connection connection = connectionManager.getConnection();
           PreparedStatement insertStmt = connection
                   .prepareStatement(insertGroup, Statement.RETURN_GENERATED_KEYS);)
      {
				insertStmt.setString(1, groupName);
				insertStmt.setInt(2, userId);
				insertStmt.executeUpdate();
        resultKey = insertStmt.getGeneratedKeys();

				Groups group = new Groups(groupName,userId);
				int groupId;
				if(resultKey.next()) {
					groupId = resultKey.getInt(1);
				} else {
					throw new SQLException("Unable to retrieve auto-generated key.");
				}
				group.setGroupId(groupId);
				return group;
			} finally {
			  if (resultKey != null) {
          resultKey.close();
        }
      }
		}
		return null;
	}

	public Groups addtoGroup(String groupName, String userName, UserDao usersDao) throws SQLException {
		int userId = usersDao.findUserIdByName(userName);

		if (!checkGroupExists(groupName)) {
		  return createGroup(groupName, userName, usersDao);
    } else if(usersDao.checkUserNameExists(userName) && !checkUserInGroup(groupName,userId)) {

		  String insertGroup = "INSERT INTO groups(groupName,userId) VALUES(?,?);";
		  ResultSet resultKey = null;
		  try (Connection connection = connectionManager.getConnection();
           PreparedStatement insertStmt = connection.prepareStatement(insertGroup, Statement.RETURN_GENERATED_KEYS);
      ){
		    insertStmt.setString(1, groupName);
		    insertStmt.setInt(2, userId);
		    insertStmt.executeUpdate();
		    resultKey = insertStmt.getGeneratedKeys();
		    Groups group = new Groups(groupName,userId);
		    int groupId = -1;
		    if(resultKey.next()) {
		      groupId = resultKey.getInt(1);
		    } else {
		      throw new SQLException("Unable to retrieve auto-generated key.");
		    }
		    group.setGroupId(groupId);
		    return group;
		  } finally {
		    if(resultKey != null) {
		      resultKey.close();
		    }
		  }
		}
		return null;
	}




	public String deleteUserFromGroup(String groupName, String userName, UserDao usersDao) throws SQLException {
		int userId = usersDao.findUserIdByName(userName);
		if(usersDao.checkUserNameExists(userName)
            && checkUserInGroup(groupName,userId) && checkGroupExists(groupName)) {
			String deleteUser = "DELETE FROM groups WHERE userId=? and groupName = ?;";
			try (Connection connection = connectionManager.getConnection();
          PreparedStatement deleteStmt = connection.prepareStatement(deleteUser);)
      {
				deleteStmt.setInt(1, userId);
				deleteStmt.setString(2, groupName);
				int affectedRows = deleteStmt.executeUpdate();
				if (affectedRows == 0) {
					throw new SQLException("No records available to delete");
				}
				return SUCCESS;
			}
		}
		return ERROR;
	}


	public boolean deleteUserFromGroup(int userId) throws SQLException {
		String deleteUser = "DELETE FROM groups WHERE userId=?;";
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement deleteStmt = connection.prepareStatement(deleteUser);)
    {
			deleteStmt.setInt(1, userId);
			deleteStmt.executeUpdate();
      int affectedRows = deleteStmt.executeUpdate();
      return affectedRows != 0;
		}
	}

	public String updateGroupName(String oldName, String newName) throws SQLException {
		boolean exists = checkGroupExists(oldName);
		if(exists) {
			String updateUser = "UPDATE groups SET groupName=? WHERE groupName=?;";
			try (Connection connection = connectionManager.getConnection();
          PreparedStatement updateStmt = connection.prepareStatement(updateUser);
          ){
				updateStmt.setString(1, newName);
				updateStmt.setString(2, oldName);
				updateStmt.executeUpdate();
				return SUCCESS;
			}
		}
		else {
			return ERROR;
		}
	}


	public List<String> listGroupName(String groupName) throws SQLException {
		List<String> users = new ArrayList<>();
		String selectGroup =
				"SELECT user.username as username from user inner join groups where user.userid = groups.userid and groupName = ?";
		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectGroup);
    ){
			selectStmt.setString(1, groupName);
			results = selectStmt.executeQuery();
			while(results.next()) {
				String user = results.getString("username");
				users.add(user);
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return users;
	}

	public boolean checkGroupExists(String groupName) throws SQLException {
		String selectGroup =
				"SELECT groupName from GROUPS where groupName = ?";

		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectGroup);
    ){
			selectStmt.setString(1, groupName);
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

	public boolean checkUserInGroup(String groupName, int userId) throws SQLException {
		String selectGroup =
				"SELECT * from GROUPS where groupName = ? and userId = ?";
		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectGroup)
    ){
			selectStmt.setString(1, groupName);
			selectStmt.setInt(2, userId);
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

	public String deleteGroup(String groupName) throws SQLException {
		if(checkGroupExists(groupName))
		{
			String deleteUser = "DELETE FROM groups WHERE groupName = ?;";

			try (Connection connection = connectionManager.getConnection();
          PreparedStatement deleteStmt = connection.prepareStatement(deleteUser);
      ){
				deleteStmt.setString(1, groupName);
				int affectedRows = deleteStmt.executeUpdate();
				if (affectedRows == 0) {
					throw new SQLException("No records available to delete");
				}
				return SUCCESS;
			}
		}
		return ERROR;
	}
	
	public int findGroupIdByName(String userName) throws SQLException {
		String selectUser =
				"SELECT groupid from groups where groupname = ? group by groupName;";

		ResultSet results = null;
		try (Connection connection = connectionManager.getConnection();
        PreparedStatement selectStmt = connection.prepareStatement(selectUser);
    ){
			selectStmt.setString(1, userName);
			results = selectStmt.executeQuery();
			if(results.next()) {
				return results.getInt("groupid");
			}
		} finally {
			if(results != null) {
				results.close();
			}
		}
		return 0;
	}


}
