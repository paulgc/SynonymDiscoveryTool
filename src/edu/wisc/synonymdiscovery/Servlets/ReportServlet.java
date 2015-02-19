package edu.wisc.synonymdiscovery.Servlets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.wisc.synonymdiscovery.Logging.ReportGenerator;
import edu.wisc.synonymdiscovery.Logging.ReportObject;


/**
 * Servlet implementation class ReportServlet
 */
public class ReportServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ReportServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		PrintWriter out = response.getWriter();
		try {			
			// get the session id
			response.setContentType("text/html");
			//HttpSession session = request.getSession();			
			ArrayList<ReportObject> reportCandidates = new ArrayList<ReportObject>();
			HashMap<String,String> parameters = new HashMap<String,String>();
			String guid = request.getParameter("guid");
			parameters.put("regex", request.getParameter("regex"));
			parameters.put("datapath", request.getParameter("datapath"));
			parameters.put("multiline", request.getParameter("multiline"));
			parameters.put("contextlen", request.getParameter("contextlen"));
			parameters.put("synlength", request.getParameter("synlength"));
			parameters.put("synWordCount", request.getParameter("synWordCount"));
			parameters.put("anyNumCharBound", request.getParameter("anyNumCharBound"));
			parameters.put("iter", request.getParameter("iter"));

			FileInputStream fileIn = new FileInputStream("RegexTemp/cand_"+guid+".ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);        	       
			reportCandidates = (ArrayList<ReportObject>) in.readObject();		
			in.close();
			fileIn.close();	        	   

			String reportName = "RegexTemp/report_"+guid+".html";
			ReportGenerator rp = new ReportGenerator(parameters,reportCandidates);						
			String htmlReport = rp.generateHTMLReport(reportName);
			request.getSession().invalidate();
			out.println(htmlReport);

		}
		catch (ClassNotFoundException e) 
		{		
			out.println(e.getMessage() + '\n' + Arrays.toString(e.getStackTrace()).replace(",", "\n"));
		} 
		catch (URISyntaxException e) {

			out.println(e.getMessage() + '\n' + Arrays.toString(e.getStackTrace()).replace(",", "\n"));
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
