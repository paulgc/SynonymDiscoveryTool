package edu.wisc.synonymdiscovery.Feedback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.json.simple.JSONValue;

import edu.wisc.synonymdiscovery.Beans.Synonym;
import edu.wisc.synonymdiscovery.Logging.ReportObject;
import edu.wisc.synonymdiscovery.Utils.CandidateSynonymScoringInfo;
import edu.wisc.synonymdiscovery.Utils.Match;


public class FeedbackThread implements Runnable {

	private HashMap<Synonym,CandidateSynonymScoringInfo> candidateScoringInfo;
	private CandidateSynonymScoringInfo meanPrototypeScoringInfo;


	private ArrayList<Synonym> candSynonyms;
	private int iteration;
	private HashMap<Synonym,Boolean> feedback = new HashMap<Synonym,Boolean>();
	private ArrayList<Synonym> relevantRes = new ArrayList<Synonym>();
	private ArrayList<Synonym> nonRelevantRes = new ArrayList<Synonym>();
	private HashMap<String,Double> relevantPreVector = new HashMap<String,Double>();
	private HashMap<String,Double> nonRelevantPreVector = new HashMap<String,Double>();
	private HashMap<String,Double> relevantSufVector = new HashMap<String,Double>();
	private HashMap<String,Double> nonRelevantSufVector = new HashMap<String,Double>();

	//queues
	private static BlockingQueue<Object> msgQueue;	
	private static BlockingQueue<Object> candidateQueue;
	private static Logger logger = Logger.getLogger("Log");  
	private FileHandler fh;  
	private String sid;
	private String guid;
	private boolean isLoggingEnabled;    

	/** Constructor 
	 * 
	 * @param queue - for message passing
	 * @param candQueue - for the passing of candidate set
	 */
	public FeedbackThread(BlockingQueue<Object> queue,BlockingQueue<Object> candQueue) {
		msgQueue = queue;
		candidateQueue = candQueue;
	}   

	public HashMap<Synonym, CandidateSynonymScoringInfo> getCandidateScoringInfo() {
		return candidateScoringInfo;
	}

	public void setCandidateScoringInfo(HashMap<Synonym, CandidateSynonymScoringInfo> candidateScoringInfo) {
		this.candidateScoringInfo = candidateScoringInfo;
	}

	public CandidateSynonymScoringInfo getMeanPrototypeScoringInfo() {
		return meanPrototypeScoringInfo;
	}

	public void setMeanPrototypeScoringInfo(
			CandidateSynonymScoringInfo meanPrototypeScoringInfo) {
		this.meanPrototypeScoringInfo = meanPrototypeScoringInfo;
	}

	public ArrayList<Synonym> getCandSynonyms() {
		return candSynonyms;
	}

	public void setCandSynonyms(ArrayList<Synonym> candSynonyms) {
		this.candSynonyms = candSynonyms;
	}

	/** setFeedback - reads the feedback given by the user
	 * 
	 * @param feedbackParam
	 * @throws InterruptedException 
	 */
	public void setFeedback(String feedbackParam) throws InterruptedException {
		String[] splitFeedback = feedbackParam.split("\\|"); 				
		int i=0;		
		while (i<splitFeedback.length)
		{	

			feedback.put(Synonym.constructSynonym(splitFeedback[i],","), (splitFeedback[i+1].equals("y"))?true:false);
			i+=2;			
		}				
	}

