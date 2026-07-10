package org.apache.logging.log4j; public interface Logger { void info(String s); void info(String s,Object o); void warn(String s); void error(String s,Throwable t); }
