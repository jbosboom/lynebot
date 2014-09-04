package com.jeffreybosboom.lyne.rules;

import com.jeffreybosboom.lyne.Puzzle;
import java.util.function.Function;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public interface InferenceRule extends Function<Puzzle, Puzzle> {
	@Override
	public Puzzle apply(Puzzle puzzle);
}