	/** calculateRelevanceFactors - Processes the relevant and non-relevant results flagged by the user
	 * 
	 * @throws InterruptedException
	 */
	public void calculateRelevanceFactors() throws InterruptedException
	{
		for(Synonym syn : feedback.keySet())
		{
			//	String s = str.trim().toLowerCase();
			if (feedback.get(syn))
				relevantRes.add(syn);
			else
				nonRelevantRes.add(syn);			
		}
		addToQueue(relevantRes.toString());
		addToQueue(nonRelevantRes.toString());
		for(String s : meanPrototypeScoringInfo.getPrefixWords())
		{
			relevantPreVector.put(s, 0.0);
			nonRelevantPreVector.put(s, 0.0);
			for(Synonym syn : relevantRes)
			{				
				if(relevantPreVector.containsKey(s))
				{
					if(candidateScoringInfo.get(syn).getMeanPrefixVector().containsKey(s))
						relevantPreVector.put(s, relevantPreVector.get(s)+candidateScoringInfo.get(syn).getMeanPrefixVector().get(s));
				}				
			}
			for(Synonym syn : nonRelevantRes)
			{				
				if(nonRelevantPreVector.containsKey(s))
				{													
					if (candidateScoringInfo.get(syn).getMeanPrefixVector().containsKey(s))
						nonRelevantPreVector.put(s, nonRelevantPreVector.get(s)+candidateScoringInfo.get(syn).getMeanPrefixVector().get(s));
				}				
			}
		}
		for(String s : meanPrototypeScoringInfo.getSuffixWords())
		{
			relevantSufVector.put(s, 0.0);
			nonRelevantSufVector.put(s, 0.0);
			for(Synonym syn : relevantRes)
			{				
				if(relevantSufVector.containsKey(s))
				{
					if(candidateScoringInfo.get(syn).getMeanSuffixVector().containsKey(s))
						relevantSufVector.put(s, relevantSufVector.get(s)+candidateScoringInfo.get(syn).getMeanSuffixVector().get(s));
				}				
			}
			for(Synonym syn : nonRelevantRes)
			{				
				if(nonRelevantSufVector.containsKey(s))
				{					
					if(candidateScoringInfo.get(syn).getMeanSuffixVector().containsKey(s))
						nonRelevantSufVector.put(s, nonRelevantSufVector.get(s)+candidateScoringInfo.get(syn).getMeanSuffixVector().get(s));
				}				
			}
		}
	}

	/** manipulateVectors - main method that does the relevance feedback process by updating the weights of the mean synonym
	 * vector based on the relevant and the non-relevant results flagged by the user
	 * 
	 */
	public void manipulateVectors()
	{
		int numRelevantRes = 0;
		int numNonRelevantRes = 0;
		//smoothing
		if (relevantRes.size() == 0)
			numRelevantRes = 1;
		if (nonRelevantRes.size() == 0)
			numNonRelevantRes = 1;
		for(String s : meanPrototypeScoringInfo.getPrefixWords())
		{
			double newValue = meanPrototypeScoringInfo.getMeanPrefixVector().get(s) + (0.75 * (relevantPreVector.get(s)/(1+numRelevantRes))) 
					- (0.25 * (nonRelevantPreVector.get(s)/(1+numNonRelevantRes)));
			meanPrototypeScoringInfo.setPrefixVectorWeight(s, newValue);			
		}
		for(String s : meanPrototypeScoringInfo.getSuffixWords())
		{
			double newValue = meanPrototypeScoringInfo.getMeanSuffixVector().get(s) + (0.75 * (relevantSufVector.get(s)/(1+numRelevantRes))) 
					- (0.25 * (nonRelevantSufVector.get(s)/(1+numNonRelevantRes)));
			meanPrototypeScoringInfo.setSuffixVectorWeight(s, newValue);			
		}

	}

