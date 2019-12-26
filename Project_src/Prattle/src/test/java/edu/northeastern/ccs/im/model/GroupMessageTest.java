package edu.northeastern.ccs.im.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class GroupMessageTest {

  private GroupMessage testMessage;

  @BeforeEach
  void setUp() {
    testMessage = new GroupMessage(123, 1,"test text");
    testMessage.setGrpMsgId(0);
  }
  @Test
  void setMsgId() {
    assertEquals(0, testMessage.getGrpMsgId());
    testMessage.setGrpMsgId(1);
    assertEquals(1, testMessage.getGrpMsgId());
  }

  @Test
  void getMsgId() {
    assertEquals(0, testMessage.getGrpMsgId());
  }

  @Test
  void getSentTime() {
    Date testDate = new Date(0);
    testMessage.setSentTime(testDate);
    assertEquals(testDate, testMessage.getSentTime());
  }

  @Test
  void getSenderId() {
    assertEquals(123, testMessage.getSenderId());
  }

  @Test
  void setSenderId() {
    testMessage.setSenderId(4321); 
    assertEquals(4321, testMessage.getSenderId());
  }

  @Test
  void getMsgText() {
    assertEquals("test text", testMessage.getMsgText());
  }

  @Test
  void setMsgText() {
    testMessage.setMsgText("");
    assertEquals("", testMessage.getMsgText());
  }
  
  @Test
  void getGroupId() {
    assertEquals(1, testMessage.getGroupId());
  }

  @Test
  void setGroupId() {
	  testMessage.setGroupId(12345);
    assertEquals(12345, testMessage.getGroupId());
  }


}