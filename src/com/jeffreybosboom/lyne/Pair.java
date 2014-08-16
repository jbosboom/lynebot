package com.jeffreybosboom.lyne;

import java.util.Objects;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/16/2014
 */
public final class Pair<A, B> {
	public final A first;
	public final B second;
	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Pair<?, ?> other = (Pair<?, ?>)obj;
		if (!Objects.equals(this.first, other.first))
			return false;
		if (!Objects.equals(this.second, other.second))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + Objects.hashCode(this.first);
		hash = 67 * hash + Objects.hashCode(this.second);
		return hash;
	}
}
