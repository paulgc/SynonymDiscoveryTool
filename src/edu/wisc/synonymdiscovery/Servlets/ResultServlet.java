package edu.wisc.synonymdiscovery.Servlets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.*;

/**
 * Servlet implementation class ResultServlet
 */
public class ResultServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ResultServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		// TODO Auto-generated method stub		
		response.setContentType("application/json");	
		PrintWriter out = null;
		BufferedReader br = null;
		try
		{
			out = response.getWriter();		
			br = new BufferedReader(new FileReader("candidates.txt"));
			String line = "", data = "";
			while((line = br.readLine()) != null)
			{
				data += line;
			}
			JSONObject obj = (JSONObject) JSONValue.parse(data);
			out.println(obj);			
		}				
		catch(IOException e)
		{
			out.println(Arrays.toString(e.getStackTrace()).replace(",", "\n"));
		}
		finally
		{
			try {
				br.close();
			} catch (IOException e) {
				out.println(Arrays.toString(e.getStackTrace()).replace(",", "\n"));			
			}
			out.close();
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
