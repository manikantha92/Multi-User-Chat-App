package edu.northeastern.ccs.im.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupsTest {
  private Groups testGroup;

  @BeforeEach
  void setUp() {
    testGroup = new Groups("testGroup", 0);
  }

  @Test
  void getGroupId() {
    assertEquals(0, testGroup.getGroupId());
  }

  @Test
  void setGroupId() {
    testGroup.setGroupId(12345);
    assertEquals(12345, testGroup.getGroupId());
  }

  @Test
  void getGroupName() {
    assertEquals("testGroup", testGroup.getGroupName());
  }

  @Test
  void setGroupName() {
    testGroup.setGroupName("newName");
    assertEquals("newName", testGroup.getGroupName());
  }

  @Test
  void getUserId() {
    assertEquals(0, testGroup.getUserId());
  }

  @Test
  void setUserId() {
    testGroup.setUserId(12345);
    assertEquals(12345, testGroup.getUserId());
  }
}