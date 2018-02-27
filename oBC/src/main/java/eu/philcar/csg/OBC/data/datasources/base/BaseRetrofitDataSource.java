package eu.philcar.csg.OBC.data.datasources.base;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.db.DbRecord;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;

public abstract class BaseRetrofitDataSource {

    protected <T> ObservableTransformer<Result<T>, T> handleRetrofitRequest() {

        return resultObservable -> resultObservable.flatMap((Function<Result<T>, Observable<T>>) r -> {

            if (r.isError()) {
                Throwable throwable = r.error();

                if (throwable instanceof IOException) {
                    if (throwable instanceof ConnectException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.NO_NETWORK));
                    } else if (throwable instanceof SocketTimeoutException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.SERVER_TIMEOUT));
                    } else if (throwable instanceof UnknownHostException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.NO_NETWORK));
                    } else {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.UNEXPECTED));
                    }
                } else {
                    return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.UNEXPECTED));
                }
            }
            else {
                Response<T> retrofitResponse = r.response();
                if (!retrofitResponse.isSuccessful()) {
                    int code = retrofitResponse.code();
                    String message = "";
                    try {
                        message = retrofitResponse.errorBody().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ErrorResponse errorResponse = new ErrorResponse(ErrorResponse.ErrorType.HTTP);
                    errorResponse.httpStatus = code;
                    errorResponse.rawMessage = message;
                    return Observable.error(errorResponse);
                }
            }

            return Observable.just(r.response().body());
        });
    }

    protected void handleResponsePersistance(DbRecord record,BaseResponse response, DataManager manager, int callOrder){



        record.handleResponse(response, manager, callOrder);

    }
}
