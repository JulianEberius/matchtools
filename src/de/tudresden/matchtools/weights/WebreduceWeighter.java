package de.tudresden.matchtools.weights;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;

public class WebreduceWeighter extends Weighter {

	class TermFrequencyRequest {
		public String term;

		public TermFrequencyRequest() {
		}

		public TermFrequencyRequest(String t) {
			this.term = t;
		}
	}

	class TermFrequencyResult {
		public double frequency;

		public TermFrequencyResult() {
		}

		public TermFrequencyResult(double tf) {
			this.frequency = tf;
		}
	}

	private String serverUrl;
	private Gson gson;
	private Map<String, Double> cache = new HashMap<>();
	private final double DEFAULT = Double.MAX_VALUE;

	public WebreduceWeighter(String url) {
		this.serverUrl = url;
		this.gson = new Gson();
	}

	public double weight(String s) {
		double tf = DEFAULT;
		Double cached = cache.get(s);
		if (cached != null)
			tf = cached;
		else {
			TermFrequencyRequest tfReq = new TermFrequencyRequest(s);
			String reqStr = gson.toJson(tfReq);

			try {
				String resp = Request.Post(serverUrl + "/termFrequency")
						.bodyString(reqStr, ContentType.APPLICATION_JSON)
						.execute().returnContent().asString();
				TermFrequencyResult result = gson.fromJson(resp,
						TermFrequencyResult.class);

				cache.put(s, result.frequency);
				tf = result.frequency;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return 1.0 / tf;
	}

	public static void main(String[] args) throws ClientProtocolException,
			IOException {
		Weighter w = new WebreduceWeighter("http://141.76.47.133:9876");
		System.out.println(w.weight("united states"));
		System.out.println(w.weight("company"));
		System.out.println(w.weight("rea"));
	}
}
