package edu.wisc.synonymdiscovery.Servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import edu.wisc.synonymdiscovery.Beans.Options;
import edu.wisc.synonymdiscovery.Beans.Synonym;
import edu.wisc.synonymdiscovery.Feedback.FeedbackThread;
import edu.wisc.synonymdiscovery.RegExp.ProcessThread;
import edu.wisc.synonymdiscovery.Utils.CandidateSynonymScoringInfo;



/**
 * Servlet implementation class RegexServlet
 */
public class RegexServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private BlockingQueue<Object> sharedQueue = new LinkedBlockingDeque<Object>(); 
	private BlockingQueue<Object> candidateQueue = new LinkedBlockingDeque<Object>();
	private HashMap<Synonym,CandidateSynonymScoringInfo> candidateScoringInfo;
	private CandidateSynonymScoringInfo meanPrototypeScoringInfo;
	private ArrayList<Synonym> candSynonyms;
	Thread processThread;
	ProcessThread p;
	FeedbackThread fb;
	Thread feedbackThread;
	private int iter = 0;


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RegexServlet() {
		super();
		// TODO Auto-generated constructor stub        
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		// initialize the thread
		p = new ProcessThread(sharedQueue,candidateQueue);
		processThread = new Thread(p);					
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */

	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		// TODO Auto-generated method stub
		//we need to check the request state and then kick off the thread or send a response
		HashMap<String,String> map = new HashMap<String,String>();		
		PrintWriter out = null; 
		try
		{		
			HttpSession session = request.getSession();					
			out = response.getWriter();
			response.setContentType("application/json");
			if(request.getParameter("action").equals("process_match"))
			{
				if(processThread.getState() == Thread.State.NEW)
				{
					try
					{			
						sharedQueue.clear();
						candidateQueue.clear();
						p.setSid(session.getId());
						p.setRegex(request.getParameter("regex"));
						p.setDataPath(request.getParameter("datapath"));
						
						Options options = new Options();
						options.setMultiLineFlag(Boolean.parseBoolean(request.getParameter("multiline")));
						options.setMinSynLength(Integer.parseInt(request.getParameter("synlength")));
						options.setNumContextWords(Integer.parseInt(request.getParameter("contextlen")));
						options.setNumSynWords(Integer.parseInt(request.getParameter("synWordCount")));

						String numCharBound = request.getParameter("anyNumCharBound");
						if(numCharBound.equals("None"))
							options.setAnyNumCharBound(0);
						else
							options.setAnyNumCharBound(Integer.parseInt(numCharBound));

						p.setOptions(options);
						p.setLoggingEnabled(Boolean.parseBoolean(request.getParameter("log")));
						p.setGuid(request.getParameter("guid"));
						processThread.start();
						//set the input params				
						map.put("message", "Processing Started.."+"\n"+"GUID :"+request.getParameter("guid")+"\n");					
						map.put("complete", "false");
					}
					catch(Exception e)
					{
						map.put("message", e.getMessage() + '\n' + Arrays.toString(e.getStackTrace()).replace(",", "\n") + '\n');
						map.put("complete", "fail");
					}					
					out.println(new JSONObject(map));			
				}
				else if(processThread.getState() == Thread.State.TERMINATED)
				{
					String data = "";		
					boolean exceptionOccurred = false;
					try 
					{
						while(!sharedQueue.isEmpty())
						{            		
							Object obj = sharedQueue.poll(5,TimeUnit.SECONDS);
							if(obj instanceof Exception)
							{            			 
								data += ((Exception)obj).getMessage() + '\n' + Arrays.toString(((Exception) obj).getStackTrace()).replace(",", "\n") + '\n';
								exceptionOccurred = true;
							}
							else
								data += obj.toString() + '\n';
						}
						if(!exceptionOccurred)
						{
							map.put("complete", "true");
							if(!candidateQueue.isEmpty())
							{
								Object o = candidateQueue.poll();
								if(o instanceof HashMap<?,?>)
								{
									candidateScoringInfo = (HashMap<Synonym,CandidateSynonymScoringInfo>)o;	            				
								}
								Object p = candidateQueue.poll();
								if(p instanceof CandidateSynonymScoringInfo)
								{
									meanPrototypeScoringInfo = (CandidateSynonymScoringInfo)p;	            				
								}
								Object ob = candidateQueue.poll();
								if(ob instanceof ArrayList)
								{
									candSynonyms = (ArrayList<Synonym>)ob;	            				
								}
							}
						}
						else
							map.put("complete", "fail");
					} 
					catch (Exception ex) 
					{
						data += ex.getMessage() + '\n' + Arrays.toString(ex.getStackTrace()).replace(",", "\n") + '\n';
						map.put("complete", "fail");
					}				
					map.put("message", data);	
					//initialize the thread object
					processThread = new Thread(p);
					out.println(new JSONObject(map));						
				}
				else
				{
					String data = "";			
					boolean exceptionOccurred = false;
					try 
					{
						while(!sharedQueue.isEmpty())
						{            		
							Object obj = sharedQueue.poll(5,TimeUnit.SECONDS);
							if(obj instanceof Exception)
							{            			 
								data += ((Exception)obj).getMessage() + '\n' + Arrays.toString(((Exception) obj).getStackTrace()).replace(",", "\n") + '\n';
								exceptionOccurred = true;
							}
							else
							{
								data += obj.toString() + '\n';
							}
						}
						if(!exceptionOccurred)
							map.put("complete", "false");
						else
							map.put("complete", "fail");
					} 
					catch (Exception ex) 
					{
						data += ex.getMessage() + '\n' + Arrays.toString(ex.getStackTrace()).replace(",", "\n") + '\n';
						map.put("complete", "fail");
					}	
					map.put("message", data);			
					out.println(new JSONObject(map));					
				}		
			}
			else if(request.getParameter("action").equals("feedback"))
			{								
				int newIter = Integer.parseInt(request.getParameter("iter"));		
				String reqState = request.getParameter("req");
				if(newIter == 1 && reqState.equals("NEW"))
					iter = 0;
				if(newIter > iter)
				{			
					sharedQueue.clear();				
					candidateQueue.clear();
					fb = new FeedbackThread(sharedQueue,candidateQueue);	
					feedbackThread = new Thread(fb);				
					iter = newIter;	
					HttpSession feedbacksession = request.getSession();
					fb.setSid(feedbacksession.getId());								
					fb.setCandidateScoringInfo(candidateScoringInfo);
					fb.setMeanPrototypeScoringInfo(meanPrototypeScoringInfo);
					fb.setCandSynonyms(candSynonyms);				
					fb.setFeedback(request.getParameter("feedback"));
					fb.setIteration(newIter);				
					fb.setLoggingEnabled(Boolean.parseBoolean(request.getParameter("log")));	
					fb.setGuid(request.getParameter("guid"));
				}
				if(feedbackThread.getState() == Thread.State.NEW)
				{
					try
					{					
						feedbackThread.start();
						//set the input params				
						map.put("message", "Iteration : "+iter+"\nFeedback Processing Started.."+"\n");					
						map.put("complete", "false");
					}
					catch(Exception e)
					{
						map.put("message", e.getMessage() + '\n' + Arrays.toString(e.getStackTrace()).replace(",", "\n"));					
						map.put("complete", "fail");
					}				
					map.put("iter", Integer.toString(iter));
					out.println(new JSONObject(map));				

				}
				else if(feedbackThread.getState() == Thread.State.TERMINATED)
				{				
					String data = "";		
					boolean exceptionOccurred = false;
					try 
					{
						while(!sharedQueue.isEmpty())
						{            		
							Object obj = sharedQueue.poll(5,TimeUnit.SECONDS);
							if(obj instanceof Exception)
							{            			 
								data += ((Exception) obj).getMessage() + '\n' + Arrays.toString(((Exception) obj).getStackTrace()).replace(",", "\n") + '\n';
								exceptionOccurred = true;
							}
							else
								data += obj.toString() + '\n';
						}
						if(!exceptionOccurred)
						{
							map.put("complete", "true");
							map.put("iter", Integer.toString(iter+1));
							if(!candidateQueue.isEmpty())
							{
								candidateScoringInfo = (HashMap<Synonym,CandidateSynonymScoringInfo>)candidateQueue.poll();
								meanPrototypeScoringInfo = (CandidateSynonymScoringInfo)candidateQueue.poll();
								candSynonyms = (ArrayList<Synonym>)candidateQueue.poll();
							}
						}
						else
						{
							map.put("iter", Integer.toString(iter));
							map.put("complete", "fail");
						}
					} 
					catch (Exception ex) 
					{
						data += ex.getMessage() + Arrays.toString(ex.getStackTrace()).replace(",", "\n") + '\n';
						map.put("iter", Integer.toString(iter));
						map.put("complete", "fail");
					}				
					map.put("message", data);			
					out.println(new JSONObject(map));		    		   
				}
				else
				{
					String data = "";			
					boolean exceptionOccurred = false;
					try 
					{
						while(!sharedQueue.isEmpty())
						{            		
							Object obj = sharedQueue.poll(5,TimeUnit.SECONDS);
							if(obj instanceof Exception)
							{            			 
								data += ((Exception) obj).getMessage() + '\n' + Arrays.toString(((Exception) obj).getStackTrace()).replace(",", "\n") + '\n';
								exceptionOccurred = true;
							}
							else
								data += obj.toString() + '\n';
						}
						if(!exceptionOccurred)
						{
							map.put("complete", "false");
							map.put("iter", Integer.toString(iter));
						}
						else
						{
							map.put("complete", "fail");
							map.put("iter", Integer.toString(iter));
						}
					} 
					catch (Exception ex) 
					{
						data += ex.getMessage() + Arrays.toString(ex.getStackTrace()).replace(",", "\n") + '\n';
						map.put("complete", "fail");
						map.put("iter", Integer.toString(iter));
					}	
					map.put("message", data);			
					out.println(new JSONObject(map));		 
				}		
			}

		}
		catch(Exception e)
		{
			map.put("message", e.getMessage() + '\n' + Arrays.toString(e.getStackTrace()).replace(",", "\n") + '\n');
			map.put("complete", "fail");
			out.println(new JSONObject(map));
		}
		finally
		{
			out.close();
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	public void destroy()
	{		
		processThread = null;
		feedbackThread = null;
	}

}
