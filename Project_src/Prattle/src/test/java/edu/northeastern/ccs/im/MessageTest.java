package edu.northeastern.ccs.im;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.Message.MessageType;

public class MessageTest {

	private Message testMessage;

	/**
	 * Test makeQuitMessage method.
	 */
	@Test
	void testMakeQuitMessage() {
		testMessage = Message.makeQuitMessage("testName");
		assertEquals("testName", testMessage.getName());
		assertEquals(null, testMessage.getText());
	}

	/**
	 * Test terminate method.
	 */
	@Test
	void testTerminate() {
		testMessage = Message.makeQuitMessage("testName");
		assertEquals("testName", testMessage.getName());
		assertTrue(testMessage.terminate());
	}

	/**
	 * Test makeBroadcastMessage method.
	 */
	@Test
	void testMakeBroadcastMessage() {
		testMessage = Message.makeBroadcastMessage("testName", "test content");
		assertEquals("test content\n", testMessage.getText());
		assertEquals("testName", testMessage.getName());
		assertTrue(testMessage.isBroadcastMessage());
		assertTrue(testMessage.isDisplayMessage());
	}

	/**
	 * Test makeHelloMessage method.
	 */
	@Test
	void testMakeHelloMessage() {
		testMessage = Message.makeHelloMessage("hello test");
		assertEquals("hello test", testMessage.getText());
		assertTrue(testMessage.isInitialization());
	}

