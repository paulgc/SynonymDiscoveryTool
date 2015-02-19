package edu.wisc.synonymdiscovery.Beans;

import java.util.ArrayList;

import edu.wisc.synonymdiscovery.Utils.Match;

public class Candidate {

	private Synonym synonym;
	public Synonym getSynonym() {
		return synonym;
	}

	public ArrayList<Match> getMatches() {
		return matches;
	}

	private ArrayList<Match> matches;

	public Candidate(Synonym synonym, ArrayList<Match> matches) {
		this.synonym = synonym;
		this.matches = matches;
	}



}
