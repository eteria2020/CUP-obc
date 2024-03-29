package eu.philcar.csg.OBC.controller.map.asynctask;

import android.os.AsyncTask;

public abstract class GHAsyncTask<A, B, C> extends AsyncTask<A, B, C> {
	private Throwable error;

	@SuppressWarnings("unchecked")
	protected abstract C saveDoInBackground(A... params) throws Exception;

	@SuppressWarnings("unchecked")
	protected C doInBackground(A... params) {
		try {
			return saveDoInBackground(params);
		} catch (Throwable t) {
			error = t;
			return null;
		}
	}

	public boolean hasError() {
		return error != null;
	}

	public Throwable getError() {
		return error;
	}

	public String getErrorMessage() {
		if (hasError()) {
			return error.getMessage();
		}
		return "No Error";
	}
}