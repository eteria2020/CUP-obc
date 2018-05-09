package eu.philcar.csg.OBC.data.datasources.base;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.model.SharengoResponse;
import eu.philcar.csg.OBC.db.DbRecord;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;

public abstract class BaseRetrofitDataSource {

    private CompositeDisposable mSubscriptions;

    protected void addDisposable(Disposable d){
        if(mSubscriptions == null || mSubscriptions.isDisposed()) {
            mSubscriptions = new CompositeDisposable();
        }
        mSubscriptions.add(d);
    }

    protected <T> Observable<T> extractResponse(SharengoResponse<T> response){

        return Observable.just(response.data);

    }

    protected <T> ObservableTransformer<Result<T>, T> handleRetrofitRequest() {

        return resultObservable -> resultObservable.flatMap((Function<Result<T>, Observable<T>>) r -> {

            if (r.isError()) {
                Throwable throwable = r.error();

                if (throwable instanceof IOException) {
                    if (throwable instanceof ConnectException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.NO_NETWORK,throwable));
                    } else if (throwable instanceof SocketTimeoutException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.SERVER_TIMEOUT,throwable));
                    } else if (throwable instanceof UnknownHostException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.NO_NETWORK,throwable));
                    } else if (throwable instanceof EOFException) {
                        return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.EMPTY,throwable));
                    }else{
                            return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.UNEXPECTED,throwable));
                    }
                } else {
                    return Observable.error(new ErrorResponse(ErrorResponse.ErrorType.UNEXPECTED,throwable));
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
        }
        );
    }

    protected void handleResponsePersistance(DbRecord record,BaseResponse response, DataManager manager, int callOrder){



        record.handleResponse(response, manager, callOrder);

    }

    protected void handleErorResponse(Throwable e){
        if(e instanceof ErrorResponse){
            App.onFailedApi((ErrorResponse) e);
            ErrorResponse er = (ErrorResponse) e;
            switch (er.errorType){
                case HTTP:
                    DLog.E("Retrofit Exception HTTP ",er.error);
                    break;
                case EMPTY:
                    DLog.D("Retrofit Response EMPTY ");
                    break;
                case CUSTOM:
                    DLog.E("Retrofit Exception CUSTOM ",er.error);
                    break;
                case CONVERSION:
                    DLog.E("Retrofit Exception CONVERSION ",er.error);
                    break;
                case NO_NETWORK:
                    DLog.E("Retrofit Exception NO_NETWORK ",er.error);
                    break;
                case UNEXPECTED:
                    DLog.E("Retrofit Exception UNEXPECTED ",er.error);
                    break;
                case SERVER_TIMEOUT:
                    DLog.E("Retrofit Exception SERVER_TIMEOUT ",er.error);
                    break;
                default:
                    break;
            }
        }
    }

    protected void hanldeCompletation(){
     App.setNetworkStable(true);
    }
}
