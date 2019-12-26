package edu.northeastern.ccs.im.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.sql.Date;

public class UserTest {

  public User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("testName", "testPass");
  }

  @Test
  void testConstructor() {
    assertEquals("testName", testUser.getUserName());
    assertEquals("testPass", testUser.getPassword());
    assertEquals(0, testUser.getUserId());
    assertEquals(null, testUser.getLoggedInTime());
  }

  @Test
  void testNullConstructor() {
    testUser = new User(null, null);
    assertEquals(null, testUser.getUserName());
    assertEquals(null, testUser.getPassword());
  }

  @Test
  void testUserID() {
    assertEquals(0, testUser.getUserId());
    testUser.setUserId(12345);
    assertEquals(12345, testUser.getUserId());
  }

  @Test
  void testUserName() {
    assertEquals("testName", testUser.getUserName());

    testUser.setUserName("newName");
    assertEquals("newName", testUser.getUserName());
  }

  @Test
  void testPassword() {
    assertEquals("testPass", testUser.getPassword());

    testUser.setPassword("newPass");
    assertEquals("newPass", testUser.getPassword());
  }

  @Test
  void testDate() {
    Date testDate = new Date(0);
    assertEquals(null, testUser.getLoggedInTime());

    testUser.setLoggedInTime(testDate);
    assertEquals(testDate, testUser.getLoggedInTime());
  }

}