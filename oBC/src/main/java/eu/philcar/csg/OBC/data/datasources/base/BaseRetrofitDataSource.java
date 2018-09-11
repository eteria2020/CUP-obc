package eu.philcar.csg.OBC.data.datasources.base;

import android.os.SystemClock;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.SystemControl;
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

    private void addDisposable(Disposable d){
        if(mSubscriptions == null || mSubscriptions.isDisposed()) {
            mSubscriptions = new CompositeDisposable();
        }
        mSubscriptions.add(d);
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
        })
            .doOnSubscribe(this::addDisposable)
            .doOnError(this::handleErorResponse)
            .doOnComplete(this::handleCompletion);
    }

    protected <T> ObservableTransformer<Result<SharengoResponse<T>>, T> handleSharengoRetrofitRequest() {

        return resultObservable -> resultObservable.flatMap((Function<Result<SharengoResponse<T>>, Observable<T>>) r -> {

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
                        Response<SharengoResponse<T>> retrofitResponse = r.response();
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

                    return Observable.just(r.response().body())
                            .concatMap(this::extractResponse);
                }
        )
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse)
                .doOnComplete(this::handleCompletion);
    }

    protected void handleResponsePersistance(DbRecord record,BaseResponse response, DataManager manager, int callOrder){



        record.handleResponse(response, manager, callOrder);

    }

    private void handleErorResponse(Throwable e){
        if(e instanceof ErrorResponse){
            App.onFailedApi((ErrorResponse) e);
            ErrorResponse er = (ErrorResponse) e;
            switch (er.errorType){
                case HTTP:
                    DLog.E("Retrofit Exception HTTP " + er.rawMessage,er.error);
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
                    DLog.E("Retrofit Exception NO_NETWORK ");
                    break;
                case UNEXPECTED:
                    DLog.E("Retrofit Exception UNEXPECTED ",er.error);
                    if(er.error instanceof SocketException){ //controllare nel caso che sea tipo emfile (too many open file)
                        DLog.D("qui farei un EMFILE REBOOT ");
                        //SystemControl.emfileException(er.error);
                    }
                    break;
                case SERVER_TIMEOUT:
                    DLog.E("Retrofit Exception SERVER_TIMEOUT ",er.error);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleCompletion(){
     App.setNetworkStable(true);
    }

    private <T> Observable<T> extractResponse(SharengoResponse<T> response){
        if(response.timestamp!=0){
            App.sharengoTime = response.timestamp;
            App.sharengoTimeFix = SystemClock.elapsedRealtime();
        }

        return Observable.just(response.data);

    }
}
