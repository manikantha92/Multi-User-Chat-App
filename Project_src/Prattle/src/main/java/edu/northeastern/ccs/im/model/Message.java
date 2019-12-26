package edu.northeastern.ccs.im.model;

import java.sql.Date;

public class Message {

	public Message(int senderId, int receiverId, String msgText) {
		super();
		this.senderId = senderId;
		this.receiverId = receiverId;
		this.msgText = msgText;
	}
	private int msgId;
	private int senderId;
	private int receiverId;
	private String msgText;
	private Date sentTime;

	public int getMsgId() {
		return msgId;
	}
	public Date getSentTime() {
		return sentTime;
	}
	public void setSentTime(Date sentTime) {
		this.sentTime = sentTime;
	}
	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
	public int getSenderId() {
		return senderId;
	}
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
	public int getReceiverId() {
		return receiverId;
	}
	public void setReceiverId(int receiverId) {
		this.receiverId = receiverId;
	}
	public String getMsgText() {
		return msgText;
	}
	public void setMsgText(String msgText) {
		this.msgText = msgText;
	}
}
