package de.tudresden.matchtools.datastructures;

import java.util.Arrays;

import de.tudresden.matchtools.weights.Weighter;

public class PreparedString {
	public String[] words;
	public double[] weights;
	public String string;
	public double weightSum;

	public PreparedString(String s, Weighter weighter) {
		super();
		this.string = s;

		this.words = s.split(" ");
		this.weights = new double[words.length];

		for (int j = 0; j < words.length; j++) {
			String w = words[j];
			double weight = weighter.weight(w);
			weights[j] = weight;
			weightSum += weight;
		}
	}

	public String toString() {
		return String.format("PreparedString{%s}[%s]", this.string, Arrays.toString(this.weights));
	}
}
