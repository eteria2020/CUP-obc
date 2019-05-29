package eu.philcar.csg.OBC.task;

import android.os.AsyncTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.FileTools;
import eu.philcar.csg.OBC.helpers.Log;
import eu.philcar.csg.OBC.helpers.Logs;

/**
 * Created by Fulvio on 21/09/2017.
 */

public class OldLogCleanup extends AsyncTask<Void, Void, Boolean> {
	private DLog dlog = new DLog(this.getClass());
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH", Locale.getDefault());
	private Logs logs = new Logs();

	@Override
	protected Boolean doInBackground(Void... params) {

		try {
			dlog.i("CheckOldLogSize ~ Start cleaning log");
			File logRoot = new File(App.getAppLogPath());
			long logsize = FileTools.getFileSize(logRoot);

			dlog.i("CheckOldLogSize ~ Log directory weight: " + logsize);
			if (logsize > 2147483648L) {
				dlog.i("CheckOldLogSize ~ Folder too big deleting last 30 record");

				retreiveAllLogs(logRoot);
				logs.removeFromBottom(30);
				return true;
			} else {

				retreiveAllLogs(logRoot);

				dlog.i("CheckOldLogSize ~ Let's delete some trash");

				Date date = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.add(Calendar.MONTH, -9);
				date = calendar.getTime();
				if (date.getTime() > 1474466729L) {

					logs.removeFromBottom(date);
				}
				return true;
			}
		} catch (Exception e) {
			dlog.e("CheckOldLogSize ~ An Exception occurs", e);
			return false;
		} finally {
			logs = null;
		}

	}

	private void retreiveAllLogs(File logRoot) {
		try {
			if (!logRoot.isDirectory())
				return;
			File logsFile[] = logRoot.listFiles();
			if (logsFile.length == 0) {
				dlog.i("CheckOldLogSize ~ Root folder Empty");
				if (!logRoot.getName().equalsIgnoreCase("log")) {
					logRoot.delete();
					dlog.i("CheckOldLogSize ~ deleting empty folder" + logRoot.getPath());
				}
				return;
			}
			for (File logFile : logsFile) {
				if (logFile != null) {
					if (logFile.isDirectory()) {
						retreiveAllLogs(logFile);
					} else {
						String name = logFile.getName();
						try {
							String fileType = name.substring(name.lastIndexOf(".") + 1);
							if (fileType.equalsIgnoreCase("tmp")) {
								boolean succes = logFile.delete();
								dlog.i("CheckOldLogSize ~ Found .tmp file, deleting" + succes);
								continue;
							}
							Log log = new Log();
							String date = name.substring(name.indexOf('.') + 1, name.indexOf('.', name.indexOf('.') + 1));
							log.setDate(formatter.parse(date));
							log.setFile(logFile);
							log.setIndex(0);
							logs.addLog(log);
							log = null;
						} catch (Exception e) {
							dlog.w("CheckOldLogSize ~ Found a file that should not be here, what to do?" + logFile.getName());
							String fileType = name.substring(name.lastIndexOf(".") + 1);
							if (fileType.equalsIgnoreCase("tmp")) {
								boolean succes = logFile.delete();

								dlog.i("CheckOldLogSize ~ Found .tmp file, deleting" + succes);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			dlog.e("CheckOldLogSize ~ Exception while retreiving oldLog", e);
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
		dlog.i("LogCleanup ~ finished with result: " + aBoolean);
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
