package edu.northeastern.ccs.im;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.jupiter.api.Test;
import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.server.ServerConstants;

public class ServerConstantsTest {

	@Test
	public void testGetBroadcastResponse_void() {
		List<Message> result = ServerConstants.getBroadcastResponses("potato");
		assertNull(result);
	}

	@Test
	public void testGetBroadcastResponse_Hello() {
		List<Message> result = ServerConstants.getBroadcastResponses("Hello");
		assertEquals(2, result.size());
		assertEquals("BCT 7 Prattle 21 Hello.  How are you?\n", result.get(0).toString());
		assertEquals("BCT 7 Prattle 46 I can communicate with you to test your code.\n", result.get(1).toString());
	}

	@Test
	public void testGetBroadcastResponse_Query() {
		List<Message> result = ServerConstants.getBroadcastResponses("How are you?");
		assertEquals(2, result.size());
		assertEquals("BCT 7 Prattle 28 Why are you asking me this?\n", result.get(0).toString());
		assertEquals("BCT 7 Prattle 32 I am a computer program. I run.\n", result.get(1).toString());
	}

	@Test
	public void testGetBroadcastResponse_Cool() {
		List<Message> result = ServerConstants.getBroadcastResponses("WTF");
		assertEquals(1, result.size());
		assertEquals("BCT 7 Prattle 14 OMG ROFL TTYL\n", result.get(0).toString());

	}

	@Test
	public void testGetBroadcastResponse_Impatient() {
		List<Message> result = ServerConstants.getBroadcastResponses("What time is it Mr. Fox?");
		GregorianCalendar cal = new GregorianCalendar();
		int currHour = cal.get(Calendar.HOUR_OF_DAY);
		int currMinute = cal.get(Calendar.MINUTE);
		String time = currHour + ":" + currMinute;
		String str = "BCT 7 Mr. Fox " + (time.length() + 1) + " " + time;

		assertEquals(2, result.size());
		assertEquals("BCT 3 BBC 16 The time is now\n", result.get(0).toString());
		assertEquals(str + "\n", result.get(1).toString());

	}

	@Test
	public void testGetBroadcastResponse_Date() {
		List<Message> result = ServerConstants.getBroadcastResponses("What is the date?");
		GregorianCalendar cal = new GregorianCalendar();
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);
		int year = cal.get(Calendar.YEAR);

		String dateStr = month + "/" + date + "/" + year;
		assertEquals(1, result.size());
		assertEquals("BCT 4 NIST " + (dateStr.length() + 1) + " " + dateStr + "\n", result.get(0).toString());


	}
	@Test
	public void testGetBroadcastResponse_Time() {
		List<Message> result = ServerConstants.getBroadcastResponses("What time is it?");
		GregorianCalendar cal = new GregorianCalendar();
		int currHour = cal.get(Calendar.HOUR_OF_DAY);
		int currMinute = cal.get(Calendar.MINUTE);
		String time = currHour + ":" + currMinute;

		assertEquals(1, result.size());
		assertEquals("BCT 4 NIST " + (time.length() + 1) + " " + time + "\n", result.get(0).toString());
	}


}