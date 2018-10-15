package eu.philcar.csg.OBC.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Fulvio on 19/09/2017.
 */

public class Logs {
	private ArrayList<Log> logs;
	private int validLogIndex = 0;
	private DLog dlog = new DLog(this.getClass());
	private boolean sorted = false;

	public Logs() {
		logs = new ArrayList<Log>();
	}

	public void addLog(Log log) {
		try {
			logs.add(log);
			sorted = false;
		} catch (Exception e) {
			throw e;
		}
	}

	public void addLog(Log log, int index) {
		try {
			logs.add(index, log);
			sorted = false;
		} catch (Exception e) {
			throw e;
		}
	}

	public void sort() {
		try {
			Collections.sort(logs, new Comparator<Log>() {
				@Override
				public int compare(Log lhs, Log rhs) {
					if (lhs.getDate() == null || rhs.getDate() == null)
						return 0;
					return lhs.getDate().compareTo(rhs.getDate());
				}
			});
			sorted = true;
		} catch (Exception e) {
			dlog.e("Exceprion while sorting");
			sorted = false;
		}
	}

	public void removeFromBottom(int quantity) {
		int i = 0;
		if (!sorted)
			sort();
		while (i++ < quantity && logs.size() > 0) {
			removeLog(getValidLogIndex());
		}
	}

	public void removeFromBottom(Date time) {
		if (!sorted)
			sort();
		while (logs.get(getValidLogIndex()).getDate().before(time) && logs.size() > 0) {
			removeLog(getValidLogIndex());
		}
	}

	private void removeLog(int index) {
		if (index >= 0 && index < logs.size() - 1) {
			String filename = logs.get(index).getFile().getName();
			boolean success = logs.get(index).getFile().delete();
			dlog.d("removeLog " + filename + " " + index + " " + success);
			if (success)
				logs.remove(index);
			else {
				logs.get(index).setDeletingError(true);
				updateValidLogIndex();
			}
		}
	}

	private int getValidLogIndex() {
		return validLogIndex;
	}

	private void updateValidLogIndex() {
		while (validLogIndex < logs.size() && logs.get(validLogIndex).isDeletingError()) {
			validLogIndex++;
			if (validLogIndex >= logs.size()) {
				validLogIndex = -1;
				return;
			}
		}
	}

}