package de.tudresden.matchtools;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.function.tdouble.IntIntDoubleFunction;
import cern.colt.function.tint.IntIntIntFunction;
import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import de.tudresden.matchtools.datastructures.Match;
import de.tudresden.matchtools.datastructures.PreparedString;
import de.tudresden.matchtools.datastructures.MatchingIndices;

public class SimMatrix {
	protected DoubleMatrix2D M;
	protected String[] A;
	protected String[] B;

    public SimMatrix(DoubleMatrix2D m) {
        super();
        M = m;
        A = null;
        B = null;
    }

	public SimMatrix(DoubleMatrix2D m, String[] a, String[] b) {
		super();
		M = m;
		A = a;
		B = b;
	}

    public SimMatrix(DoubleMatrix2D m, PreparedString[] a, PreparedString[] b) {
        super();
        M = m;
        String[] unboxedA = new String[a.length];
        for (int i=0; i < a.length; i++) {
            unboxedA[i] = a[i].string;
        }
        String[] unboxedB = new String[b.length];
        for (int i=0; i < b.length; i++) {
            unboxedB[i] = b[i].string;
        }
        A = unboxedA;
        B = unboxedB;
    }

    public SimMatrix(DoubleMatrix2D m, String[] a, PreparedString[] b) {
        super();
        M = m;
        String[] unboxedB = new String[b.length];
        for (int i=0; i < b.length; i++) {
            unboxedB[i] = b[i].string;
        }
        A = a;
        B = unboxedB;
    }

	public DoubleMatrix2D getM() {
		return M;
	}

	public String[] getA() {
		return A;
	}

	public String[] getB() {
		return B;
	}

	int countMatches() {
		return M.cardinality();
	}

	int[] shape() {
		return new int[] {M.rows(), M.columns()};
	}

	protected class ThresholdFunction implements DoubleFunction {
		double threshold;

		public ThresholdFunction(double threshold) {
			this.threshold = threshold;
		}

		@Override
		public double apply(double d) {
			if (d >= threshold)
				return d;
			else
				return 0.0;
		}
	}

	public void selectThreshold(final double th) {
		M.assign(new ThresholdFunction(th));
	}

	public void selectMax() {
		DoubleMatrix2D t_h = maxima(M);
		DoubleMatrix2D t_v = maxima(M.viewDice()).viewDice();
		DoubleMatrix2D t_m = matMax(t_h, t_v);
		selectWhereLarger(t_m);
	}

	protected class DeltaFunction implements DoubleFunction {

		double delta;

		public DeltaFunction(double delta) {
			this.delta = delta;
		}

		@Override
		public double apply(double d) {
			return d - delta;
		}
	}

	public void selectMaxDelta(double delta) {
		DoubleFunction df = new DeltaFunction(delta);
 		DoubleMatrix2D t_h = maxima(M).assign(df);
        DoubleMatrix2D t_v = maxima(M.viewDice()).viewDice().assign(df);
        DoubleMatrix2D t_m = matMax(t_h, t_v);
        selectWhereLarger(t_m);
	}

	public void selectMaxDelta() {
		selectMaxDelta(0.1);
	}

	public void selectBipartiteGreedy() {
		int m = M.rows();
		int n = M.columns();
		BitSet usedRows = new BitSet(m);
		BitSet usedCols = new BitSet(n);
		int maxMatches = min(m, n);
		int matches = 0;

		List<MatchingIndices> rawMatches = getMatchingIndices();
		Collections.sort(rawMatches, Collections.reverseOrder());


		for (MatchingIndices mi : rawMatches) {
			int a = mi.a;
			int b = mi.b;
			double sim = mi.c;
			// still free?
			if  (usedRows.get(a) || usedCols.get(b))
				continue;

			usedRows.set(a);
			usedCols.set(b);
			M.viewRow(a).assign(0.0);
			M.viewColumn(b).assign(0.0);
			M.setQuick(a,b,sim);

			matches++;
			if (matches == maxMatches)
				break;
		}
	}

