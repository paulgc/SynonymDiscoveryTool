package edu.wisc.synonymdiscovery.Beans;

public class Options {
	private boolean multiLineFlag =  false;
	private int numContextWords;
	private int minSynLength;
	private int numSynWords;
	private int anyNumCharBound;
	
	public boolean isMultiLineFlag() {
		return multiLineFlag;
	}

	public void setMultiLineFlag(boolean multiLineFlag) {
		this.multiLineFlag = multiLineFlag;
	}

	public int getNumContextWords() {
		return numContextWords;
	}

	public void setNumContextWords(int numContextWords) {
		this.numContextWords = numContextWords;
	}

	public int getMinSynLength() {
		return minSynLength;
	}

	public void setMinSynLength(int minSynLength) {
		this.minSynLength = minSynLength;
	}

	public int getNumSynWords() {
		return numSynWords;
	}

	public void setNumSynWords(int numSynWords) {
		this.numSynWords = numSynWords;
	}

	public int getAnyNumCharBound() {
		return anyNumCharBound;
	}

	public void setAnyNumCharBound(int anyNumCharBound) {
		this.anyNumCharBound = anyNumCharBound;
	}

	public Options(boolean multiLineFlag, int numContextWords, int minSynLength, int numSynWords, int anyNumCharBound) {
		this.multiLineFlag = multiLineFlag;
		this.numContextWords = numContextWords;
		this.minSynLength = minSynLength;
		this.anyNumCharBound = anyNumCharBound;
		this.numSynWords = numSynWords;
	}
	
	public Options() {
	}
	
}
