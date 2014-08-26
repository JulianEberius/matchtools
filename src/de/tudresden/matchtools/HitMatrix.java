package de.tudresden.matchtools;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import de.tudresden.matchtools.datastructures.MatchingIndices;

public class HitMatrix extends SimMatrix {

	private String[][] haystack;
	private String needle;
	private String[] needles;

	public HitMatrix(DoubleMatrix2D m, String needle, String[][] haystack) {
		super(m);
		this.needle = needle;
		this.haystack = haystack;
		this.M = m;
	}

	public HitMatrix(DoubleMatrix2D m, String[] needles, String[][] haystack) {
		super(m);
		this.needles = needles;
		this.haystack = haystack;
		this.M = m;
	}

	public MatchingIndices getBestHit() {
		double[] maxLocAndValue = this.M.getMaxLocation();
		return new MatchingIndices((int)maxLocAndValue[1], (int)maxLocAndValue[2], maxLocAndValue[0]);
	}

	public void selectMaxDelta(double delta) {
		double[] maxLocAndValue = this.M.getMaxLocation();
        selectThreshold(maxLocAndValue[0]-delta);
	}

	public void selectMaxDelta() {
		selectMaxDelta(0.1);
	}

	public String[][] getHaystack() {
		return haystack;
	}

	public void setHaystack(String[][] haystack) {
		this.haystack = haystack;
	}

	public String getNeedle() {
		return needle;
	}

	public String[] getNeedles() {
		return needles;
	}


	public void setNeedle(String needle) {
		this.needle = needle;
	}

}
