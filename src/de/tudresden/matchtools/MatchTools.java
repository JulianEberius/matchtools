package de.tudresden.matchtools;

import java.util.ArrayList;
import java.util.List;

import cern.colt.function.tdouble.IntIntDoubleFunction;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import de.tudresden.matchtools.datastructures.Match;
import de.tudresden.matchtools.datastructures.PreparedString;
import de.tudresden.matchtools.similarities.StringSimilarities;
import de.tudresden.matchtools.similarities.StringSimilarity;
import de.tudresden.matchtools.weights.Weighter;

public class MatchTools {

	public static MatchTools DEFAULT = new MatchTools(new Weighter());

	protected Weighter weighter;
	public StringSimilarity levenshtein;
	public StringSimilarity byWordLevenshtein;
	public StringSimilarity weightedByWordLevenshtein;
	public StringSimilarity ngram;
	public StringSimilarity jaro;
	public StringSimilarity aFocusedByWordLevenshtein;
	public StringSimilarity weightedByWordAndPositionLevenshtein;
	public StringSimilarity fastWeightedByWordLevenshtein;

	public MatchTools(Weighter weighter) {
		this.weighter = weighter;
		this.levenshtein = new StringSimilarities.Levenshtein();
		this.byWordLevenshtein = new StringSimilarities.ByWordLevenshtein();
		this.weightedByWordLevenshtein = new StringSimilarities.WeightedByWordLevenshtein(
				weighter);
		this.ngram = new StringSimilarities.NGram();
		this.jaro = new StringSimilarities.Levenshtein();
		this.aFocusedByWordLevenshtein = new StringSimilarities.AFocusedByWordLevenshtein();
		this.weightedByWordAndPositionLevenshtein = new StringSimilarities.WeightedByWordAndPositionLevenshtein(
				weighter);
	}

	public SimMatrix doMatch(final PreparedString[] a,
			final PreparedString[] b, List<StringSimilarity> matcherList,
			AggregationStrategy aggStrategy) {
		List<SimMatrix> matrices = new ArrayList<>(matcherList.size());
		for (final StringSimilarity sd : matcherList) {
			final DoubleMatrix2D m = DoubleFactory2D.dense.make(a.length,
					b.length, 1.0);
			m.forEachNonZero(new IntIntDoubleFunction() {
				@Override
				public double apply(int x, int y, double v) {
					return sd.similarity(a[x], b[y]);
				}
			});
			matrices.add(new SimMatrix(m, a, b));
		}

		SimMatrix m = matrices.get(0);

		switch (aggStrategy) {
		case AGG_AVG:
		default:
			for (SimMatrix otherM : matrices.subList(1, matrices.size())) {
				m.aggregateAvg(otherM);
			}
			break;
		}
		return m;
	}

	public SimMatrix doMatch(final String[] a, final String[] b,
			List<StringSimilarity> matcherList, AggregationStrategy aggStrategy) {
		List<SimMatrix> matrices = new ArrayList<>(matcherList.size());
		for (final StringSimilarity sd : matcherList) {
			final DoubleMatrix2D m = DoubleFactory2D.dense.make(a.length,
					b.length, 1.0);
			m.forEachNonZero(new IntIntDoubleFunction() {
				@Override
				public double apply(int x, int y, double v) {
					return sd.similarity(a[x], b[y]);
				}
			});
			matrices.add(new SimMatrix(m, a, b));
		}
		SimMatrix m = matrices.get(0);
		switch (aggStrategy) {
		case AGG_AVG:
		default:
			for (SimMatrix otherM : matrices.subList(1, matrices.size())) {
				m.aggregateAvg(otherM);
			}
			break;
		}
		return m;
	}

	public SimMatrix doMatch(String[] a, String[] b) {
		List<StringSimilarity> lst = new ArrayList<>();
		lst.add(weightedByWordLevenshtein);
		lst.add(ngram);
		return doMatch(a, b, lst, AggregationStrategy.AGG_AVG);
	}

	public HitMatrix locate(final String needle, final String[][] haystack) {
		int m = haystack.length;
		int n = haystack[0].length;
		final StringSimilarity sd = this.weightedByWordLevenshtein;

		final DoubleMatrix2D mat = DoubleFactory2D.dense.make(m, n, 1.0);
		mat.forEachNonZero(new IntIntDoubleFunction() {
			@Override
			public double apply(int x, int y, double v) {
				return sd.similarity(needle, haystack[x][y]);
			}
		});
		return new HitMatrix(mat, needle, haystack);
	}

	public HitMatrix locate(final String[] needles, final String[][] haystack) {
		int m = haystack.length;
		int n = haystack[0].length;
		final StringSimilarity sd = this.weightedByWordLevenshtein;

		final DoubleMatrix2D mat = DoubleFactory2D.dense.make(m, n, 1.0);
		mat.forEachNonZero(new IntIntDoubleFunction() {
			@Override
			public double apply(int x, int y, double v) {
				double maxSim = 0.0;
				for (int i = 0; i < needles.length; i++) {
					double needleSim = sd
							.similarity(needles[i], haystack[x][y]);
					if (needleSim > maxSim)
						maxSim = needleSim;
				}
				return maxSim;
			}
		});
		return new HitMatrix(mat, needles, haystack);
	}

	public SimMatrix doMatch(String[] a, String[] b,
			List<StringSimilarity> matcherList) {
		return doMatch(a, b, matcherList, AggregationStrategy.AGG_AVG);
	}

	public SimMatrix doMatch(PreparedString[] a, PreparedString[] b,
			List<StringSimilarity> matcherList) {
		return doMatch(a, b, matcherList, AggregationStrategy.AGG_AVG);
	}

	public List<Match> defaultMapping(String[] a, String[] b) {
		SimMatrix m = doMatch(a, b);
		m.selectThreshold(0.13);
		m.selectBipartiteGreedy();
		return m.getMapping();
	}

	public void cleanStringArray(String[] S) {
		for (int i = 0; i < S.length; i++) {
			String s = S[i];
			if (s == null)
				S[i] = "";
			else
				S[i] = s.toLowerCase().trim();
		}

	}
}