	public void selectAboveNoise() {
		StandardDeviation sd = new StandardDeviation();
		int m = A.length;
		int n = B.length;
		int i = 0, j = 0;
		double s = 0.0;

		for (i = 0; i < m; i++) {
			for (j = 0; j < n; j++) {
				double v = M.getQuick(i, j);
				sd.increment(v);
				s += v;
			}
		}
		double stdDev = sd.getResult();
		double noiseLevel = s / (m*n) + (2 * stdDev);
		// //System.out.println("noiselevel: "+noiseLevel);
		selectThreshold(noiseLevel);
	}

	protected class AverageFunction implements DoubleDoubleFunction {
		@Override
		public double apply(double d1, double d2) {
			return (d1+d2) / 2.0;
		}
	}

	protected AverageFunction avgFunction = new AverageFunction();

	public SimMatrix aggregateAvg(SimMatrix m) {
		M.assign(m.getM(), avgFunction);
		return this;
	}

	public SimMatrix aggregateMax(SimMatrix m) {
		M.assign(m.getM(), DoubleFunctions.max);
		return this;
	}

	protected class SignAndAddFunction implements DoubleDoubleFunction {
		@Override
		public double apply(double d1, double d2) {
			if (d2 > 0.0)
				return d1 + 1.0;
			else
				return d1;
		}
	}

	protected SignAndAddFunction signAndAddFunction = new SignAndAddFunction();

	public SimMatrix aggregateMajorityHorizontal(Collection<SimMatrix> mats) {
		M.assign(DoubleFunctions.sign);
		for (SimMatrix other : mats) {
			M.assign(other.getM(), signAndAddFunction);
		}
		int majority = (int)ceil((mats.size() + 1.0) / 2.0);
		M.assign(new ThresholdFunction(majority));
		return this;
	}

	public Map<Integer, Integer> getMatchingIndicesMapAtoB() {
        IntArrayList rl = new IntArrayList();
        IntArrayList cl = new IntArrayList();
        DoubleArrayList vl = new DoubleArrayList();
        M.getNonZeros(rl, cl, vl);

        Map<Integer, Integer> result = new HashMap<>();
        for (int i=0; i<rl.size(); i++) {
        	result.put(rl.get(i), cl.get(i));
        }
		return result;
    }

    public Map<Integer, MatchingIndices> getMatchingIndicesMapAtoBWithSim() {
        IntArrayList rl = new IntArrayList();
        IntArrayList cl = new IntArrayList();
        DoubleArrayList vl = new DoubleArrayList();
        M.getNonZeros(rl, cl, vl);

        Map<Integer, MatchingIndices> result = new HashMap<>();
        for (int i=0; i<rl.size(); i++) {
            result.put(rl.get(i), new MatchingIndices(rl.get(i), cl.get(i), vl.get(i)));
        }
        return result;
    }

  	public List<MatchingIndices> getMatchingIndices() {
        IntArrayList rl = new IntArrayList();
        IntArrayList cl = new IntArrayList();
        DoubleArrayList vl = new DoubleArrayList();
        M.getNonZeros(rl, cl, vl);

        List<MatchingIndices> result = new ArrayList<>(rl.size());
        for (int i=0; i<rl.size(); i++) {
        	result.add(new MatchingIndices(rl.get(i), cl.get(i), vl.get(i)));
        }
		return result;
    }

  	public List<Match> getMapping() {
        IntArrayList rl = new IntArrayList();
        IntArrayList cl = new IntArrayList();
        DoubleArrayList vl = new DoubleArrayList();
        M.getNonZeros(rl, cl, vl);

        List<Match> result = new ArrayList<>(rl.size());
        for (int i=0; i<rl.size(); i++) {
        	result.add(new Match(A[rl.get(i)], B[cl.get(i)], vl.get(i)));
        }
		return result;
    }

