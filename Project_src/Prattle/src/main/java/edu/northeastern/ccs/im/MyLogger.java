package edu.northeastern.ccs.im;
/**
 * Class MyLogger. Creates a static Logger for all classes.
 *
 * @author Justine Lo
 */

import java.util.logging.Logger;
import java.util.logging.Level;

public class MyLogger {
  private static Logger logger;
  private static boolean isOn = false;

  /**
   * Private constructor.
   */
  private MyLogger() {

  }

  /**
   * Sets the boolean variable isOn to true or false to turn on/off printing to console.
   * @param onOff boolean for whether to set isOn to true or false
   */
  public static void setLogOn(Boolean onOff){
    isOn = onOff;
  }

  /**
   * Method to log the error level and message.
   * @param level Level of the error
   * @param msg error message
   */
  public static Logger log(Level level, String msg){
    logger = Logger.getLogger(MyLogger.class.getName());
    logger.setUseParentHandlers(isOn);
    logger.log(level, msg);
    return logger;
  }

  /**
   * Getter for the variable isOn.
   * @return boolean value of the variable isOn
   */
  public static boolean getIsOn() {
    return isOn;
  }
}