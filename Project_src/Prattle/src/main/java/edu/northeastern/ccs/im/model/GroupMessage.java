package edu.northeastern.ccs.im.model;


import java.sql.Date;

public class GroupMessage {

	public GroupMessage(int senderId, int groupId, String msgText) {
		super();
		this.senderId = senderId;
		this.groupId = groupId;
		this.msgText = msgText;
	}
	private int grpMsgId;
	private int senderId;
	private int groupId;
	private String msgText;
	private Date sentTime;

	public Date getSentTime() {
		return sentTime;
	}
	public void setSentTime(Date sentTime) {
		this.sentTime = sentTime;
	}

	public int getSenderId() {
		return senderId;
	}
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public String getMsgText() {
		return msgText;
	}
	public void setMsgText(String msgText) {
		this.msgText = msgText;
	}
	public int getGrpMsgId() {
		return grpMsgId;
	}
	public void setGrpMsgId(int grpMsgId) {
		this.grpMsgId = grpMsgId;
	}

	
	
}
