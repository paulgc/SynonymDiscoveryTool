package edu.wisc.synonymdiscovery.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import edu.wisc.synonymdiscovery.Beans.Options;
import edu.wisc.synonymdiscovery.Beans.Synonym;
import edu.wisc.synonymdiscovery.RegExp.ProcessThread;


public class RegexUtils {

	private String regExpression;
	private List<List<String>> trueSynonyms;
	private Options options;
	private ArrayList<String> regexWords;
	private int synCount;
	private BlockingQueue<Object> queue;
	private Logger logger;
	private boolean batchFlag;

	public int getSynCount() {
		return synCount;
	}
	public List<List<String>> getTrueSynonyms() {
		return trueSynonyms;
	}

	public ArrayList<String> getRegexWords()
	{
		return regexWords;
	}

	/** Constructor - Pass the regular expression and the maximum number of words in the synonym expected
	 * 
	 * @param regex
	 * @param numWords
	 */
	public RegexUtils(String regex,Options options, boolean batchFlag)
	{
		this.regExpression = regex;		
		this.options = options;
		this.batchFlag = batchFlag;
		this.regexWords = new ArrayList<String>(Arrays.asList(regex.split("[^\\w]+")));
		queue = ProcessThread.getSharedQueue();
		logger = ProcessThread.getLogger();
		this.trueSynonyms = new ArrayList<List<String>>();

		// Count number of syn tags in the regex
		int first = 0, cnt=0;
		while(true)
		{
			int index = regExpression.indexOf("|\\syn",first);		
			if(index < 0)
				break;
			cnt+=1;
			first = index + 4;
		}
		this.synCount = cnt;
	}

	public List<String> getCandidateRules(List<Synonym> candidateSynonyms) {
		List<String> rules = new ArrayList<String>();

		for(Synonym syn : candidateSynonyms) {
			StringBuffer sb = new StringBuffer(regExpression);
			int synPos=0;

			while(true)
			{
				int index = sb.indexOf("|\\syn");		
				if(index < 0)
					break;
				char[] regExpArray = sb.toString().toCharArray();
				int startIndex, endIndex;

				startIndex = index;
				while (regExpArray[startIndex] != '(' && startIndex >= 0)
				{
					startIndex--;
				}		
				endIndex = index;
				while (regExpArray[endIndex] != ')' && endIndex < sb.length())
				{
					endIndex++;
				}			
				sb.replace(startIndex+1, endIndex, syn.getSynonyms().get(synPos));
				synPos+=1;
			}
			rules.add(sb.toString());
		}

		return rules;
	}

	/** getGeneralizedRegexes - Generate all possible generalized regular expressions to be executed on the dataset
	 * 	
	 * @return ArrayList consisting of the generalized regexes
	 * @throws Exception
	 */
	public ArrayList<String> getGeneralizedRegexes() throws Exception
	{		
		ArrayList<String> genExpressions = new ArrayList<String>();

		// Find positions of syn tag
		List<Integer> synPositions = new ArrayList<Integer>();
		int first = 0;
		while(true)
		{
			int index = regExpression.indexOf("|\\syn",first);		
			if(index < 0)
				break;
			synPositions.add(index);
			first = index + 4;
		}


		for(int synIndex : synPositions)
		{
			int fixedPartPos = synPositions.indexOf(synIndex)+1;
			StringBuffer sb = new StringBuffer(regExpression);
			char[] regExpArray = sb.toString().toCharArray();
			int startIndex, endIndex;
			String synString;
			startIndex = synIndex;

			while (regExpArray[startIndex] != '(' && startIndex >= 0)
			{
				startIndex--;
			}		

			endIndex = synIndex;
			while (regExpArray[endIndex] != ')' && endIndex < sb.length())
			{
				endIndex++;
			}			

			synString = (sb.substring(startIndex+1, endIndex));
			List<String> tmp = Arrays.asList(synString.toLowerCase().split("\\|"));
			trueSynonyms.add(tmp);		

			if(synPositions.size() > 1) {
				synString = synString.replace("|\\syn", "");
				sb.replace(startIndex, endIndex+1, "(?<synonym"+fixedPartPos+">("+synString+"))");
			}

			regExpArray = sb.toString().toCharArray();

			Integer[] tmpNumSynWords = new Integer[options.getNumSynWords()];
			for(int i=1;i<=options.getNumSynWords();i++)
				tmpNumSynWords[i-1] = i;

			ICombinatoricsVector<Integer> originalVector = Factory.createVector(tmpNumSynWords);
			int var = synCount - 1;
			if(var==0)
				var=1;
			Generator<Integer> gen = Factory.createPermutationWithRepetitionGenerator(originalVector,var);

			for (ICombinatoricsVector<Integer> perm : gen) {
				int groupIndex = 1;
				StringBuffer tmpSb =new StringBuffer(sb);
				int permIndex = 0;
				while(true)
				{

					if(groupIndex==fixedPartPos && synPositions.size()>1)
						groupIndex+=1;
					int index = sb.indexOf("\\syn");
					if(index<0)
						break;
					regExpArray = sb.toString().toCharArray();

					startIndex = index;
					while (regExpArray[startIndex] != '(' && startIndex >= 0)
					{
						startIndex--;
					}		
					endIndex = index;
					while (regExpArray[endIndex] != ')' && endIndex < sb.length())
					{
						endIndex++;
					}			
					synString = (sb.substring(startIndex+1, endIndex));

					StringBuilder synonymExp = new StringBuilder("(?<synonym"+groupIndex+">(");	
					groupIndex+=1;

					for(int j=1;j<=perm.getValue(permIndex);j++)
					{
						if (j == perm.getValue(permIndex)) synonymExp.append("[^\\s()0-9]+");
						else synonymExp.append("[^\\s()0-9]+[ ]+"); 
					}
					permIndex +=1;
					synonymExp.append("))");
					String regexp = sb.substring(0, startIndex) + synonymExp;
					if(endIndex+1 < sb.length())
						regexp += sb.substring(endIndex+1, sb.length());

					sb = new StringBuffer(regexp);

				}

				genExpressions.add(sb.toString());
				addToQueue(sb.toString());
				sb = tmpSb;

			}
		}	

		return genExpressions;

	}

	/**
	 * Adds the messages to the shared queue
	 * If logging is enabled, also logs the same
	 * @param obj
	 * @throws InterruptedException
	 */
	private void addToQueue(Object obj) throws InterruptedException {
		if(batchFlag)
			return;
		if(obj instanceof Exception)	
			logger.log(Level.SEVERE,((Exception) obj).getMessage(),(Exception)obj);
		else
			logger.info(obj.toString());
		queue.put(obj);
	}

}
