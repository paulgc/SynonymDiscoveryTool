package edu.wisc.synonymdiscovery.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.wisc.synonymdiscovery.Beans.Synonym;

public class SimilarityMeasure {

	private HashMap<Synonym, CandidateSynonymScoringInfo> candidateScoringInfo;	
	private HashMap<Synonym, CandidateSynonymScoringInfo> prototypeScoringInfo;	
	private CandidateSynonymScoringInfo meanPrototypeScoringInfo;

	private ArrayList<Synonym> candidateSynonyms;

	/** Constructor
	 * 
	 * @param candidateMatches
	 */
	public SimilarityMeasure()
	{
		candidateScoringInfo = new HashMap<Synonym,CandidateSynonymScoringInfo>();	
		prototypeScoringInfo = new HashMap<Synonym,CandidateSynonymScoringInfo>();	
		meanPrototypeScoringInfo = new CandidateSynonymScoringInfo(new Synonym());

		candidateSynonyms = new ArrayList<Synonym>();
	}

	/** processVectors - computes the tf-idf vector representation for all the list of candidates passed
	 * 
	 * @param synList
	 * @throws InterruptedException
	 */
	public void computeVectors(List<Synonym> synList, HashMap<Synonym, ArrayList<Match>> candidateMatches, String flag) throws InterruptedException
	{

		int docCount = Match.getDocCount();		
		for(Synonym syn : synList)
		{
			if (candidateMatches.containsKey(syn))
			{
				CandidateSynonymScoringInfo cand = new CandidateSynonymScoringInfo(syn);	
				HashMap<String,Integer> preDocFreq = Match.getPreDocFreq();					
				HashMap<String,Integer> sufDocFreq = Match.getSufDocFreq();
				for(Match m : candidateMatches.get(syn))
				{										
					double preNormValue = 0;
					double sufNormValue = 0;					
					for(String s : m.getPrefixVector().keySet())
					{												
						cand.getPrefixWords().add(s);
						double tfIdfValue = m.getPreTermFreq().get(s)* Math.log10(docCount/(1+preDocFreq.get(s)));						
						m.getPrefixVector().put(s, tfIdfValue);
						preNormValue += (tfIdfValue * tfIdfValue);
					}

					for(String s : m.getSuffixVector().keySet())
					{						
						cand.getSuffixWords().add(s);
						double tfIdfValue = m.getSufTermFreq().get(s)* Math.log10(docCount/(1+sufDocFreq.get(s)));
						m.getSuffixVector().put(s, tfIdfValue);
						sufNormValue += (tfIdfValue * tfIdfValue);
					}
					m.setPrefixNorm(Math.sqrt(preNormValue));
					m.setSuffixNorm(Math.sqrt(sufNormValue));
					// set normalized values
					for(String s : m.getPrefixVector().keySet())
					{						
						m.getPrefixVector().put(s, m.getPrefixVector().get(s)/(1+m.getPrefixNorm()));					
					}
					for(String s : m.getSuffixVector().keySet())
					{						
						m.getSuffixVector().put(s, m.getSuffixVector().get(s)/(1+m.getSuffixNorm()));					
					}
				}
				cand.setMatches(candidateMatches.get(syn));					
				cand.calculateMeanVectors();
				if(flag.equals("TRUE_SYNONYM"))
					prototypeScoringInfo.put(syn, cand);
				else
					candidateScoringInfo.put(syn, cand);
			}

		}

	}


	public ArrayList<Synonym> getCandidateSynonyms() {
		return candidateSynonyms;
	}



	public HashMap<Synonym, CandidateSynonymScoringInfo> getCandidatesScoringInfo() {
		return candidateScoringInfo;
	}

	public CandidateSynonymScoringInfo getMeanPrototypeScoringInfo() {
		return meanPrototypeScoringInfo;
	}


	/** createMeanSynVector - calculates the mean prefix and suffix vector for the true synonym candidates
	 * 
	 * @param synList
	 * @return candidate representing the true synonyms
	 * @throws InterruptedException
	 */
	public void computeMeanPrototypeVector(List<Synonym> synList) throws InterruptedException
	{

		//calculate mean prefix and mean suffix vector locally
		HashMap<String,Double> meanPrefixVector = new HashMap<String,Double>();
		HashMap<String,Double> meanSuffixVector = new HashMap<String,Double>();
		for(Synonym syn : synList)
		{
			CandidateSynonymScoringInfo c = prototypeScoringInfo.get(syn);
			for(String str : c.getPrefixWords())
			{
				meanPrototypeScoringInfo.getPrefixWords().add(str);
				if(meanPrefixVector.get(str)==null)
					meanPrefixVector.put(str, 0.0);
				if (c.getMeanPrefixVector().containsKey(str))
					meanPrefixVector.put(str, meanPrefixVector.get(str)+c.getMeanPrefixVector().get(str));			
			}

			for(String str : c.getSuffixWords())
			{
				meanPrototypeScoringInfo.getSuffixWords().add(str);
				if(meanSuffixVector.get(str)==null)
					meanSuffixVector.put(str, 0.0);
				if (c.getMeanSuffixVector().containsKey(str))
					meanSuffixVector.put(str, meanSuffixVector.get(str)+c.getMeanSuffixVector().get(str));				
			}
		}	
		meanPrototypeScoringInfo.setMeanPrefixVector(meanPrefixVector);
		meanPrototypeScoringInfo.setMeanSuffixVector(meanSuffixVector);

	}

	/** processMatches - called from the main thread - Does the processing of all the matches found
	 * and computes the similarity scores of all the candidates
	 * @param synList
	 * @throws InterruptedException
	 */
	public void computeScoringInfo(List<List<String>> trueSynonyms, HashMap<Synonym, ArrayList<Match>> candidateMatches) throws InterruptedException
	{	
		List<Synonym> trueSyn = new ArrayList<Synonym>();
		for(Synonym candidate : candidateMatches.keySet())
		{
			boolean synFlag = true;
			List<String> tmpSynList = candidate.getSynonyms();
			for(int i=0;i<tmpSynList.size();i++)
				if(!trueSynonyms.get(i).contains(tmpSynList.get(i))) {
					synFlag = false;
					break;
				}
			if(synFlag)
				trueSyn.add(candidate);
			if (!synFlag && !candidateSynonyms.contains(candidate)) {
				candidateSynonyms.add(candidate);
			}
		}				
		computeVectors(trueSyn, candidateMatches,"TRUE_SYNONYM");
		computeMeanPrototypeVector(trueSyn);

		computeVectors(candidateSynonyms, candidateMatches,"CANDIDATE_SYNONYM");			
		for(Synonym syn : candidateSynonyms)
		{
			CandidateSynonymScoringInfo c = candidateScoringInfo.get(syn);
			c.computeSimilarityScores(meanPrototypeScoringInfo);
		}		
	}



}
