package edu.wisc.synonymdiscovery.Utils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.wisc.synonymdiscovery.Beans.Synonym;



public class Match implements Serializable {

	private static final long serialVersionUID = 1L;
	private Synonym synonym;
	private String prefix;
	private HashMap<String,Double> prefixVector;
	private HashMap<String,Double> suffixVector;
	private double prefixNorm = 0;	
	private double suffixNorm = 0;
	private String suffix;
	private String fileName;
	private String matchText;
	private HashMap<String,Integer> preTermFreq;
	private HashMap<String,Integer> sufTermFreq;
	private static HashMap<String,Integer> preDocFreq = new HashMap<String,Integer>();
	private static HashMap<String,Integer> sufDocFreq = new HashMap<String,Integer>();
	private static int docCount = 0;
	private static StanfordLemmatizer lemmatizer = new StanfordLemmatizer();


	public void setPrefix(String prefix) {
		this.prefix = prefix.trim();		
	}
	public HashMap<String, Integer> getPreTermFreq() {
		return preTermFreq;
	}

	public HashMap<String, Integer> getSufTermFreq() {
		return sufTermFreq;
	}

	public String getPrefix() {
		return prefix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix.trim();
	}
	public String getSuffix() {
		return suffix;
	}

	public void setMatchText(String matchText) {
		this.matchText = matchText;
	}
	public String getMatchText() {
		return matchText;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
	}

	public HashMap<String,Double> getPrefixVector() {
		return prefixVector;
	}
	public void setPrefixVector(HashMap<String, Double> prefixVector) {
		this.prefixVector = prefixVector;
	}
	public HashMap<String,Double> getSuffixVector() {
		return suffixVector;
	}
	public void setSuffixVector(HashMap<String, Double> suffixVector) {
		this.suffixVector = suffixVector;
	}
	public static void setPreDocFreq(HashMap<String,Integer> preDocFreq) {
		Match.preDocFreq = preDocFreq;
	}
	public static HashMap<String,Integer> getPreDocFreq() {
		return preDocFreq;
	}
	public static void setSufDocFreq(HashMap<String,Integer> sufDocFreq) {
		Match.sufDocFreq = sufDocFreq;
	}
	public static HashMap<String,Integer> getSufDocFreq() {
		return sufDocFreq;
	}
	public static void setDocCount(int docCount) {
		Match.docCount = docCount;
	}
	public static int getDocCount() {
		return docCount;
	}
	public void setPrefixNorm(double prefixNorm) {
		this.prefixNorm = prefixNorm;
	}
	public double getPrefixNorm() {
		return prefixNorm;
	}
	public void setSuffixNorm(double suffixNorm) {
		this.suffixNorm = suffixNorm;
	}
	public double getSuffixNorm() {
		return suffixNorm;
	}

	/** checkSynonym - checks if the synonym is valid or not
	 * 
	 * @param synonym
	 * @return
	 */
	public static boolean checkSynonym(String synonym)
	{
		String syn = stripDigits(synonym).replaceAll("^[\\p{Punct} ]+", "").replaceAll("[\\p{Punct} ]+$", "");
		Pattern p = Pattern.compile("^(https?|ftp|file)://.+$");
		Matcher m = p.matcher(synonym);
		Pattern p1 = Pattern.compile("[\\.\\w]+@[\\.\\w]+");
		Matcher m1 = p1.matcher(synonym);
		if (!syn.equals(""))
		{
			if (m.find() || m1.find())
			{
				return false;
			}
			else
				return true;
		}
		else
		{
			return false;
		}
	}

	/** stripDigits - removes digits from synonyms and returns the new synonym
	 * 
	 * @param word
	 * @return
	 */
	public static String stripDigits(String word)
	{
		return word.replaceAll("[0-9]+", "");
	}

	/** createMatch - factory method that creates each match
	 * 
	 * @param synonym
	 * @param prefix
	 * @param suffix
	 * @param matchText
	 * @return Match object
	 * @throws Exception
	 */
	public Match(Synonym synonym, String prefix,String suffix,String matchText, int contextLen) throws Exception
	{
		this.synonym = synonym;
		String lPrefix = "", lSuffix = "";

		// Lemmatization of prefix and suffix text

		String[] prefixTokens = prefix.toLowerCase().trim().split(" ");
		String[] suffixTokens = suffix.toLowerCase().trim().split(" ");

		for(int i=0;i<prefixTokens.length;i++)//String str : prefixTokens)
		{
			for(String str : lemmatizer.lemmatize(prefixTokens[i].replaceAll("\\p{Punct}+$", "").replaceAll("^\\p{Punct}+", "")))
				lPrefix += str + " ";
		}
		lPrefix = lPrefix.trim();
		for(int i=0;i<suffixTokens.length;i++)
		{
			for(String str : lemmatizer.lemmatize(suffixTokens[i].replaceAll("\\p{Punct}+$", "").replaceAll("^\\p{Punct}+", "")))
				lSuffix += str + " ";
		}

		lSuffix = lSuffix.trim();		
		List<String> pr = Arrays.asList(lPrefix.split(" "));
		int k=0;
		List<String> fpr = new ArrayList<String>();
		for(int i=pr.size()-1;i>=0;i--)
		{
			if(k==contextLen)
				break;
			fpr.add(pr.get(i));
			k++;
		}

		List<String> su = Arrays.asList(lSuffix.split(" "));
		k=0;
		List<String> fsu = new ArrayList<String>();
		for(int i=0;i<su.size();i++)
		{
			if(k==contextLen)
				break;
			fsu.add(su.get(i));
			k++;
		}

		this.prefix = lPrefix;
		this.suffix = lSuffix;
		this.matchText = matchText;

		preTermFreq = new HashMap<String,Integer>();
		sufTermFreq = new HashMap<String,Integer>();

		prefixVector = new HashMap<String,Double>();
		suffixVector = new HashMap<String,Double>();

		if (!(lPrefix.equals("") || lPrefix.equals(null)))			
		{
			for (String w : fpr)
			{
				if (stripDigits(w).length() == 0)
					continue;								
				if (!preTermFreq.containsKey(w))
				{
					preTermFreq.put(w, 1);				
					prefixVector.put(w, 0.0);
				}
				else
				{
					preTermFreq.put(w, preTermFreq.get(w)+1);				
				}
				//set doc freq
				if (!preDocFreq.containsKey(w))
					preDocFreq.put(w, 1);
				else
					preDocFreq.put(w, preDocFreq.get(w)+1);
			}

		}		
		if (!(lSuffix.equals("") || lSuffix.equals(null)))			
		{
			for (String w : fsu)
			{
				if (stripDigits(w).length() == 0)
					continue;		
				if (!sufTermFreq.containsKey(w))
				{
					sufTermFreq.put(w, 1);				
					suffixVector.put(w, 0.0);
				}
				else
				{
					sufTermFreq.put(w, sufTermFreq.get(w)+1);					
				}
				if (!sufDocFreq.containsKey(w))
					sufDocFreq.put(w, 1);
				else
					sufDocFreq.put(w, sufDocFreq.get(w)+1);
			}

		}

		docCount++;

	}




}
