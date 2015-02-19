package edu.wisc.synonymdiscovery.Utils;

import java.util.HashMap;

public class DFUtils {

	private HashMap<String,Integer> prefixDocFreq;
	private HashMap<String,Integer> suffixDocFreq;
	private int docCount;

	public DFUtils() {
		prefixDocFreq = new HashMap<String,Integer>();
		suffixDocFreq = new HashMap<String,Integer>();
		docCount = 0;
	}

	public int getPrefixDocFreq(String token) {
		if(prefixDocFreq.get(token)!=null)
			return prefixDocFreq.get(token);
		return 0;
	}

	public int getSuffixDocFreq(String token) {
		if(suffixDocFreq.get(token)!=null)
			return suffixDocFreq.get(token);
		return 0;
	}

	public int getDocCount() {
		return docCount;
	}

	public void incrementDocCount() {
		this.docCount++;
	}

	public void incrementPrefixDocFreq(String token) {
		if(prefixDocFreq.get(token) == null)
			prefixDocFreq.put(token, 1);
		else
			prefixDocFreq.put(token, prefixDocFreq.get(token)+1);

	}

	public void incrementSuffixDocFreq(String token) {
		if(suffixDocFreq.get(token) == null)
			suffixDocFreq.put(token, 1);
		else
			suffixDocFreq.put(token, suffixDocFreq.get(token)+1);

	}

}
