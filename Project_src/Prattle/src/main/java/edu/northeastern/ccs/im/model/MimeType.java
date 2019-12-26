package edu.northeastern.ccs.im.model;

public class MimeType {

	public MimeType(int mimeId, String fileName, String content, int msgId, int grpMsgId) {
		super();
		this.mimeId = mimeId;
		this.fileName = fileName;
		this.content = content;
		this.msgId = msgId;
		this.grpMsgId = grpMsgId;
	}
	private int mimeId;
	private String fileName;
	private String content;
	private int msgId;
	private int grpMsgId;
	public int getMimeId() {
		return mimeId;
	}
	public void setMimeId(int mimeId) {
		this.mimeId = mimeId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getMsgId() {
		return msgId;
	}
	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
	public int getGrpMsgId() {
		return grpMsgId;
	}
	public void setGrpMsgId(int grpMsgId) {
		this.grpMsgId = grpMsgId;
	}
	
	
}
