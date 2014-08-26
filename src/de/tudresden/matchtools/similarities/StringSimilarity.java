package de.tudresden.matchtools.similarities;

import de.tudresden.matchtools.datastructures.PreparedString;

public interface StringSimilarity {
	public abstract double similarity(String a, String b);
    public abstract double similarity(PreparedString a, PreparedString b);
}


