package com.thread.dump.parser.domain;

/**
 * @author Leo Gutiérrez
 */
public enum StackTraceLock {
	LOCKED,
	PARKING_TO_WAIT_FOR,
	WAITING_ON,
	WAITING_TO_LOCK
}
