package edu.northeastern.ccs.im.dao;

import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.jdbc.PreparedStatementWrapper;
import com.mysql.cj.jdbc.result.ResultSetImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.northeastern.ccs.im.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for UserDao.
 * Sean Fitzpatrick
 */

class UserDaoTest {
  private UserDao testDao;
  private User testUser;

  @Mock
  private ConnectionManager mockManager;
  @Mock
  private Connection mockConnection;
  @Mock
  private PreparedStatement mockStatement;
  @Mock
  private ResultSet mockResultSet;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    testUser = new User("test", "testPass");
    assertNotNull(mockManager);
    testDao = UserDao.getInstance();
    testDao.connectionManager = mockManager;

    when(mockManager.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(any(String.class), any(Integer.class))).thenReturn(mockStatement);
    when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockStatement);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockStatement.executeQuery()).thenReturn(mockResultSet);
  }

  @Test
  void testCreateUser() throws Exception{
    when(mockResultSet.getInt(1)).thenReturn(54321);
    when(mockResultSet.next()).thenReturn(true);

    User createdUser = testDao.create(testUser);

    assertEquals(54321, createdUser.getUserId());
    assertEquals("test", createdUser.getUserName());
    assertEquals("testPass", createdUser.getPassword());
  }

  @Test
  void testCreateUserFailure() throws Exception{
    when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(false);

    assertThrows(SQLException.class, () -> {
      testDao.create(testUser);
    });
  }

  @Test
  void testCheckUserExists() throws Exception {
    String hash = "$2a$04$jYcAt2x3PoNt80WF5hVA/.hwyt/2TW.1QjZj73T7iGNqpfOFIIW2i";
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("password")).thenReturn(hash);
    assertTrue(testDao.checkUserExists("testUser", "testPass"));
  }


  @Test
  void testCheckUserDoesNotExist() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(testDao.checkUserExists("testUser", "testPass"));
  }

  @Test
  void testCheckUserExistsFailure() throws Exception {
    when(mockStatement.executeQuery()).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.checkUserExists("testUser", "testPass");
    });
  }

  @Test
  void checkUserNameExists() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(testDao.checkUserNameExists("testUser"));
  }

  @Test
  void testCheckUserNameDoesNotExist() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(testDao.checkUserNameExists("testName"));
  }

  @Test
  void testCheckUserNameExistsFailure() throws Exception {
    when(mockConnection.prepareStatement(anyString())).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.checkUserNameExists("testName");
    });
  }

  @Test
  void testFindUserIdByName() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt("userid")).thenReturn(54321);

    assertEquals(54321, testDao.findUserIdByName("testUser"));
  }

  @Test
  void testCannotFindUserId() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertEquals(0, testDao.findUserIdByName("testUser"));
  }

  @Test
  void testFindUserIdByNameFailure() throws Exception {
    when(mockStatement.executeQuery()).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.findUserIdByName("testName");
    });
  }

  @Test
  void testUpdateUserName() throws Exception {
    when(mockResultSet.next()).thenReturn(true);

    assertEquals("success", testDao.updateUserName("testName", "newName"));
  }

  @Test
  void testCannotUpdateUserName() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertEquals("Invalid", testDao.updateUserName("testName", "newName"));
  }

  @Test
  void testUpdateUserNameFailure() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockStatement.executeUpdate()).thenThrow(SQLException.class);

    assertThrows(SQLException.class, () -> {
      testDao.updateUserName("testName", "newName");
    });
  }

  @Test
  void testDelete() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);

    GroupsDao mockGroupsDao = mock(GroupsDao.class);
    when(mockGroupsDao.deleteUserFromGroup("testGroup", "testUser", testDao)).thenReturn("success");

    assertEquals("success", testDao.delete("testName", mockGroupsDao));
  }

  @Test
  void testDeleteFailure() throws Exception{
    when(mockStatement.executeUpdate()).thenReturn(0);

    GroupsDao mockGroupsDao = mock(GroupsDao.class);
    when(mockGroupsDao.deleteUserFromGroup("testGroup", "testUser", testDao)).thenReturn("success");

    assertThrows(SQLException.class, () -> {
      testDao.delete("testUser", mockGroupsDao);
    });
  }

  @Test
  void testFindUserNameById() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("username")).thenReturn("testuser");
    assertEquals("testuser", testDao.findUserNameById(123));
  }
  
  @Test
  void testCannotFindUserName() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertEquals(null, testDao.findUserNameById(123));
  }

  
  @Test
  void testFindUserNAmeById() throws Exception {
    when(mockStatement.executeQuery()).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.findUserNameById(12345);
    
        });
}
  @Test
  void testGetUnreadMessages() throws Exception {
    PreparedStatement userIdMockStatement = mock(PreparedStatement.class);
    ResultSet userIdMockResultSet = mock(ResultSet.class);
    when(mockConnection.prepareStatement("SELECT username from user where userid = ?"))
            .thenReturn(userIdMockStatement);
    when(userIdMockStatement.executeQuery()).thenReturn(userIdMockResultSet);
    when(userIdMockResultSet.next()).thenReturn(true);
    when(userIdMockResultSet.getString("username")).thenReturn("testName");

    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getString("msgText")).thenReturn("test1","test2","test3");
    when(mockResultSet.getInt("senderId")).thenReturn(12345);

    List<String> unreadMessages = new ArrayList<>();
    unreadMessages.add("test1");
    unreadMessages.add("test2");
    unreadMessages.add("test3");

    GroupMessageDao mockGroupMessageDao = mock(GroupMessageDao.class);
    when(mockGroupMessageDao.getUnreadGroupMessages("testName", testDao)).thenReturn(unreadMessages);

    List<String> receivedMessages = testDao.getUnreadMessages("testName", mockGroupMessageDao);
    for (int i = 0; i < 3; i++) {
      assertEquals(unreadMessages.get(i), receivedMessages.get(i));
      assertEquals("testName : " + unreadMessages.get(i) + "\n", receivedMessages.get(i + 3));
    }
  }

  @Test
  void testGetUnreadMessagesEmpty() throws Exception {
    PreparedStatement userIdMockStatement = mock(PreparedStatement.class);
    ResultSet userIdMockResultSet = mock(ResultSet.class);
    when(mockConnection.prepareStatement("SELECT username from user where userid = ?"))
            .thenReturn(userIdMockStatement);
    when(userIdMockStatement.executeQuery()).thenReturn(userIdMockResultSet);
    when(userIdMockResultSet.next()).thenReturn(true);
    when(userIdMockResultSet.getString("username")).thenReturn("testName");

    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getString("msgText")).thenReturn("test1","test2","test3");
    when(mockResultSet.getInt("senderId")).thenReturn(12345);

    List<String> unreadMessages = new ArrayList<>();

    GroupMessageDao mockGroupMessageDao = mock(GroupMessageDao.class);
    when(mockGroupMessageDao.getUnreadGroupMessages("testName", testDao)).thenReturn(unreadMessages);

    List<String> receivedMessages = testDao.getUnreadMessages("testName", mockGroupMessageDao);
    assertEquals(3, receivedMessages.size());
  }

  @Test
  void testFindNameByUserId() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("username")).thenReturn("testName");

    assertEquals("testName", testDao.findUserNameById(12345));
  }

  @Test
  void testFindNameByUserIDFailure() throws Exception {
    when(mockResultSet.next()).thenReturn(false);

    assertEquals(null, testDao.findUserNameById(12345));
  }

  @Test
  void testUpdateReadReceipts() throws Exception {
    GroupMessageDao mockGroupMessageDao = mock(GroupMessageDao.class);
    when(mockGroupMessageDao.updateReadReceipt("testSender", UserDao.getInstance())).thenReturn("success");
    assertEquals("success", testDao.updateReadReceipt("testSender", mockGroupMessageDao));

  }

  @Test
  void testUpdateReadReceiptsFailure() throws Exception {
    GroupMessageDao mockGroupMessageDao = mock(GroupMessageDao.class);
    when(mockGroupMessageDao.updateReadReceipt("testSender", UserDao.getInstance())).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.updateReadReceipt("testSender", mockGroupMessageDao);
    });
  }
}