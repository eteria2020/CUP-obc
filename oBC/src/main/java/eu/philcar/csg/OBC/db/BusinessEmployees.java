package eu.philcar.csg.OBC.db;

import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.Collection;

import eu.philcar.csg.OBC.helpers.DLog;
import io.reactivex.Observable;

public class BusinessEmployees extends DbTable<BusinessEmployee, Integer> {

	public BusinessEmployees(ConnectionSource connectionSource, Class dataClass) throws SQLException {
		super(connectionSource, dataClass);
	}

	public void deleteAll() {
		try {
			this.executeRawNoArgs("DELETE FROM business_employee");

		} catch (SQLException e) {
			DLog.E("deleteAll failed:", e);

		}
	}

	public static Class GetRecordClass() {
		return BusinessEmployee.class;
	}

	public BusinessEmployee getBusinessEmployee(int employeeId) {
		try {
			return queryForId(employeeId);
		} catch (SQLException e) {
			DLog.E("getBusinessEmployee fail:", e);
		}
		return null;
	}

	public Observable<BusinessEmployee> setEmployee(final Collection<BusinessEmployee> customers) {
		return Observable.create(emitter -> {
			if (emitter.isDisposed()) return;

			try {
				for (BusinessEmployee customer : customers) {
					int result = createOrUpdate(customer).getNumLinesChanged();
					if (result >= 0) emitter.onNext(customer);
				}
				emitter.onComplete();
			} catch (Exception e) {
				DLog.E("Exception setting Employee", e);
				emitter.onError(e);
			}
		});
	}
}
