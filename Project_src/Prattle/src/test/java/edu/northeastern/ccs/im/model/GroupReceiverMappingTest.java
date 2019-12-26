package edu.northeastern.ccs.im.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupReceiverMappingTest {
	  private GroupReceiverMapping testMessage;

 @BeforeEach
void setUp() {
	    testMessage = new GroupReceiverMapping(123, 1);
	  }
	  
	@Test
	public void getGroupMsgId() {
		  assertEquals(123, testMessage.getGroupMsgId());
	  }
	
	@Test
	public void setGroupMsgId() {
			testMessage.setGroupMsgId(10);
			 assertEquals(10, testMessage.getGroupMsgId());
	}
	@Test
	public void getReceiverId() {
		assertEquals(1, testMessage.getReceiverId());
	}

	@Test
	public void setReceiverId() {
			testMessage.setReceiverId(5);
			 assertEquals(5, testMessage.getReceiverId());
	}
	
	@Test
	public void getGrpUserMsgId() {
		assertEquals(0, testMessage.getGrpUserMsgId());
	}

	@Test
	public void setGrpUserMsgId() {
			testMessage.setGrpUserMsgId(5);
			 assertEquals(5, testMessage.getGrpUserMsgId());
	}
	


}
