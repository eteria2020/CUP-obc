package eu.philcar.csg.OBC.helpers;

import io.reactivex.disposables.Disposable;

public class RxUtil {

	public static void dispose(Disposable disposable) {
		if (disposable != null && !disposable.isDisposed()) {
			disposable.dispose();
		}
	}

	public static boolean isRunning(Disposable disposable) {
		return disposable != null && !disposable.isDisposed();
	}

}