	public SimMatrix copy() {
		return new SimMatrix(M.copy(), A, B);
	}

    public double getMonogamy() {
    	DoubleMatrix2D partnerMatrix = partnerMatrix(M);
    	partnerMatrix.assign(M, new DoubleDoubleFunction() {
    		@Override
    		public double apply(double d1, double d2) {
    			return d2 / d1;
    		}
    	});
    	if (M.rows() < M.columns())
    		partnerMatrix = partnerMatrix.viewDice();
    	DoubleMatrix1D rowSums = DoubleFactory1D.dense.make(partnerMatrix.rows());
    	for (int i =0; i<rowSums.size(); i++) {
    		rowSums.setQuick(i, partnerMatrix.viewRow(i).zSum());
    	}
    	return rowSums.zSum() / rowSums.size();
    }

    public double getCoverageA() {
    	List<MatchingIndices> matches = getMatchingIndices();
    	BitSet matchedRows  = new BitSet(M.rows());
    	for (MatchingIndices m: matches) {
    		matchedRows.set(m.a);
    	}
    	return (double)matchedRows.cardinality() / (double)A.length;
    }

    public double getCoverageB() {
    	List<MatchingIndices> matches = getMatchingIndices();
    	BitSet matchedRows  = new BitSet(M.columns());
    	for (MatchingIndices m: matches) {
    		matchedRows.set(m.b);
    	}
    	return (double)matchedRows.cardinality() / (double)B.length;
    }

    public double getBestCoverage() {
    	return max(getCoverageA(), getCoverageB());
    }


    protected DoubleMatrix2D partnerMatrix(final DoubleMatrix2D mat) {
    	int n = mat.rows();
    	int m = mat.columns();
    	final IntMatrix2D entries = IntFactory2D.dense.make(n, m, 1);
    	entries.forEachNonZero(new IntIntIntFunction() {
    		@Override
			public int apply(int x, int y, int v) {
				if (mat.getQuick(x, y) > 0.0)
					return 1;
				else
					return 0;
			}
    	});

    	final int[] rowSums = new int[n];
    	final int[] colSums = new int[m];
    	for (int i=0; i<n; i++) {
    		rowSums[i] = entries.viewRow(i).zSum();
    	}
    	for (int i=0; i<m; i++) {
    		colSums[i] = entries.viewColumn(i).zSum();
    	}

    	DoubleMatrix2D partnerMatrix = DoubleFactory2D.dense.make(n, m, 1.0);
    	partnerMatrix.forEachNonZero(new IntIntDoubleFunction() {
			@Override
			public double apply(int x, int y, double v) {
				if (entries.getQuick(x, y) > 0)
					return rowSums[x] + colSums[y] - 1.0;
				else
					return 1.0;
			}
	    });
    	return partnerMatrix;
    }

	protected DoubleMatrix2D maxima(DoubleMatrix2D m) {
		DoubleMatrix2D rowMaxima = DoubleFactory2D.dense.make(m.rows(), 1);
		for (int i = 0; i < m.rows(); i++) {
			rowMaxima.setQuick(i, 0, m.viewRow(i).getMaxLocation()[0]);
		}
		return DoubleFactory2D.dense.repeat(rowMaxima, 1, m.columns());
	}

	protected DoubleMatrix2D matMax(DoubleMatrix2D a, DoubleMatrix2D b) {
		return a.assign(b, new DoubleDoubleFunction() {
			@Override
			public double apply(double x, double y) {
				return max(x, y);
			}
		});
	}

	protected void selectWhereLarger(DoubleMatrix2D b) {
		M.assign(b, new DoubleDoubleFunction() {
			@Override
			public double apply(double x, double y) {
				if (x >= y)
					return x;
				else
					return 0.0;
			}
		});
	}

	protected class Maximum {
		public int row;
		public int column;
		public double similarity;
	}

}
