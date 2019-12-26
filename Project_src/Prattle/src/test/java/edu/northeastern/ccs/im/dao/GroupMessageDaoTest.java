package edu.northeastern.ccs.im.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.ccs.im.model.GroupMessage;
import edu.northeastern.ccs.im.model.GroupReceiverMapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupMessageDaoTest {

	private GroupMessageDao testDao;

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
	@Mock
	private GroupsDao mockGroupsDao;



	@BeforeEach
	void setUp() throws Exception{
		MockitoAnnotations.initMocks(this);
		MockitoAnnotations.initMocks(this);
		assertNotNull(mockManager);
		testDao = GroupMessageDao.getInstance();
		testDao.connectionManager = mockManager;

		when(mockManager.getConnection()).thenReturn(mockConnection);
		when(mockConnection.prepareStatement(any(String.class), any(Integer.class))).thenReturn(mockStatement);
		when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockStatement);
		when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
		when(mockStatement.executeQuery()).thenReturn(mockResultSet);
	}

	@Test
	void createGroupMessage() throws Exception{
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockGroupsDao.checkGroupExists(anyString())).thenReturn(true);
		when(mockUserDao.findUserIdByName(anyString())).thenReturn(1234);
		when(mockGroupsDao.findGroupIdByName(anyString())).thenReturn(4321);
		when(mockResultSet.next()).thenReturn(true);
		PreparedStatement mockMappingStatement = mock(PreparedStatement.class);
		when(mockConnection.prepareStatement("INSERT INTO groupreceivermapping(GroupMsgId,receiverId, isread) VALUES(?,?,?);",
				Statement.RETURN_GENERATED_KEYS)).thenReturn(mockMappingStatement);

		Map<String, Boolean> recipientsAvalable = new HashMap<String, Boolean>();
		recipientsAvalable.put("test1", true);
		recipientsAvalable.put("test2", true);
		recipientsAvalable.put("test3", false);

		when(mockUserDao.findUserIdByName("test1")).thenReturn(111);
		when(mockUserDao.findUserIdByName("test2")).thenReturn(222);
		when(mockUserDao.findUserIdByName("test3")).thenReturn(333);

		when(mockResultSet.getInt(1)).thenReturn(4321);

		GroupMessage resultMessage = testDao.createGroupMessage(recipientsAvalable, "test message",
				"testSender", "testGroup", mockUserDao, mockGroupsDao);

		assertEquals(4321, resultMessage.getGrpMsgId());
		assertEquals(1234, resultMessage.getSenderId());
		assertEquals(4321, resultMessage.getGroupId());
		assertEquals("test message", resultMessage.getMsgText());

	}

	@Test
	void createGroupMessageException() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockGroupsDao.checkGroupExists(anyString())).thenReturn(true);
		when(mockUserDao.findUserIdByName(anyString())).thenReturn(1234);
		when(mockGroupsDao.findGroupIdByName(anyString())).thenReturn(4321);
		when(mockResultSet.next()).thenReturn(false);
		PreparedStatement mockMappingStatement = mock(PreparedStatement.class);
		when(mockConnection.prepareStatement("INSERT INTO groupreceivermapping(GroupMsgId,receiverId, isread) VALUES(?,?,?);",
				Statement.RETURN_GENERATED_KEYS)).thenReturn(mockMappingStatement);

		Map<String, Boolean> recipientsAvalable = new HashMap<String, Boolean>();
		recipientsAvalable.put("test1", true);
		recipientsAvalable.put("test2", true);
		recipientsAvalable.put("test3", false);

		when(mockUserDao.findUserIdByName("test1")).thenReturn(111);
		when(mockUserDao.findUserIdByName("test2")).thenReturn(222);
		when(mockUserDao.findUserIdByName("test3")).thenReturn(333);

		when(mockResultSet.getInt(1)).thenReturn(4321);

		assertThrows(SQLException.class, () -> {
			GroupMessage resultMessage = testDao.createGroupMessage(recipientsAvalable, "test message",
					"testSender", "testGroup", mockUserDao, mockGroupsDao);
		});
	}

	@Test
	void createGroupMessageEmptyGroup() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockGroupsDao.checkGroupExists(anyString())).thenReturn(true);
		when(mockUserDao.findUserIdByName(anyString())).thenReturn(1234);
		when(mockGroupsDao.findGroupIdByName(anyString())).thenReturn(4321);
		when(mockResultSet.next()).thenReturn(true);
		PreparedStatement mockMappingStatement = mock(PreparedStatement.class);
		when(mockConnection.prepareStatement("INSERT INTO groupreceivermapping(GroupMsgId,receiverId, isread) VALUES(?,?,?);",
				Statement.RETURN_GENERATED_KEYS)).thenReturn(mockMappingStatement);

		Map<String, Boolean> recipientsAvalable = new HashMap<String, Boolean>();

		when(mockResultSet.getInt(1)).thenReturn(4321);

		GroupMessage resultMessage = testDao.createGroupMessage(recipientsAvalable, "test message",
				"testSender", "testGroup", mockUserDao, mockGroupsDao);

		assertEquals(4321, resultMessage.getGrpMsgId());
		assertEquals(1234, resultMessage.getSenderId());
		assertEquals(4321, resultMessage.getGroupId());
		assertEquals("test message", resultMessage.getMsgText());

	}

	@Test
	void createGroupMessageGroupDoesNotExist() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockGroupsDao.checkGroupExists(anyString())).thenReturn(false);

		Map<String, Boolean> recipientsAvalable = new HashMap<String, Boolean>();

		assertEquals(null, testDao.createGroupMessage(recipientsAvalable, "test message",
				"testSender", "testGroup", mockUserDao, mockGroupsDao));

	}

	@Test
	void getUnreadGroupMessages() throws Exception {

		when(mockResultSet.getString("msgText")).thenReturn("testText");
		when(mockResultSet.getInt("senderId")).thenReturn(1,2, 3);
		when(mockResultSet.getString("groupname")).thenReturn("testGroup");
		when(mockResultSet.next()).thenReturn(true,true,true,false);
		when(mockUserDao.findUserNameById(anyInt())).thenReturn("testName1", "testName2", "testName3");
		List<String> messages = testDao.getUnreadGroupMessages("testSender", mockUserDao);
		List<String> testNames = new ArrayList<String>();
		testNames.add("testName1");
		testNames.add("testName2");
		testNames.add("testName3");

		assertEquals(3, messages.size());
		for (int i = 0; i < messages.size(); i++) {
			assertEquals(testNames.get(i) + " in group testGroup : testText\n", messages.get(i));
		}
	}

	@Test
	void updateReadReceipt() throws Exception {
		when(mockUserDao.findUserIdByName("testName")).thenReturn(111);
		assertEquals("success", testDao.updateReadReceipt("testName", mockUserDao));
	}

	@Test
	void updateReadReceiptException() throws Exception {
		assertEquals("success", testDao.updateReadReceipt("testName", mockUserDao));
	}

	@Test
	void testCreateRecallGroupMessage() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockGroupsDao.checkGroupExists(anyString())).thenReturn(true);
		when(mockUserDao.findUserIdByName(anyString())).thenReturn(123);
		when(mockGroupsDao.findGroupIdByName(anyString())).thenReturn(321);
		when(mockResultSet.next()).thenReturn(true);
		assertEquals("success", testDao.createRecallGroupMessage("testSender", "testGroup",
				mockUserDao, mockGroupsDao));

		when(mockResultSet.next()).thenReturn(false);
		assertEquals(null, testDao.createRecallGroupMessage("testSender", "testGroup",
				mockUserDao, mockGroupsDao));
	}

	@Test
	void testCreateRecallAnyGroupMessageId() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockResultSet.next()).thenReturn(true);
		assertEquals("Success", testDao.createRecallAnyGroupMessageId("testSender",
				"321", mockUserDao));

	}

	@Test
	void testCreateRecallAnyGroupMessageTest() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getInt("grpMsgId")).thenReturn(123);
		when(mockResultSet.getString("groupName")).thenReturn("testGroup");
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		List<String> messages = testDao.createRecallAnyGroupMessage("testSender", mockUserDao);

		for(String message : messages) {
			assertEquals(message, "1970-01-01 00:00:00.0 : test" + messages.indexOf(message) +
					" has message id 123 received by group testGroup");
		}

		when(mockResultSet.next()).thenReturn(false);
		assertTrue(testDao.createRecallAnyGroupMessage("testSender", mockUserDao).isEmpty());

		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(false);
		assertEquals(null, testDao.createRecallAnyGroupMessage("testSender", mockUserDao));
	}

	@Test
	void testCreateSearchReceiverGroupMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getString("groupname")).thenReturn("testGroup");
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		when(mockResultSet.getString("username")).thenReturn("testName");
		when(mockUserDao.findUserNameById(123)).thenReturn("testName");

		List<String> messages = testDao.createSearchReceiverGroupMessage(123, 321, mockUserDao);

		for(String message : messages) {
			assertEquals("testName at 1970-01-01 00:00:00.0 in group testGroup : test" + messages.indexOf(message) +" received by testName\n", message);
		}
	}

	@Test
	void testcreateSearchTimeGroupMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getString("groupname")).thenReturn("testGroup");
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		when(mockResultSet.getString("username")).thenReturn("testName");
		when(mockUserDao.findUserNameById(123)).thenReturn("testName");

		List<String> messages = testDao.createSearchTimeGroupMessage(123, new Timestamp(0),GroupMessageDao.getInstance(), mockUserDao);

		for(String message : messages) {
			assertEquals("testName at 1970-01-01 00:00:00.0 in group testGroup : test" + messages.indexOf(message) +" received by testName\n", message);
		}
	}
}