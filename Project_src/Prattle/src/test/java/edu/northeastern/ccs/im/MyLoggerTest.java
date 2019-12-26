package edu.northeastern.ccs.im;
/**
 * JUnit Tests for class MyLogger.
 *
 * @author Justine Lo
 */

import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MyLoggerTest {

  /**
   * Tests method setLogOn.
   */
  @Test
  public void testSetLogOn() {
    MyLogger.setLogOn(true);
    assertTrue(MyLogger.getIsOn());

    MyLogger.setLogOn(false);
    assertFalse(MyLogger.getIsOn());
  }

  /**
   * Test log method.
   */
  @Test
  public void testLog() {
    Logger testLog = MyLogger.log(Level.SEVERE, "testing");
    assertEquals(800, testLog.getParent().getLevel().intValue());
    assertEquals("edu.northeastern.ccs.im.MyLogger", testLog.getName());
  }
}