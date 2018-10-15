package eu.philcar.csg.OBC.scheduler;

import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;

/**
 * Created by Fulvio on 01/10/2018.
 */

public class SuperSocScheduler implements Runnable {

	private final LowLevelInterface obcIO;
	private final DataManager dataManager;
	private final DLog dlog;

	public SuperSocScheduler(LowLevelInterface obcIO, DataManager dataManager) {
		this.obcIO = obcIO;
		this.dataManager = dataManager;
		this.dlog = new DLog(this.getClass());
	}

	@Override
	public void run() {
		try {

			//update data from last iteration

		} catch (Exception e) {
			dlog.e("serverUpdateScheduler error", e);
		}
	}
}
