package eu.philcar.csg.OBC.db;

import android.os.Parcelable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import eu.philcar.csg.OBC.helpers.DLog;
import io.reactivex.Observable;

public class DbTable<tableClass, pk>  extends BaseDaoImpl<tableClass, pk>{

	public DbTable(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		
	}

	
	public  static Class GetRecordClass() {
		return DbTable.class;
	}

	public Observable<tableClass> createOrUpdateMany(final Collection<tableClass> collection){
		return Observable.create(emitter -> {
			if (emitter.isDisposed())
				return;
			callBatchTasks((Callable<Void>) () -> {
				try {
					for (tableClass row : collection) {
						if(row instanceof CustomOp){
							((CustomOp) row).onDbWrite();
						}
						int result = createOrUpdate(row).getNumLinesChanged();
						if (result >= 0) emitter.onNext(row);
					}
					emitter.onComplete();
				} catch(Exception e) {
					DLog.E("Exception updating Customer",e);
					emitter.onError(e);
				}

				return null;


			});

		});
	}
	
}
