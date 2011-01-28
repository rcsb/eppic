package ch.systemsx.sybit.crkwebui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>CrkWebService</code>.
 */
public interface CrkWebServiceAsync 
{
	public void test(String test, AsyncCallback callback);
	
	public void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;

	public void checkIfDataProcessed(String id, AsyncCallback callback);
	
	public void getStatusData(String id, AsyncCallback  callback);
	
	public void getResultData(String id, AsyncCallback callback);

	public void killJob(String id, AsyncCallback callback);

	public void getJobsForCurrentSession(AsyncCallback callback);

	public void untieJobsFromSession(AsyncCallback callback);

}
