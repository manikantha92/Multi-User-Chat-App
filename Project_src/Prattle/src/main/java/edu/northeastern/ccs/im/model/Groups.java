package edu.northeastern.ccs.im.model;

public class Groups {

	private int groupId;
	private String groupName;
	private int userId;

	public Groups(String groupName,int userId)
	{
		this.groupName = groupName;
		this.userId = userId;
	}

	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
}
