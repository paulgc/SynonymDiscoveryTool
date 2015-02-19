package edu.wisc.synonymdiscovery.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.wisc.synonymdiscovery.Beans.Options;
import edu.wisc.synonymdiscovery.Beans.Synonym;
import edu.wisc.synonymdiscovery.RegExp.ProcessThread;


public class Dataset {

	private Options options;

	private int synCount;
	private String dataPath = null;
	private ArrayList<String> stopWordList = new ArrayList<String>();;
	private ArrayList<String> fileList = new ArrayList<String>();
	private ArrayList<String> regexWords = new ArrayList<String>();
	private BlockingQueue<Object> queue;
	private Logger logger;
	private boolean batchFlag;


	/*
	 * Parameters : multiLineFlag - denotes if the context spans over multiple
	 * lines or a single line numContextWords - number of words to consider for
	 * the context of synonym minSynLength - minimum number of characters in the
	 * synonym
	 */
	public Dataset(Options options, String dataPath, int synCount, boolean batchFlag) {

		this.dataPath = dataPath;
		this.options = options;
		this.synCount = synCount;
		queue = ProcessThread.getSharedQueue();
		logger = ProcessThread.getLogger();
		this.batchFlag = batchFlag;
	}

	/**
	 * getStopWords - Reads the stop words from the file
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void getStopWords() throws FileNotFoundException, IOException,
	InterruptedException {
		BufferedReader br = null;
		try {
			String line = null;
			InputStream input = getClass().getResourceAsStream("stopwords.txt");

			br = new BufferedReader(new InputStreamReader(input));
			while ((line = br.readLine()) != null) {
				stopWordList.add(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			br.close();
		}
	}

	/**
	 * processDataSet - Main method that processes reads, pre-processes and
	 * generates matches
	 * 
	 * @param reg
	 *            - Regex Parser
	 * @throws Exception
	 */
	public HashMap<Synonym, ArrayList<Match>> matchRegExp(RegexUtils reg) throws Exception {
		try {
			HashMap<Synonym, ArrayList<Match>> candidateMatches;
			getStopWords();
			regexWords = reg.getRegexWords();
			
			// process the data in the files
			addToQueue("Read Files in datapath - Started");
			listFiles(dataPath);
			addToQueue("Read Files in datapath - Completed");
			
			addToQueue("Running Regex over Dataset and Pre-processing - Started");
			candidateMatches = readAndProcessFiles(reg.getGeneralizedRegexes());
			addToQueue("Running Regex over Dataset and Pre-processing - Completed");
			
			return candidateMatches;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * extractMatches - Takes each file as input and runs the regular expression
	 * extracting matches
	 * 
	 * @param fileName
	 * @param regex
	 * @param dataLines
	 * @return ArrayList of matches
	 * @throws Exception
	 */
	public HashMap<Synonym, ArrayList<Match>> extractMatches(String fileName, String regex,
			String origData, String preProcessedData) throws Exception {
		String[] preProcessedDataLines = preProcessedData.split("\\n");
		String[] origDataLines = origData.split("\\n");

		HashMap<Synonym, ArrayList<Match>> candidateMatches = new HashMap<Synonym, ArrayList<Match>>();
		for (int index=0;index<preProcessedDataLines.length;index++) {
			String line = preProcessedDataLines[index];
			Pattern regexPattern = Pattern.compile(regex,
					Pattern.CASE_INSENSITIVE);
			Matcher regexMatch = regexPattern.matcher(line);
			if (regexMatch.find()) {
				Synonym synonym = new Synonym();
				boolean flag = false;
				for (int i = 1; i <= synCount; i++) {
					String currSyn="";
					if (i<=regexMatch.groupCount() && regexMatch.group("synonym" + i) != null) {
						currSyn = regexMatch.group("synonym" + i);
					}

					synonym.getSynonyms().add(currSyn);

					if (currSyn.equals("") || currSyn.length() < options.getMinSynLength())
						flag = true;
				}
				if (flag)
					continue;

				int start = regexMatch.start();
				int end = regexMatch.end();
				String prefix = line.substring(0, start);
				String suffix = "";
				if (end < line.length())
					suffix = line.substring(end, line.length());
				String matchText = origDataLines[index];
				Match m = new Match(synonym, prefix, suffix, matchText, this.options.getNumContextWords());
				if (m != null) {
					m.setFileName(fileName);
					if(candidateMatches.get(synonym) == null)
						candidateMatches.put(synonym, new ArrayList<Match>());
					candidateMatches.get(synonym).add(m);
				}

			}
		}


		return candidateMatches;
	}

	/**
	 * preProcess - pre-processes the data read from each file
	 * 
	 * @param data
	 * @return cleanData
	 * @throws InterruptedException
	 */

	public String preProcess(String data) throws InterruptedException {
		// queue.put("Pre-processing Data - Started");
		// split it into words, special characters and spaces
		Pattern splitPattern = Pattern.compile("(\\s+|\\w+|[^\\w\\s])");
		Matcher splitMatch = splitPattern.matcher(data);

		StringBuilder preProcessedData = new StringBuilder();
		while (splitMatch.find()) {
			String token = splitMatch.group().toLowerCase();
			// do not consider the words which are stop words unless they occur
			// in the regex
			if (!stopWordList.contains(token) || regexWords.contains(token))
				// tokens.add(token);
				preProcessedData.append(token);
		}
		// shrink multiple white spaces
		Pattern spacePat = Pattern.compile("[^\\S\\n\\t\\r]{2,}");
		// shrink multiple tabs
		Pattern tabPat = Pattern.compile("[^\\S\\n\\r ]{2,}");
		Matcher sm = spacePat.matcher(preProcessedData);
		String input = sm.replaceAll(" ");
		Matcher tm = tabPat.matcher(input);
		String cleanData = tm.replaceAll("\\t");
		return cleanData;
	}


	/**
	 * readAndProcessFiles - loops through the entire list of files and calls
	 * the processData method
	 * 
	 * @param regExpressions
	 * @throws Exception
	 */
	public HashMap<Synonym, ArrayList<Match>> readAndProcessFiles(ArrayList<String> regExpressions)
			throws Exception {
		HashMap<Synonym, ArrayList<Match>> candidateMatches = new HashMap<Synonym, ArrayList<Match>>();


		addToQueue("Extracting matches - Started");
		for (String fileName : fileList) {
			String preProcessedData = "";
			StringBuilder data = new StringBuilder();
			String line = null;
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(fileName));
				while ((line = br.readLine()) != null) {
					if (!line.trim().equals("")) {
						data.append(line);
						data.append('\n');
					}
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				throw e;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw e;
			} finally {
				br.close();
			}
			String origData = data.toString();
			preProcessedData = preProcess(origData);
			for (String str : regExpressions) {
				HashMap<Synonym, ArrayList<Match>> currCandidateMatches = extractMatches(fileName, str, origData, preProcessedData);

				for(Synonym syn : currCandidateMatches.keySet()) {
					if(candidateMatches.get(syn) == null)
						candidateMatches.put(syn, new ArrayList<Match>());
					for(Match m : currCandidateMatches.get(syn))
						candidateMatches.get(syn).add(m);
				}
			}

		}
		addToQueue("Extracting matches - Completed"+candidateMatches.keySet().size());
		return candidateMatches;
	}

	/**
	 * listFiles - Get all the list of files that have to be read
	 * 
	 * @param dataPath
	 */
	public void listFiles(String dataPath) {
		File f = new File(dataPath);
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				this.listFiles(file.getPath());
			}
		} else {
			fileList.add(f.getPath());
		}
	}

	/**
	 * Add messages to the shared queue
	 * 
	 * @param obj
	 * @throws InterruptedException
	 */
	private void addToQueue(Object obj) throws InterruptedException {
		if(batchFlag)
			return;
		if (obj instanceof Exception)
			logger.log(Level.SEVERE, ((Exception) obj).getMessage(),
					(Exception) obj);
		else
			logger.info(obj.toString());
		queue.put(obj);
	}

}
