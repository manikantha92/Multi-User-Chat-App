package edu.northeastern.ccs.im.model;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MimeTypeTest {

	private MimeType testMessage;
	 @BeforeEach
	  void setUp() {
	    testMessage = new MimeType(1,"Document.txt", "Some content in doc",2,5);
	    testMessage.setMimeId(0);
	  }
	  @Test
	  void setMsgId() {
	    assertEquals(2, testMessage.getMsgId());
	    testMessage.setMsgId(1);
	    assertEquals(1, testMessage.getMsgId());
	  }

	  @Test
	  void getMsgId() {
	    assertEquals(2, testMessage.getMsgId());
	  }
	  
	  @Test
	  void setContent() {
	    assertEquals("Some content in doc", testMessage.getContent());
	    testMessage.setContent("content");
	    assertEquals("content", testMessage.getContent());
	  }

	  @Test
	  void getFileName() {
		  assertEquals("Document.txt", testMessage.getFileName());
	  }
	  @Test
	  void setFileName() {
	    testMessage.setFileName("test.txt");
	    assertEquals("test.txt", testMessage.getFileName());
	  }
	  
	@Test
	public void getGroupMsgId() {
		  assertEquals(5, testMessage.getGrpMsgId());
	 }
		
	@Test
	public void setGroupMsgId() {
			testMessage.setGrpMsgId(10);
			 assertEquals(10, testMessage.getGrpMsgId());
	}

	  @Test
	  void setMimeId() {
	    assertEquals(0, testMessage.getMimeId());
	    testMessage.setMimeId(1);
	    assertEquals(1, testMessage.getMimeId());
	  }
	  
	  @Test
	  void setMimeId2() {
	    testMessage.setMimeId(2);
	    assertEquals(2, testMessage.getMimeId());
	  }

	  @Test
	  void getMimeId() {
	    assertEquals(0, testMessage.getMimeId());
	  }

}
