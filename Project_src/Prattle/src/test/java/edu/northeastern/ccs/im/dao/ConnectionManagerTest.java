package edu.northeastern.ccs.im.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import edu.northeastern.ccs.im.Message;

class ConnectionManagerTest {
	ConnectionManager c;
	public ConnectionManagerTest() {
		 c = new ConnectionManager();
	}
	@Test
	void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Method connectionManager = c.getClass().getDeclaredMethod("getConnection");
		connectionManager.setAccessible(true);
		Connection connection = (Connection) connectionManager.invoke(c);
		assertNotNull(connection);

		try {
			connection.close();
		} catch (SQLException e){
		}



	}

}
