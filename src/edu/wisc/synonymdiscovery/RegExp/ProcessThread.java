package edu.wisc.synonymdiscovery.RegExp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;

import edu.wisc.synonymdiscovery.Beans.Options;
import edu.wisc.synonymdiscovery.Beans.Synonym;
import edu.wisc.synonymdiscovery.Logging.HtmlFormatter;
import edu.wisc.synonymdiscovery.Utils.CandidateSynonymScoringInfo;
import edu.wisc.synonymdiscovery.Utils.Dataset;
import edu.wisc.synonymdiscovery.Utils.Match;
import edu.wisc.synonymdiscovery.Utils.RegexUtils;
import edu.wisc.synonymdiscovery.Utils.SimilarityMeasure;



public class ProcessThread implements Runnable {

	private String regex;
	private String dataPath;

	private Options options;
	//shared queue used for the purpose of the status updation
	private static BlockingQueue<Object> sharedQueue;	
	//candidate queue used for the purpose of passing candidate set
	private static BlockingQueue<Object> candidateQueue;
	private ArrayList<Synonym> candidateSynonyms;
	private String sid;
	private String guid;
	private static Logger logger = Logger.getLogger("Log");  
	private FileHandler fh;  
	private boolean isLoggingEnabled = false;

	public boolean isLoggingEnabled() {
		return isLoggingEnabled;
	}

	public void setLoggingEnabled(boolean isLoggingEnabled) {
		this.isLoggingEnabled = isLoggingEnabled;
	}

	public static Logger getLogger() {
		return logger;
	}

	public String getDataPath() {
		return dataPath;
	}

	public void setDataPath(String dataPath) {
		this.dataPath = dataPath;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public static BlockingQueue<Object> getSharedQueue() {
		return sharedQueue;
	}

	public String getRegex() {
		return regex;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public Options getOptions() {
		return options;
	}




	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getSid() {
		return sid;
	}



	/** Constructor
	 * 
	 * @param queue
	 * @param candQueue
	 */
	public ProcessThread(BlockingQueue<Object> queue,BlockingQueue<Object> candQueue) {
		sharedQueue = queue;
		candidateQueue = candQueue;
	}

	/** Thread run 
	 *  Main thread for processing and generation of candidates
	 */
	public void run() {
		// TODO Auto-generated method stub				
		try {
			//create RegexTemp directory			
			File dir = new File("RegexTemp");			
			if(!dir.exists())
			{
				boolean dirCreated = dir.mkdir();
				if(dirCreated)
					addToQueue("RegexTemp directory created");
			}
			//set up the logger
			if(!isLoggingEnabled)
				logger.setLevel(Level.OFF);
			else
			{
				logger.setLevel(Level.ALL);
				fh = new FileHandler("RegexTemp/log_"+this.getGuid()+".html");  
				logger.addHandler(fh);
				HtmlFormatter formatter = new HtmlFormatter();  
				fh.setFormatter(formatter);
			}

			File fcn = new File("candidates.txt");
			fcn.delete();

			File repFile = new File("RegexTemp/cand_"+this.getGuid()+".ser");
			if(repFile.exists())
				repFile.delete();
			addToQueue("Regular Expression : "+regex);
			//to check compilation
			@SuppressWarnings("unused")

			Pattern regPattern = Pattern.compile(regex);
			addToQueue("Reading Regular Expression - Started");
			RegexUtils regexParser = new RegexUtils(regex, options, false);
			addToQueue("Reading Regular Expression - Completed");

			addToQueue("Reading Other Input Parameters  - Started");
			Dataset dt = new Dataset(options, dataPath, regexParser.getSynCount(), false);
			addToQueue("Reading Other Input Parameters  - Completed");

			// reads the dataset and processes the regexes over the dataset
			addToQueue("Processing of Dataset - Started");
			HashMap<Synonym, ArrayList<Match>> candidateMatches = dt.matchRegExp(regexParser);
			addToQueue("Processing of Dataset - Completed");

			addToQueue("Computing the Vector Representation and Similarity of candidates - Started");
			SimilarityMeasure simMeasure = new SimilarityMeasure();							
			addToQueue("True synonyms : " + regexParser.getTrueSynonyms().toString());			
			simMeasure.computeScoringInfo(regexParser.getTrueSynonyms(), candidateMatches);
			addToQueue("Computing the Vector Representation and Similarity of candidates - Completed");

			ArrayList<CandidateSynonymScoringInfo> candidatesScoringInfo = new ArrayList<CandidateSynonymScoringInfo>();			
			//get candidate synonyms
			candidateSynonyms = simMeasure.getCandidateSynonyms();
			logger.info("Candidate Synonyms :"+ candidateSynonyms.toString());
			for (Synonym cand : candidateSynonyms ) {				
				candidatesScoringInfo.add(simMeasure.getCandidatesScoringInfo().get(cand));
			}			

			// need to sort in reverse order
			addToQueue("Sorting of Candidates according to Rank - Started");
			Collections.sort(candidatesScoringInfo);
			addToQueue("Sorting of Candidates according to Rank - Completed");

			candidateQueue.put(simMeasure.getCandidatesScoringInfo());
			candidateQueue.put(simMeasure.getMeanPrototypeScoringInfo());
			candidateQueue.put(candidateSynonyms);

			// write top 10 to file - serialize (writing to file may not be
			// required as we would pass candidates to main servlet)			
			addToQueue("Writing top-10 Candidates to file - Started");			
			HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>();
			File f = new File("candidates.txt");			
			BufferedWriter bwriter = new BufferedWriter(new FileWriter(f,false));
			ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();			
			for (CandidateSynonymScoringInfo candSynInfo : candidatesScoringInfo) {
				if (!candidateSynonyms.contains(candSynInfo.getCandidate())) continue;
				ArrayList<String> candList = new ArrayList<String>();
				String currSynonym = candSynInfo.getCandidate().toString();
				candList.add(currSynonym);
				candList.add(new Integer(candSynInfo.getMatches().size()).toString());
				int count = 0;
				for (Match m : candSynInfo.getMatches()) {
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
			bwriter.write(JSONValue.toJSONString(map));
			bwriter.close();			
			addToQueue("Writing top-10 Candidates to file - Completed");

		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			try {
				addToQueue(e1);				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.log(Level.SEVERE,e.getMessage(),e);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				addToQueue(e);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				logger.log(Level.SEVERE,e.getMessage(),e);
			}
		}
	}

	private void addToQueue(Object obj) throws InterruptedException {	
		if(obj instanceof Exception)	
			logger.log(Level.SEVERE,((Exception) obj).getMessage(),(Exception)obj);
		else
			logger.info(obj.toString());

		sharedQueue.put(obj);
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getGuid() {
		return guid;
	}


}
