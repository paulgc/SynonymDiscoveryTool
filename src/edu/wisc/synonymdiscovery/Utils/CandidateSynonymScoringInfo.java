package edu.wisc.synonymdiscovery.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.wisc.synonymdiscovery.Beans.Synonym;

public class CandidateSynonymScoringInfo implements Comparable<CandidateSynonymScoringInfo> {

	private Synonym candidate;
	private Set<String> prefixWords, suffixWords;
	private ArrayList<Match> matches;
	private HashMap<String,Double> meanPrefixVector;
	private HashMap<String,Double> meanSuffixVector;
	private double prefixSimScore = 0;
	private double suffixSimScore = 0;
	private double totalSimScore = 0;	

	public CandidateSynonymScoringInfo(Synonym candidate)
	{
		this.setCandidate(candidate);		
		meanPrefixVector = new HashMap<String,Double>();
		meanSuffixVector = new HashMap<String,Double>();
		prefixWords = new HashSet<String>();
		suffixWords = new HashSet<String>();
	}

	public ArrayList<Match> getMatches() {
		return matches;
	}

	public void setMatches(ArrayList<Match> matches) {
		this.matches = matches;
	}

	public void setPrefixWords(Set<String> prefixWords) {
		this.prefixWords = prefixWords;
	}

	public Set<String> getPrefixWords() {
		return prefixWords;
	}

	public void setSuffixWords(Set<String> suffixWords) {
		this.suffixWords = suffixWords;
	}

	public Set<String> getSuffixWords() {
		return suffixWords;
	}

	public void setMeanPrefixVector(HashMap<String,Double> meanPrefixVector) {
		this.meanPrefixVector = meanPrefixVector;
	}

	public HashMap<String,Double> getMeanPrefixVector() {
		return meanPrefixVector;
	}

	public void setMeanSuffixVector(HashMap<String,Double> meanSuffixVector) {
		this.meanSuffixVector = meanSuffixVector;
	}

	public HashMap<String,Double> getMeanSuffixVector() {
		return meanSuffixVector;
	}

	/** calculateMeanVectors - calculates the mean vector for all the synonym candidates
	 * 
	 * @throws InterruptedException
	 */
	public void calculateMeanVectors() throws InterruptedException
	{		
		// mean prefix vector
		for(String str : prefixWords)
		{
			meanPrefixVector.put(str, 0.0);

			//loop through all matches
			for(Match m : matches)
			{				

				if(m.getPrefixVector().containsKey(str))
					meanPrefixVector.put(str, meanPrefixVector.get(str)+m.getPrefixVector().get(str));
			}
		}		
		//mean suffix vector
		for(String str : suffixWords)
		{
			meanSuffixVector.put(str, 0.0);
			//loop through all matches
			for(Match m : matches)
			{
				if(m.getSuffixVector().containsKey(str))
					meanSuffixVector.put(str, meanSuffixVector.get(str)+m.getSuffixVector().get(str));
			}
		}	
	}

	/** computeSimilarityScores - compute the prefix, suffix and total similarity scores for all the candidate synonyms 
	 * with respect to the mean synonym
	 * @param meanSyn
	 * @throws InterruptedException
	 */
	public void computeSimilarityScores(CandidateSynonymScoringInfo prototypeScoringInfo) throws InterruptedException
	{
		for(String word : meanPrefixVector.keySet())
		{
			if (prototypeScoringInfo.getMeanPrefixVector().containsKey(word))
				prefixSimScore += (meanPrefixVector.get(word)*prototypeScoringInfo.getMeanPrefixVector().get(word));
		}

		for(String word : meanSuffixVector.keySet())
		{
			if (prototypeScoringInfo.getMeanSuffixVector().containsKey(word))
				suffixSimScore += (meanSuffixVector.get(word)*prototypeScoringInfo.getMeanSuffixVector().get(word));
		}

		setTotalSimScore(prefixSimScore + suffixSimScore);

	}

	public void setTotalSimScore(double totalSimScore) {
		this.totalSimScore = totalSimScore;
	}

	public double getTotalSimScore() {
		return totalSimScore;
	}

	public void setCandidate(Synonym candidate) {
		this.candidate = candidate;
	}

	public Synonym getCandidate() {
		return candidate;
	}

	public void setPrefixVectorWeight(String word,Double weight)
	{
		this.meanPrefixVector.put(word, weight);
	}

	public void setSuffixVectorWeight(String word,Double weight)
	{
		this.meanSuffixVector.put(word, weight);
	}

	@Override
	public int compareTo(CandidateSynonymScoringInfo compareCand)
	{		
		return Double.compare(compareCand.getTotalSimScore(), this.getTotalSimScore());		
	}



}
