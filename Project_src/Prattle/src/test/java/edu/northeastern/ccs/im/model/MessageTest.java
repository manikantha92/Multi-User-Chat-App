package edu.northeastern.ccs.im.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

  private Message testMessage;

  @BeforeEach
  void setUp() {
    testMessage = new Message(123, 1,"test text");
    testMessage.setMsgId(0);
  }
  @Test
  void setMsgId() {
    assertEquals(0, testMessage.getMsgId());
    testMessage.setMsgId(1);
    assertEquals(1, testMessage.getMsgId());
  }

  @Test
  void getMsgId() {
    assertEquals(0, testMessage.getMsgId());
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
  void getReceiverId() {
    assertEquals(1, testMessage.getReceiverId());
  }

  @Test
  void setReceiverId() {
    testMessage.setReceiverId(1);
    assertEquals(1, testMessage.getReceiverId());
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

}