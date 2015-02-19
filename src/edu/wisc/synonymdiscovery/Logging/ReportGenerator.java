package edu.wisc.synonymdiscovery.Logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.wisc.synonymdiscovery.Utils.Match;


public class ReportGenerator {

	HashMap<String,String> parameters;
	ArrayList<ReportObject> candidateInfo;


	public ReportGenerator(HashMap<String,String> params, ArrayList<ReportObject> candInfo)
	{
		this.parameters = params;
		this.candidateInfo = candInfo;
	}

	public String generateHTMLReport(String fileName) throws IOException, URISyntaxException
	{
		File f = new File(fileName);	
		StringBuilder htmlStr = new StringBuilder();	
		String line = null;
		InputStream input = getClass().getResourceAsStream("ReportTemplate.html");		
		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		while ((line = br.readLine()) != null)
		{
			htmlStr.append(line);
		}		
		br.close();		
		StringBuilder params = new StringBuilder();
		int iter = Integer.parseInt(parameters.get("iter"));
		params.append("Location of Report File : " + f.getCanonicalPath() + "<br>");
		params.append("Regular Expression : " + parameters.get("regex") + "<br>");
		params.append("Datapath : " + parameters.get("datapath") + "<br>");
		params.append("Total Number of iterations : " + iter + "<br>");
		params.append("Total number of synonyms retrieved : "+candidateInfo.size() + "<br>");
		params.append("-------Options-------" + "<br>");
		params.append("Multiline : "+ parameters.get("multiline") + "<br>");
		params.append("Number of context words : " + parameters.get("contextlen") + "<br>");
		params.append("Minimum Number of characters in synonym : "+ parameters.get("synlength") + "<br>");
		params.append("Maximum Number of words in the synonym : "+ parameters.get("synWordCount") + "<br>");
		params.append("Word count if (.*) used in regular expression : "+ parameters.get("anyNumCharBound") + "<br>");

		StringBuilder cands = new StringBuilder();
		int itera = 0;
		int newIter = 0;
		for(ReportObject r : candidateInfo)
		{					
			itera = r.getIteration();			
			if(itera > newIter ) 
			{
				cands.append("<tr><td colspan=\"2\">" + "Iteration : " + r.getIteration() + "</td></tr>");
				newIter = itera;
			}
			cands.append("<tr><td>");
			cands.append(r.getCandidate());		
			cands.append("</td><td>");
			for(Match m : r.getMatches())
			{				
				String text = m.getMatchText();				
				cands.append(text+"<br>");
			}
			cands.append("</td></tr>");
		}
		String htmlString = htmlStr.toString();
		String finalHTML = htmlString.replaceAll("\\$parameters", params.toString()).replaceAll("\\$candidates", cands.toString());

		BufferedWriter bw = new BufferedWriter(new FileWriter(f,true));	
		bw.write(finalHTML);
		bw.close();
		return finalHTML;
	}

}
