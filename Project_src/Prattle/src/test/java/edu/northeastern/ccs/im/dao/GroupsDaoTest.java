package edu.northeastern.ccs.im.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.northeastern.ccs.im.model.Groups;
import edu.northeastern.ccs.im.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupsDaoTest {

  private GroupsDao testDao;
  private User testUser;

  @Mock
  private ConnectionManager mockManager;
  @Mock
  private Connection mockConnection;
  @Mock
  private PreparedStatement mockStatement;
  @Mock
  private ResultSet mockResultSet;

  @Mock
  private UserDao mockUserDao;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    testUser = new User("test", "testPass");
    assertNotNull(mockManager);
    testDao = GroupsDao.getInstance();
    testDao.connectionManager = mockManager;

    when(mockManager.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(any(String.class), any(Integer.class))).thenReturn(mockStatement);
    when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockStatement);
    when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
    when(mockUserDao.findUserIdByName(anyString())).thenReturn(12345);
  }

  @Test
  void createGroup() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(12345);
    ResultSet mockCheckMethodResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckMethodResultSet);
    when(mockCheckMethodResultSet.next()).thenReturn(false);

    Groups testGroup = testDao.createGroup("testGroup", "testUser", mockUserDao);
    assertEquals(12345, testGroup.getUserId());
    assertEquals("testGroup", testGroup.getGroupName());
  }

  @Test
  void testCreateGroupFailure() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    when(mockResultSet.getInt(1)).thenReturn(12345);
    ResultSet mockCheckMethodResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckMethodResultSet);
    when(mockCheckMethodResultSet.next()).thenReturn(false);

    assertThrows(SQLException.class, () -> {
      Groups testGroup = testDao.createGroup("testGroup", "testUser", mockUserDao);
    });
  }

  @Test
  void testCreateGroupAlreadyExists() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(12345);
    ResultSet mockCheckMethodResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckMethodResultSet);
    when(mockCheckMethodResultSet.next()).thenReturn(true);

    assertEquals(null, testDao.createGroup("testGroup", "testUser", mockUserDao));
  }

  @Test
  void testAddtoGroup() throws Exception {
    // set returns for main check in addToGroup method
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(12345);

    // set of mocks for checkUserInGroup() call
    PreparedStatement mockCheckUserInGroupStatement = mock(PreparedStatement.class);
    ResultSet mockCheckUserInGroupResultSet = mock(ResultSet.class);
    when(mockConnection.prepareStatement("SELECT * from GROUPS where groupName = ? and userId = ?"))
            .thenReturn(mockCheckUserInGroupStatement);
    when(mockCheckUserInGroupStatement.executeQuery()).thenReturn(mockCheckUserInGroupResultSet);
    when(mockCheckUserInGroupResultSet.next()).thenReturn(false);

    // set of mocks for checkGroupExists() call
    ResultSet mockCheckGroupExistsResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckGroupExistsResultSet);
    when(mockCheckGroupExistsResultSet.next()).thenReturn(true);

    Groups testGroup = testDao.addtoGroup("testGroup", "testUser", mockUserDao);
    assertEquals(12345, testGroup.getUserId());
    assertEquals("testGroup", testGroup.getGroupName());

  }

  @Test
  void testAddToGroupDoesNotExist() throws Exception {
    // set of mocks for checkGroupExists() call
    ResultSet mockCheckGroupExistsResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckGroupExistsResultSet);
    when(mockCheckGroupExistsResultSet.next()).thenReturn(true);

    Groups testGroup = testDao.addtoGroup("testGroup", "testUser", mockUserDao);
    assertEquals(null, testGroup);
  }

  @Test
  void testAddToGroupFaulure() throws Exception {
    // set returns for main check in addToGroup method
    when(mockResultSet.next()).thenReturn(false);

    // set of mocks for checkUserInGroup() call
    PreparedStatement mockCheckUserInGroupStatement = mock(PreparedStatement.class);
    ResultSet mockCheckUserInGroupResultSet = mock(ResultSet.class);
    when(mockConnection.prepareStatement("SELECT * from GROUPS where groupName = ? and userId = ?"))
            .thenReturn(mockCheckUserInGroupStatement);
    when(mockCheckUserInGroupStatement.executeQuery()).thenReturn(mockCheckUserInGroupResultSet);
    when(mockCheckUserInGroupResultSet.next()).thenReturn(false);

    // set of mocks for checkGroupExists() call
    ResultSet mockCheckGroupExistsResultSet = mock(ResultSet.class);
    when(mockStatement.executeQuery()).thenReturn(mockCheckGroupExistsResultSet);
    when(mockCheckGroupExistsResultSet.next()).thenReturn(true);

    assertThrows(SQLException.class, () -> {
      testDao.addtoGroup("testGroup", "testUser", mockUserDao);
    });

  }

  @Test
  void deleteUserFromGroupByName() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockResultSet.next()).thenReturn(true);
    assertEquals("success", testDao.deleteUserFromGroup("testName", "testGroup", mockUserDao));
  }

  @Test
  void deleteNonExistentUserFromGroupByName() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockResultSet.next()).thenReturn(false);
    assertEquals("error", testDao.deleteUserFromGroup("testName", "testGroup", mockUserDao));
  }

  @Test
  void deleteUserFromGroupByNameFailure() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(0);
    when(mockResultSet.next()).thenReturn(true);

    assertThrows(SQLException.class, () -> {
      testDao.deleteUserFromGroup("testGroup", "testName", mockUserDao);
    });
  }

  @Test
  void deleteUserFromGroupById() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);

    assertTrue(testDao.deleteUserFromGroup(12345));
  }

  @Test
  void deleteUserFromGroupByIdFailure() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(0);

    assertFalse(testDao.deleteUserFromGroup(12345));
  }

  @Test
  void updateGroupName() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    assertEquals("success", testDao.updateGroupName("testGroup", "newName"));
  }

  @Test
  void updateGroupNameDoesNotExist() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertEquals("error", testDao.updateGroupName("testGroup", "newName"));
  }

  @Test
  void updateGroupNameFailure() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockStatement.executeUpdate()).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.updateGroupName("testGroup", "newName");
    });
  }

  @Test
  void listGroupName() throws Exception {
    List<String> userNames = new ArrayList<>();
    userNames.add("user1");
    userNames.add("user2");
    userNames.add("user3");
    when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(mockResultSet.getString("username")).thenReturn("user1").thenReturn("user2").thenReturn("user3");

    List<String> testNames = testDao.listGroupName("testGroup");
    for(int i = 0; i < testNames.size(); i++) {
      assertEquals(userNames.get(i), testNames.get(i));
    }
  }

  @Test
  void listEmptyGroupName() throws Exception {
    when(mockResultSet.next()).thenReturn(false);

    List<String> testNames = testDao.listGroupName("testGroup");
    assertEquals(0, testNames.size());
  }

  @Test
  void listGroupNameFailure() throws Exception {
    when(mockStatement.executeQuery()).thenThrow(SQLException.class);
    assertThrows(SQLException.class, () -> {
      testDao.listGroupName("testGroup");
    });
  }

  @Test
  void checkGroupExists() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(testDao.checkGroupExists("testGroup"));
  }

  @Test
  void testCheckGroupDoesNotExist() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(testDao.checkGroupExists("testGroup"));
  }

  @Test
  void checkUserInGroup() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(testDao.checkUserInGroup("testGroup", 12345));
  }

  @Test
  void checkUserNotInGroup() throws Exception {
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(testDao.checkUserInGroup("testGroup", 12345));
  }

  @Test
  void deleteGroup() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(1);
    when(mockResultSet.next()).thenReturn(true);

    assertEquals("success", testDao.deleteGroup("testGroup"));
  }

  @Test
  void deleteGroupDoesNotExist() throws Exception {
    when(mockResultSet.next()).thenReturn(false);

    assertEquals("error", testDao.deleteGroup("testGroup"));
  }

  @Test
  void testDeleteGroupFailure() throws Exception {
    when(mockStatement.executeUpdate()).thenReturn(0);
    when(mockResultSet.next()).thenReturn(true);

    assertThrows(SQLException.class, () -> {
      testDao.deleteGroup("testGroup");
    });
  }

  @Test
  void testFindGroupIDByName() throws Exception {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt("groupid")).thenReturn(123);

    assertEquals(123, testDao.findGroupIdByName("testGroup"));
  }

  @Test
  void testFindGroupIDByNameFailure() throws Exception {
    when(mockResultSet.next()).thenReturn(false);

    assertEquals(0, testDao.findGroupIdByName("testGroup"));
  }
}