	/**
	 * Test makeMakeMessage method.
	 */
	@Test
	void testMakeMessage() {
		testMessage = Message.makeMessage("BYE", "testName", "test content");
		assertTrue(testMessage.terminate());
		assertEquals("testName", testMessage.getName());
		assertEquals(null, testMessage.getText());

		testMessage = Message.makeMessage("HLO", "testName", "test content");
		assertTrue(testMessage.isInitialization());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("ACK", "testName", "test content");
		assertTrue(testMessage.isAcknowledge());
		assertEquals("testName", testMessage.getName());
		assertEquals(null, testMessage.getText());

		testMessage = Message.makeMessage("BCT", "testName", "test content");
		assertTrue(testMessage.isBroadcastMessage());
		assertTrue(testMessage.isDisplayMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content\n", testMessage.getText());

		testMessage = Message.makeMessage("NAK", "testName", "test content");
		assertFalse(testMessage.isDisplayMessage());
		assertFalse(testMessage.terminate());
		assertFalse(testMessage.isInitialization());
		assertFalse(testMessage.isBroadcastMessage());
		assertFalse(testMessage.isAcknowledge());

		assertEquals(null, testMessage.getText());
		assertEquals(null, testMessage.getName());

		testMessage = Message.makeMessage("bad input", "testName", "test content");
		try {
			assertTrue(testMessage.isInitialization());
		} catch (NullPointerException e) {
			assertEquals(null, e.getMessage());
		}
	}

	/**
	 * test makeMessage method for newly added else if statements for CRUD user and group messaging
	 * and user to user message, and group messaging.
	 */
	@Test
	void testMakeMessageNewAdditions() {
		testMessage = Message.makeMessage("UPD", "testName", "test content");
		assertTrue(testMessage.update());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("DEL", "testName", "test content");
		assertTrue(testMessage.delete());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("GRP", "testName", "test content");
		assertTrue(testMessage.createGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("AGR", "testName", "test content");
		assertTrue(testMessage.addGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("DGR", "testName", "test content");
		assertTrue(testMessage.deleteFromGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("UGR", "testName", "test content");
		assertTrue(testMessage.updateGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("LGR", "testName", "test content");
		assertTrue(testMessage.listGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("DGP", "testName", "test content");
		assertTrue(testMessage.deleteGroup());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("UMG", "testName", "test content");
		assertTrue(testMessage.isUserMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("GMG", "testName", "test content");
		assertTrue(testMessage.isGroupMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("LOG", "testName", "test content");
		assertTrue(testMessage.isLoggerMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		
	}


	@Test
	void testMakeMessageRecall() {
		testMessage = Message.makeMessage("RCL", "testName", "test content");
		assertTrue(testMessage.isRecallMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("RCA", "testName", "test content");
		assertTrue(testMessage.isRecallAnyMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("GRL", "testName", "test content");
		assertTrue(testMessage.isRecallGroupMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("RID", "testName", "test content");
		assertTrue(testMessage.isRecallMessageId());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("RGD", "testName", "test content");
		assertTrue(testMessage.isRecallGroupMessageId());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("SSM", "testName", "test content");
		assertTrue(testMessage.isSearchSenderMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("SRM", "testName", "test content");
		assertTrue(testMessage.isSearchReceiverMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());

		testMessage = Message.makeMessage("STM", "testName", "test content");
		assertTrue(testMessage.isSearchTimeMessage());
		assertEquals("testName", testMessage.getName());
		assertEquals("test content", testMessage.getText());
	}

	/**
	 * Test makeNoAcknowledgeMessage method.
	 */
	@Test
	void testMakeNoAcknowledgeMessage() {
		testMessage = Message.makeMessage("NAK", "testName", "test content");
		assertFalse(testMessage.isDisplayMessage());
		assertFalse(testMessage.terminate());
		assertFalse(testMessage.isInitialization());
		assertFalse(testMessage.isBroadcastMessage());
		assertFalse(testMessage.isAcknowledge());

		assertEquals(null, testMessage.getText());
		assertEquals(null, testMessage.getName());
	}

	/**
	 * Test makeAcknowledge method.
	 */
	@Test
	void testMakeAcknowledgeMessage() {
		testMessage = Message.makeMessage("ACK", "testName", "test content");
		assertTrue(testMessage.isAcknowledge());
		assertEquals("testName", testMessage.getName());
		assertEquals(null, testMessage.getText());
	}

	/**
	 * Test makeSimpleLogin method.
	 */
	@Test
	void testMakeSimpleLoginMessage() {
		testMessage = Message.makeSimpleLoginMessage("testName","abcd");
		assertTrue(testMessage.isInitialization());
		assertEquals("testName", testMessage.getName());
		assertEquals("abcd", testMessage.getText());
	}

	/**
	 * Test getName method.
	 */
	@Test
	void testGetName() {
		testMessage = Message.makeMessage("BCT", "testName", "test content");
		assertEquals("testName", testMessage.getName());
	}

	/**
	 * Test getName method with null name input.
	 */
	@Test
	void testGetNameNull() {
		testMessage = Message.makeMessage("BCT", null, "testContent");
		assertEquals(null, testMessage.getName());
	}

	/**
	 * Test getText method.
	 */
	@Test
	void testGetText() {
		testMessage = Message.makeMessage("BCT", "testName", "test content");
		assertEquals("test content\n", testMessage.getText());
	}

	/**
	 * Test getText method with null text input.
	 */
	@Test
	void testGetTextNull() {
		testMessage = Message.makeMessage("BCT", "testName", null);
		assertEquals(null + "\n", testMessage.getText());
	}

	/**
	 * Test isAcknowledge boolean method.
	 */
	@Test
	void testIsAcknowledge() {
		testMessage = Message.makeQuitMessage("testName");
		assertFalse(testMessage.isAcknowledge());

		testMessage = Message.makeAcknowledgeMessage("testName");
		assertTrue(testMessage.isAcknowledge());
	}

	/**
	 * Test isBroadcast boolean method.
	 */
	@Test
	void testIsBroadcastMessage() {
		testMessage = Message.makeQuitMessage("testName");
		assertFalse(testMessage.isBroadcastMessage());

		testMessage = Message.makeBroadcastMessage("testName", "test content");
		assertTrue(testMessage.isBroadcastMessage());

	}

	/**
	 * Test isDisplay boolean method.
	 */
	@Test
	void testIsDisplayMessage() {
		testMessage = Message.makeQuitMessage("testName");
		assertFalse(testMessage.isDisplayMessage());

		testMessage = Message.makeBroadcastMessage("testName", "test content");
		assertTrue(testMessage.isDisplayMessage());
	}

	/**
	 * Test isInitialization boolean method.
	 */
	@Test
	void testIsInitialization() {
		testMessage = Message.makeQuitMessage("testName");

		testMessage = Message.makeHelloMessage("test content");
		assertTrue(testMessage.isInitialization());
	}

	/**
	 * Test toString method.
	 */
	@Test
	void testToString() {
		testMessage = Message.makeMessage("BCT", "testName", "test content");
		assertEquals("BCT 8 testName 13 test content\n", testMessage.toString());
	}

	/**
	 * Test toString method with null string input
	 */
	@Test
	void testToStringNulls() {
		testMessage = Message.makeMessage("BCT", null, null);
		assertEquals("BCT 2 -- 5 null\n", testMessage.toString());
	}

	/**
	 * Test makeUpdateMessage method.
	 */
	@Test
	void testMakeUpdateMessage() {
		testMessage = Message.makeUpdateMessage("Bob", "Robert");
		assertTrue(testMessage.update());
		assertEquals("UPD 3 Bob 6 Robert", testMessage.toString());
	}

	/**
	 * Test makeDeleteMessage method.
	 */
	@Test
	void testMakeDeleteMessage() {
		testMessage = Message.makeDeleteMessage("Bob", "/delete");
		assertTrue(testMessage.delete());
		assertEquals("DEL 3 Bob 7 /delete", testMessage.toString());
	}

	/**
	 * Test makeCreateGroupMessage method.
	 */
	@Test
	void testMakeCreateGroupMessage() {
		testMessage = Message.makeCreateGroupMessage("Bob", "/group Team1");
		assertTrue(testMessage.createGroup());
		assertEquals("GRP 3 Bob 12 /group Team1", testMessage.toString());
		System.out.println(testMessage.toString());
	}

	/**
	 * Test makeAddToGroupMessage method.
	 */
	@Test
	void testMakeAddToGroupMessage() {
		testMessage = Message.makeAddToGroupMessage("Bob", "/gadd Bob-Team1");
		assertTrue(testMessage.addGroup());
		assertEquals("AGR 3 Bob 15 /gadd Bob-Team1", testMessage.toString());
	}

	/**
	 * Test makeDeleteGroupMessage method.
	 */
	@Test
	void testMakeDeleteFromGroupMessage() {
		testMessage = Message.makeDeleteFromGroupMessage("Bob", "/gdelete Bob-Team1");
		assertTrue(testMessage.deleteFromGroup());
		assertEquals("DGR 3 Bob 18 /gdelete Bob-Team1", testMessage.toString());
	}

	/**
	 * Test makeUpdateGroupMessage method.
	 */
	@Test
	void testMakeUpdateGroupMessage() {
		testMessage = Message.makeUpdateGroupMessage("Bob", "/gupdate TeamWin");
		assertTrue(testMessage.updateGroup());
		assertEquals("UGR 3 Bob 16 /gupdate TeamWin", testMessage.toString());
	}

	/**
	 * Test makeListGroupMessage method.
	 */
	@Test
	void testMakeListGroupMessage() {
		testMessage = Message.makeListGroupMessage("Bob", "/glist TeamWin");
		assertTrue(testMessage.listGroup());
		assertEquals("LGR 3 Bob 14 /glist TeamWin", testMessage.toString());
	}

	/**
	 * Test makeDeleteGroupMessage method.
	 */
	@Test
	void testMakeDeleteGroupMessage() {
		testMessage = Message.makeDeleteGroupMessage("Bob", "/grdelete TeamWin");
		assertTrue(testMessage.deleteGroup());
		assertEquals("DGP 3 Bob 17 /grdelete TeamWin", testMessage.toString());
	}

	/**
	 * Test makeUserMessage method.
	 */
	@Test
	void testMakeUserMessage() {
		testMessage = Message.makeUserMessage("Bob", "/message Chelsea-Hey Chels!");
		assertTrue(testMessage.isUserMessage());
		assertEquals("UMG 3 Bob 27 /message Chelsea-Hey Chels!", testMessage.toString());
	}
	

	/**
	 * Test makeGroupMessage method.
	 */
	@Test
	void testMakeGroupMessage() {
		testMessage = Message.makeGroupMessage("Bob", "/msgGroup Team1-Hey team!");
		assertTrue(testMessage.isGroupMessage());
		assertEquals("GMG 3 Bob 25 /msgGroup Team1-Hey team!", testMessage.toString());
	}

	/**
	 * Test getReceiver method
	 */
	@Test
	void testGetReceiver() {
		testMessage = Message.makeUserMessage("Bob", "Chelsea-Hey Chels!");
		String msg = testMessage.getText();
		String receiver = testMessage.getReceiver(msg);
		assertEquals("Chelsea", testMessage.getReceiver(msg));
	}
	
	@Test
	void testMakeRecallMessage() {
		testMessage = Message.makeRecallMessage("Bob", "/recall Chelsea-");
		assertEquals("RCL 3 Bob 16 /recall Chelsea-", testMessage.toString());
		assertTrue(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testMakeRecallAnyMessage() {
		testMessage = Message.makeRecallAnyMessage("Bob", "/allMsg Chelsea-");
		assertEquals("RCA 3 Bob 16 /allMsg Chelsea-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertTrue(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testMakeRecallGroupMessage() {
		testMessage = Message.makeRecallGroupMessage("Bob", "/recallGroup-");
		assertEquals("GRL 3 Bob 13 /recallGroup-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertTrue(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testmakeRecallMessageId() {
		testMessage = Message.makeRecallMessageId("Bob", "/771-");
		assertEquals("RID 3 Bob 5 /771-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertTrue(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testmakeRecallGroupMessageId() {
		testMessage = Message.makeRecallGroupMessageId("Bob", "/1771-");
		assertEquals("RGD 3 Bob 6 /1771-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertTrue(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testMakeSearchSenderMessage() {
		testMessage = Message.makeSearchSenderMessage("Bob", "/1771-");
		assertEquals("SSM 3 Bob 6 /1771-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertTrue(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testMakeSearchReceiverMessage() {
		testMessage = Message.makeSearchReceiverMessage("Bob", "/1771-");
		assertEquals("SRM 3 Bob 6 /1771-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertTrue(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertFalse(testMessage.isSearchTimeMessage());
	}
	
	@Test
	void testMakeSearchTimeMessage() {
		testMessage = Message.makeSearchTimeMessage("Bob", "/1771-");
		assertEquals("STM 3 Bob 6 /1771-", testMessage.toString());
		assertFalse(testMessage.isRecallMessage());
		assertFalse(testMessage.isRecallAnyMessage());
		assertFalse(testMessage.isRecallGroupMessage());
		assertFalse(testMessage.isRecallGroupMessageId());
		assertFalse(testMessage.isRecallMessageId());
		assertFalse(testMessage.isSearchReceiverMessage());
		assertFalse(testMessage.isSearchSenderMessage());
		assertTrue(testMessage.isSearchTimeMessage());
		assertFalse(testMessage.isUserMessage());
		assertFalse(testMessage.isGroupMessage());
	}

	/**
	 * Test isLogger method
	 */
	@Test
	void testIsLoggerMessage() {
		testMessage = Message.makeMessage("LOG", "testName", "test content");
		assertTrue(testMessage.isLoggerMessage());

		testMessage = Message.makeUserMessage("Bob", "Chelsea-Hey Chels!");
		assertFalse(testMessage.isLoggerMessage());
	}

	
	@Test
	void testGetMessageType() {
		testMessage = Message.makeUserMessage("Bob", "Chelsea-Hey Chels!");
		assertEquals("UMG", testMessage.getMessageType());
	}

}