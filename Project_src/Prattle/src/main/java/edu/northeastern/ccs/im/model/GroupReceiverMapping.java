package edu.northeastern.ccs.im.model;

public class GroupReceiverMapping {

	private int groupMsgId;
	private int receiverId;
	private int grpUserMsgId;
	
	public GroupReceiverMapping(int groupMsgId, int receiverId) {
		super();
		this.groupMsgId = groupMsgId;
		this.receiverId = receiverId;
	}
	public int getGroupMsgId() {
		return groupMsgId;
	}
	public void setGroupMsgId(int groupMsgId) {
		this.groupMsgId = groupMsgId;
	}
	public int getReceiverId() {
		return receiverId;
	}
	public void setReceiverId(int receiverId) {
		this.receiverId = receiverId;
	}
	public int getGrpUserMsgId() {
		return grpUserMsgId;
	}
	public void setGrpUserMsgId(int grpUserMsgId) {
		this.grpUserMsgId = grpUserMsgId;
	}
}
