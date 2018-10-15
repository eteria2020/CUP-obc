package eu.philcar.csg.OBC.controller.map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.helpers.DLog;

public class DFProgressing extends DialogFragment {

    public static DFProgressing newInstance(String message) {
        DFProgressing dfpro = new DFProgressing();
        dfpro.messageRes = 0;
        dfpro.messageStr = message;
        return dfpro;
    }

    public static DFProgressing newInstance(int message) {
        DFProgressing dfpro = new DFProgressing();
        dfpro.messageRes = message;
        dfpro.messageStr = "";
        return dfpro;
    }

    private int messageRes;
    private String messageStr;
    private TextView messageTV;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.df_progressing, null);

        messageTV = (TextView) view.findViewById(R.id.dfproMessageTV);

        updateMessage();

        builder.setView(view);

        setCancelable(false);

        return builder.create();
    }

    public void setMessage(int message) {
        messageRes = message;
        updateMessage();
    }

    public void setMessage(String message) {
        if (message != null) {
            messageStr = message;
        }
        updateMessage();
    }

    public void appenMessage(String message) {
        if (message != null) {
            messageStr += message;
        }
        updateMessage();
    }

    private void updateMessage() {

        if (messageTV != null) {

            if (messageRes == 0) {

                messageTV.setText(messageStr);

            } else {

                messageTV.setText(messageRes);
            }
        }
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception e) {
            DLog.E("Exception dismissing progress dialog", e);
        }
    }
}
