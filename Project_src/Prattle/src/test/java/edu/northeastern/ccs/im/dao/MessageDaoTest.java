package edu.northeastern.ccs.im.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Null;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import edu.northeastern.ccs.im.model.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageDaoTest {
	private MessageDao testDao;

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
	private GroupMessageDao mockGroupDao;

	@BeforeEach
	void setUp() throws Exception{
		MockitoAnnotations.initMocks(this);
		assertNotNull(mockManager);
		testDao = MessageDao.getInstance();
		testDao.connectionManager = mockManager;

		when(mockManager.getConnection()).thenReturn(mockConnection);
		when(mockConnection.prepareStatement(any(String.class), any(Integer.class))).thenReturn(mockStatement);
		when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockStatement);
		when(mockStatement.getGeneratedKeys()).thenReturn(mockResultSet);
		when(mockStatement.executeQuery()).thenReturn(mockResultSet);

		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
	}

	@Test
	void createMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getInt(1)).thenReturn(123);
		when(mockUserDao.findUserIdByName("testReceiver")).thenReturn(12345);
		when(mockUserDao.findUserIdByName("testSender")).thenReturn(54321);
		Message resultMessage = testDao.createMessage("testReceiver",
				"test message", "testSender", true, mockUserDao,"127.0.0.1","127.0.0.1");

		assertEquals(54321, resultMessage.getSenderId());
		assertEquals(12345, resultMessage.getReceiverId());
		assertEquals(123, resultMessage.getMsgId());
		assertEquals("test message", resultMessage.getMsgText());
	}

	@Test
	void createMessageUsersDontExist() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(false);
		when(mockResultSet.getInt(1)).thenReturn(123);
		when(mockUserDao.findUserIdByName("testReceiver")).thenReturn(12345);
		when(mockUserDao.findUserIdByName("testSender")).thenReturn(54321);
		Message resultMessage = testDao.createMessage("testReceiver",
				"test message", "testSender", true, mockUserDao,"127.0.0.1","127.0.0.1");

		assertEquals(null, resultMessage);
	}

	@Test
	void createRecallMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getInt(1)).thenReturn(123);
		when(mockUserDao.findUserIdByName("testReceiver")).thenReturn(12345);
		when(mockUserDao.findUserIdByName("testSender")).thenReturn(54321);
		String resultMessage = testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao);

		assertEquals("success", testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao));

	}

	@Test
	void createRecallMessageReceiverNull() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getInt(1)).thenReturn(123);
		when(mockUserDao.checkUserNameExists("testReceiver")).thenReturn(true);
		when(mockUserDao.checkUserNameExists("testSender")).thenReturn(false);
		String resultMessage = testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao);

		assertEquals(null, testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao));

	}

	@Test
	void createRecallMessageSenderNull() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getInt(1)).thenReturn(123);
		when(mockUserDao.checkUserNameExists("testReceiver")).thenReturn(false);
		when(mockUserDao.checkUserNameExists("testSender")).thenReturn(true);
		String resultMessage = testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao);

		assertEquals(null, testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao));

	}

	@Test
	void createRecallMessageFailure() throws Exception {
		when(testDao.createRecallMessage("testReceiver",
				"testSender", testDao, mockUserDao)).thenThrow(SQLException.class);
		assertThrows(SQLException.class, () -> {
			testDao.createRecallMessage("testReceiver",
					"testSender", testDao, mockUserDao);
		});
	}

	@Test
	void checkMessageExists() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		assertTrue(testDao.checkMessageExists(12345));
	}

	@Test
	void testcheckMessageDoesNotExist() throws Exception {
		when(mockResultSet.next()).thenReturn(false);
		assertEquals(false,testDao.checkMessageExists(12345));
	}

	@Test
	void testcheckMessageExistsFailure() throws Exception {
		assertEquals(false,testDao.checkMessageExists(12345));
	}

	@Test
	void testupdateRecvIp() throws Exception {
		when(mockResultSet.next()).thenReturn(true);

		assertEquals("success", testDao.updateRecvIp("testName", mockUserDao, "newName"));
	}


	@Test	
	void testupdateRecvIpFailure() throws Exception {
		when(mockResultSet.next()).thenReturn(true);
		when(mockStatement.executeUpdate()).thenThrow(SQLException.class);

		assertThrows(SQLException.class, () -> {
			testDao.updateRecvIp("testName", mockUserDao, "newName");
		});
	}

	@Test
	void testCreateRecallAnyMessageId() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockResultSet.next()).thenReturn(true);
		assertEquals("Success", testDao.createRecallAnyMessageId("testSender",
				"321", testDao,mockUserDao));

	}

	@Test
	void testCreateRecallAnyMessageId2() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(false);
		when(mockResultSet.next()).thenReturn(false);
		assertEquals(null, testDao.createRecallAnyMessageId("testSender",
				"321", testDao,mockUserDao));

	}

	@Test
	void testCreateRecallAnyMessageTest() throws Exception {
		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(true);
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getInt("msgId")).thenReturn(123);
		when(mockResultSet.getString("username")).thenReturn("receivername");
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		List<String> messages = testDao.createRecallAnyMessage("testSender", testDao, GroupMessageDao.getInstance(),mockUserDao);

		for(String message : messages) {
			assertEquals(message, "1970-01-01 00:00:00.0 : test" + messages.indexOf(message) +
					" has message id 123 received by user receivername");
		}

		when(mockResultSet.next()).thenReturn(false);
		assertTrue(testDao.createRecallAnyMessage("testSender", testDao, GroupMessageDao.getInstance(), mockUserDao).isEmpty());

		when(mockUserDao.checkUserNameExists(anyString())).thenReturn(false);
		List<String> msgList = new ArrayList<>();
		assertEquals(msgList, testDao.createRecallAnyMessage("testSender",  testDao, GroupMessageDao.getInstance(), mockUserDao));
	}

	@Test
	void testCreateSearchReceiverMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getInt("msgId")).thenReturn(234);
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		when(mockUserDao.findUserIdByName("sender")).thenReturn(123);
		when(mockUserDao.findUserIdByName("receiver")).thenReturn(456);

		List<String> messages = testDao.createReceiverSearchMessage("receiver", "sender", testDao, GroupMessageDao.getInstance(), mockUserDao);

		for(String message : messages) {
			assertEquals("1970-01-01 00:00:00.0 : test" + messages.indexOf(message) +" has message id 234 received by user receiver", message);

		}
	}

	@Test
	void testcreateTimeSearchMessage() throws Exception {
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("msgText")).thenReturn("test0", "test1");
		when(mockResultSet.getInt("msgId")).thenReturn(123);
		when(mockResultSet.getInt("receiverId")).thenReturn(456);
		when(mockResultSet.getInt("senderId")).thenReturn(789);
		when(mockResultSet.getTimestamp("sentTime")).thenReturn(new Timestamp(0));
		when(mockResultSet.getString("username")).thenReturn("testName");
		when(mockUserDao.findUserNameById(456)).thenReturn("receiver");
		when(mockUserDao.findUserNameById(789)).thenReturn("sender");

		List<String> messages = testDao.createTimeSearchMessage("1969-12-31", "sender",testDao,GroupMessageDao.getInstance(), mockUserDao);

		for(String message : messages) {
			assertEquals("1970-01-01 00:00:00.0 : test" + messages.indexOf(message) +" sent by sender and received by user receiver", message);
		}
	}

}