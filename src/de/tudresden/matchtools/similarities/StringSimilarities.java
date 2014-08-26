package de.tudresden.matchtools.similarities;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;

import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.NGramDistance;

import de.tudresden.matchtools.datastructures.PreparedString;
import de.tudresden.matchtools.weights.Weighter;

public abstract class StringSimilarities {
	public static class Levenshtein implements StringSimilarity {
		private LevensteinDistance ld = new LevensteinDistance();
		@Override
		public double similarity(String a, String b) {
			return ld.getDistance(a, b);
		}
		public double similarity(PreparedString a, PreparedString b) {
			return similarity(a.string, b.string);
		}
	}

	public static class NGram implements StringSimilarity {
		private NGramDistance ld = new NGramDistance();
		@Override
		public double similarity(String a, String b) {
			return ld.getDistance(a, b);
		}
		public double similarity(PreparedString a, PreparedString b) {
			return similarity(a.string, b.string);
		}
	}

	public static class Jaro implements StringSimilarity {
		private JaroWinklerDistance ld = new JaroWinklerDistance();
		@Override
		public double similarity(String a, String b) {
			return ld.getDistance(a, b);
		}
		public double similarity(PreparedString a, PreparedString b) {
			return similarity(a.string, b.string);
		}
	}

	public static class ByWordLevenshtein implements StringSimilarity {
		private LevensteinDistance ld = new LevensteinDistance();

		@Override
		public double similarity(String s, String t) {
			String[] swords = s.split(" ");
			String[] twords = t.split(" ");

			double matching = 0.0;
			outer: for (String sw : swords) {
				for (String tw: twords) {
	                if (ld.getDistance(sw, tw) > 0.8) {
	                    matching += 1;
	                    continue outer;
	                }
				}
			}
	        return matching / max(swords.length, twords.length);
	    }
		public double similarity(PreparedString a, PreparedString b) {
			return similarity(a.string, b.string);
		}
	}
	public static class WeightedByWordLevenshtein implements StringSimilarity {
		protected LevensteinDistance ld = new LevensteinDistance();
		protected Weighter weighter;
		protected double levenshteinThreshold;

		public WeightedByWordLevenshtein(Weighter weighter) {
			super();
			this.weighter = weighter;
			this.levenshteinThreshold = 0.8;
		}

		public WeightedByWordLevenshtein(Weighter weighter, double levenshteinThreshold) {
			super();
			this.weighter = weighter;
			this.levenshteinThreshold = levenshteinThreshold;
		}

		@Override
		public double similarity(String s, String t) {
			return similarity(new PreparedString(s, weighter), new PreparedString(t, weighter));
	    }

	    @Override
		public double similarity(PreparedString s, PreparedString t) {
			String[] swords = s.words;
			String[] twords = t.words;

			double[] sweights = s.weights;
			double[] tweights = t.weights;
			double sumWeights = 0.0;
			for (int i = 0; i < swords.length; i++) {
				sumWeights += sweights[i];
			}
			for (int i = 0; i < twords.length; i++) {
				sumWeights += tweights[i];
			}
			double matching = 0.0;

			outer: for (int i = 0; i < swords.length; i++) {
				double sWeight = sweights[i] / sumWeights;
				String sw = swords[i];
				for (int j = 0; j < twords.length; j++) {
					double tWeight = tweights[j] / sumWeights;
					String tw = twords[j];
					if (ld.getDistance(sw, tw) > levenshteinThreshold) {
						matching += sWeight + tWeight;
						continue outer;
					}
				}

			}

	        return min(matching, 1.0);
	    }
	}

	public static class AFocusedByWordLevenshtein implements StringSimilarity {
		protected LevensteinDistance ld = new LevensteinDistance();
		protected double levenshteinThreshold = 0.8;

		public AFocusedByWordLevenshtein() {
			super();
		}

		@Override
		public double similarity(String s, String t) {
			String[] swords = s.split(" ");
			String[] twords = t.split(" ");
			return _similarity(swords, twords);
	    }
		public double similarity(PreparedString s, PreparedString t) {
			String[] swords = s.words;
			String[] twords = t.words;
			return _similarity(swords, twords);
		}

	    private double _similarity(String[] swords, String[] twords) {
			double matching = 0.0;
			outer: for (String sw : swords) {
				for (String tw: twords) {
	                if (ld.getDistance(sw, tw) > 0.8) {
	                    matching += 1;
	                    continue outer;
	                }
				}
			}
			double mx = max(swords.length, twords.length);
	        return ((matching / swords.length) * 0.8) + ((matching / mx) * 0.2);
	    }
	}

	public static class WeightedByWordAndPositionLevenshtein extends WeightedByWordLevenshtein {

		public WeightedByWordAndPositionLevenshtein(Weighter weighter) {
			super(weighter);
		}

		@Override
		public double similarity(String s, String t) {
			return similarity(new PreparedString(s, weighter), new PreparedString(t, weighter));
	    }

		public double similarity(PreparedString s, PreparedString t) {
			String[] swords = s.words;
			String[] twords = t.words;
			double[] sweights = Arrays.copyOf(s.weights, s.weights.length);
			double[] tweights = Arrays.copyOf(t.weights, t.weights.length);
			double sumWeights = 0.0;
			for (int i = 0; i < swords.length; i++) {
				sumWeights += sweights[i];
			}
			for (int i = 0; i < twords.length; i++) {
				sumWeights += tweights[i];
			}

			double matching = 0.0;
			outer: for (int i = 0; i < swords.length; i++) {
				double sWeight = sweights[i] / sumWeights;
				String sw = swords[i];
				for (int j = 0; j < twords.length; j++) {
					double tWeight = tweights[j] / sumWeights;
					String tw = twords[j];
					if (ld.getDistance(sw, tw) > levenshteinThreshold) {
						double weight = sWeight + tWeight;
						sweights[i] = 0.0;
						tweights[j] = 0.0;
						if (i != j)
							weight *= 0.5;
						matching += weight;
						continue outer;
					}
				}

			}

	        return min(matching, 1.0);
	    }
	}
}
