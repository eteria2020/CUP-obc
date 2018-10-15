package eu.philcar.csg.OBC.db;

import eu.philcar.csg.OBC.data.model.ServerResponse;
import eu.philcar.csg.OBC.service.DataManager;

public abstract class DbRecord<E extends ServerResponse> {
    /**
     * @param e         response that have to be elaborated
     * @param manager   dependency that help to update DB
     * @param callOrder identifier to distinguish rifferent response of the same call, Trip have different response to handle
     */
    public abstract void handleResponse(E e, DataManager manager, int callOrder);

}
