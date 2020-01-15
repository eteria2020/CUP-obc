package eu.philcar.csg.OBC.task;

import android.os.AsyncTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.FileTools;
import eu.philcar.csg.OBC.helpers.Log;
import eu.philcar.csg.OBC.helpers.Logs;

/**
 * Created by Fulvio on 20/09/2017.
 */

public class LogCleanup extends AsyncTask<Void, Void, Boolean> {
	private DLog dlog = new DLog(this.getClass());
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH", Locale.getDefault());
	private Logs logs = null;

	@Override
	protected Boolean doInBackground(Void... params) {

		try {
			dlog.d("LogCleanup.doInBackground();Start cleaning log");
			long logsize = FileTools.getFileSize(new File(App.getAppLogPath()));

			dlog.d("LogCleanup.doInBackground();Log directory weight: " + logsize);
			if (logsize < 2147483648L) {
				dlog.d("LogCleanup.doInBackground();No need to operate");
				return false;
			}

			File logRoot = new File(App.getAppLogPath());
			File[] logsFile = logRoot.listFiles();
			logs = new Logs();
			if (logsFile.length == 0) {
				dlog.d("CheckLogSize ~ Root folder Empty");
				return false;
			}
			for (File logFile : logsFile) {
				if (logFile != null && !logFile.isDirectory()) {
					String name = logFile.getName();
					try {
						String fileType = name.substring(name.lastIndexOf(".") + 1);
						if (fileType.equalsIgnoreCase("tmp")) {
							boolean succes = logFile.delete();
							dlog.d("LogCleanup.doInBackground();Found .tmp file, deleting" + succes);
							continue;
						}
						Log log = new Log();
						String date = name.substring(name.indexOf('_') + 1, name.indexOf('.'));
						String id = name.substring(name.indexOf('.') + 1, name.indexOf('.', name.indexOf('.') + 1));
						log.setDate(formatter.parse(date));
						log.setFile(logFile);
						log.setIndex(Integer.parseInt(id));
						logs.addLog(log);
//						log = null;
					} catch (Exception e) {
						dlog.e("LogCleanup.doInBackground();Found a file that should not be here, what to do?", e);
						String fileType = name.substring(name.lastIndexOf(".") + 1);
						if (fileType.equalsIgnoreCase("tmp")) {
							boolean succes = logFile.delete();

							dlog.d("LogCleanup.doInBackground();Found .tmp file, deleting" + succes);
						}
					}
				}
			}

			logs.removeFromBottom(30);
			dlog.d("LogCleanup.doInBackground();Let's delete some trash");
			return true;
		} catch (Exception e) {
			dlog.e("LogCleanup.doInBackground();An Exception occurs", e);
			return false;
		} finally {
			logs = null;
		}

	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Boolean aBoolean) {
		super.onPostExecute(aBoolean);
		logs = null;
		dlog.i("LogCleanup.onPostExecute();finished with result: " + aBoolean);
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled(Boolean aBoolean) {
		super.onCancelled(aBoolean);
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
	}

}
