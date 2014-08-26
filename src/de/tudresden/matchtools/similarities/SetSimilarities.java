package de.tudresden.matchtools.similarities;

import java.util.Set;

import com.google.common.collect.Sets;

public abstract class SetSimilarities {

	public static class Jaccard<T> implements SetSimilarity<T> {

		public double similarity(Set<T> a, Set<T> b) {
			int aSize = a.size();
			int common = 0;
			int unionSize = aSize;

			for (Object o : a) {
				if (b.contains(o))
					common += 1;
			}
			for (Object o : b) {
				if (!a.contains(o))
					unionSize += 1;
			}

			return (float)common / (float)unionSize;
		}
	}

	public static class GoogleJaccard<T> implements SetSimilarity<T> {

		public double similarity(Set<T> a, Set<T> b) {
			int aSize = a.size();
			int bSize = b.size();
			Set<T> swap;
			if (bSize < aSize) {
				swap = a;
				a = b;
				b = swap;
			}

			Set<T> intrsView = Sets.intersection(a, b);
			Set<T> unionView = Sets.union(a, b);

			return (float)intrsView.size() / (float)unionView.size();
		}
	}

}