	@Override
	public void run() {		
		try {
			//set up the logger
			if(!isLoggingEnabled)
				logger.setLevel(Level.OFF);
			else
			{
				logger.setLevel(Level.ALL);
				fh = new FileHandler("RegexTemp/log_"+this.getGuid());  
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();  
				fh.setFormatter(formatter);  
			}			


			addToQueue("Calculating Relevance Factors - Started");
			calculateRelevanceFactors();
			addToQueue("Calculating Relevance Factors - Completed");
			addToQueue("Manipulating Candidate Vectors - Started");
			manipulateVectors();
			addToQueue("Manipulating Candidate Vectors - Completed");
			//remove the current set from candSet
			if(!relevantRes.isEmpty())
			{
				writeToReport(relevantRes);
			}
			for (Synonym s : relevantRes)
			{
				candidateScoringInfo.remove(s);
				candSynonyms.remove(s);
			}

			for (Synonym s : nonRelevantRes)
			{
				candidateScoringInfo.remove(s);
				candSynonyms.remove(s);
			}
			// recompute similarity scores
			CandidateSynonymScoringInfo meanCand = meanPrototypeScoringInfo;
			addToQueue("Re-computing Similarity Scores - Started");
			for (Synonym syn : candSynonyms) {
				CandidateSynonymScoringInfo c = candidateScoringInfo.get(syn);			
				c.computeSimilarityScores(meanCand);
			}
			addToQueue("Re-computing Similarity Scores - Completed");
			candidateQueue.put(candidateScoringInfo);
			candidateQueue.put(meanPrototypeScoringInfo);
			candidateQueue.put(candSynonyms);
			//write to file
			ArrayList<CandidateSynonymScoringInfo> candidates = new ArrayList<CandidateSynonymScoringInfo>();
			for (Synonym syn : candSynonyms) {
				candidates.add(candidateScoringInfo.get(syn));
			}			
			// need to sort in reverse order
			addToQueue("Sorting Candidates according to Rank - Started");
			Collections.sort(candidates);
			addToQueue("Sorting Candidates according to Rank - Completed");			
			// write top 10 to file - serialize (writing to file may not be
			// required as we would pass candidates to main servlet)
			addToQueue("Writing top-10 Candidates to file - Started");
			HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>();
			File f = new File("candidates.txt");
			BufferedWriter br = new BufferedWriter(new FileWriter(f,false));
			ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();			
			for (CandidateSynonymScoringInfo cand : candidates) {
				ArrayList<String> candList = new ArrayList<String>();

				candList.add(cand.getCandidate().toString());			
				candList.add(new Integer(cand.getMatches().size()).toString());
				int count = 0;
				for (Match m : cand.getMatches()) {
					if (count == 3)
						break;
					candList.add(m.getMatchText());
					count++;
				}				
				list.add(candList);
				if (list.size() == 10)
					break;
			}
			map.put("candidates", list);
			br.write(JSONValue.toJSONString(map));
			br.close();			
			addToQueue("Writing top-10 Candidates to file - Completed");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			try {
				addToQueue(e);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				logger.log(Level.SEVERE,e1.getMessage(),e1);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				addToQueue(e);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				logger.log(Level.SEVERE,e1.getMessage(),e1);
			}
		}

	}

	/**
	 * Serializes the relevant results
	 * @param relevantRes
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void writeToReport(ArrayList<Synonym> relevantRes) throws Exception
	{
		ArrayList<ReportObject> reportCandidates = new ArrayList<ReportObject>();
		String serializedFile = "RegexTemp/cand_"+this.getGuid()+".ser";

		File file = new File(serializedFile);
		if(file.exists())
		{
			//try to get the object
			FileInputStream fileIn = new FileInputStream(serializedFile);
			ObjectInputStream in = new ObjectInputStream(fileIn);  			
			reportCandidates = (ArrayList<ReportObject>) in.readObject();					
			in.close();
			fileIn.close();
		}		

		for(Synonym cand : relevantRes)
		{
			ReportObject ro = new ReportObject();			
			CandidateSynonymScoringInfo c = candidateScoringInfo.get(cand);
			ro.setCandidate(c.getCandidate().toString());			
			ro.setTotalSimScore(c.getTotalSimScore());
			ro.setIteration(iteration);
			int count = 0;
			ArrayList<Match> matches = new ArrayList<Match>();
			for(Match m : c.getMatches())
			{
				if (count == 3) break;
				matches.add(m);
				count++;
			}
			ro.setMatches(matches);			
			reportCandidates.add(ro);
		}
		FileOutputStream fp = new FileOutputStream(serializedFile);
		ObjectOutputStream out = new ObjectOutputStream(fp);
		out.writeObject(reportCandidates);
		out.close();
		fp.close();

	}


	private void addToQueue(Object obj) throws InterruptedException {
		// TODO Auto-generated method stub		
		if(obj instanceof Exception)	
			logger.log(Level.SEVERE,((Exception) obj).getMessage(),(Exception)obj);
		else
			logger.info(obj.toString());

		msgQueue.put(obj);
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getSid() {
		return sid;
	}

	public void setLoggingEnabled(boolean isLoggingEnabled) {
		this.isLoggingEnabled = isLoggingEnabled;
	}

	public boolean isLoggingEnabled() {
		return isLoggingEnabled;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public int getIteration() {
		return iteration;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getGuid() {
		return guid;
	}

}
