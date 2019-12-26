package edu.northeastern.ccs.im.model;

import java.sql.Date;

public class User {

	private int userId;
	private String userName;
	private String password;
	private Date loggedInTime;

	public User(String username, String password) {
		this.userName = username;
		this.password = password;
	}

	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getUserName() { return userName; }
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Date getLoggedInTime() { return loggedInTime; }
	public void setLoggedInTime(Date loggedInTime) {
		this.loggedInTime = loggedInTime;
	}



}
