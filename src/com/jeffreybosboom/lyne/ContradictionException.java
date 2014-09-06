package com.jeffreybosboom.lyne;

/**
 * Thrown when a Puzzle operation would result in an unsolvable puzzle.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/16/2014
 */
public final class ContradictionException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ContradictionException() {}
	@Override
	public synchronized Throwable fillInStackTrace() {
		//for performance, don't bother with stack traces
		return this;
	}
}
