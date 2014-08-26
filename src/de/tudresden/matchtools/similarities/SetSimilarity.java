package de.tudresden.matchtools.similarities;

import java.util.Set;

public interface SetSimilarity<T> {
	public abstract double similarity(Set<T> a, Set<T> b);
}
