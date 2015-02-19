package edu.wisc.synonymdiscovery.Logging;


import java.io.Serializable;
import java.util.ArrayList;

import edu.wisc.synonymdiscovery.Utils.Match;

public class ReportObject implements Serializable {

	private static final long serialVersionUID = 1L;
	private String candidate;
	private ArrayList<Match> matches;
	private double totalSimScore;
	private int iteration;

	public String getCandidate() {
		return candidate;
	}
	public void setCandidate(String candidate) {
		this.candidate = candidate;
	}
	public ArrayList<Match> getMatches() {
		return matches;
	}
	public void setMatches(ArrayList<Match> list) {
		this.matches = list;
	}
	public double getTotalSimScore() {
		return totalSimScore;
	}
	public void setTotalSimScore(double d) {
		this.totalSimScore = d;
	}
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	public int getIteration() {
		return iteration;
	}	

}
