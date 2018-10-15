package eu.philcar.csg.OBC.helpers;

import java.io.File;
import java.util.Date;

/**
 * Created by Fulvio on 19/09/2017.
 */

public class Log implements Comparable<Log> {
	File file = null;
	Date date = null;
	int index;
	boolean deletingError = false;

	public Log() {

	}

	public boolean isDeletingError() {
		return deletingError;
	}

	public void setDeletingError(boolean deletingError) {
		this.deletingError = deletingError;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int compareTo(Log another) {
		if (getDate() == null || another.getDate() == null)
			return 0;
		return getDate().compareTo(another.getDate());
	}
}