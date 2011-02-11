package ch.systemsx.sybit.crkwebui.client;

import ch.systemsx.sybit.crkwebui.shared.model.ApplicationSettings;
import ch.systemsx.sybit.crkwebui.shared.model.RunJobData;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>CrkWebService</code>.
 */
public interface CrkWebServiceAsync {
	public void test(String test, AsyncCallback callback);

	public void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;

	public void getResultsOfProcessing(String id, AsyncCallback callback);

	public void killJob(String id, AsyncCallback callback);

	public void getJobsForCurrentSession(AsyncCallback callback);

	public void untieJobsFromSession(AsyncCallback callback);

	public void getSettings(AsyncCallback<ApplicationSettings> callback);

	public void runJob(RunJobData runJobData, AsyncCallback callback);

	public void getInterfaceResidues(String jobId, int interfaceId,
			AsyncCallback callback);

}
