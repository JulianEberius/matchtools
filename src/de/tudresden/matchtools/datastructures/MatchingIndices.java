package de.tudresden.matchtools.datastructures;

public class MatchingIndices implements Comparable<MatchingIndices> {
	public int a;
	public int b;
	public double c;

	public MatchingIndices(int i, int j, double d) {
		this.a = i;
		this.b = j;
		this.c = d;
	}

	@Override
	public int compareTo(MatchingIndices o) {
		return Double.compare(this.c, o.c);
	}
}
