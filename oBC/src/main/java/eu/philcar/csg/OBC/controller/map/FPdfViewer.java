package eu.philcar.csg.OBC.controller.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.server.HttpsConnector;
import eu.philcar.csg.OBC.server.SendEmail;

import static java.lang.Boolean.FALSE;


/**
 * Created by Momo on 23/11/2017.
 */
public class FPdfViewer extends FBase {

    public String subDelete = "/var/www/html/dwh/"; //remote directory to eliminate from the string.
    public String URL = "http://dwh.sharengo.it/obc.php"; // web application;
    public String fileType; //
    public JSONObject Json;
    public PDFView PDFView;
    protected String directory = "/pdf/"; // local directory
    protected String PLATE = App.CarPlate;
    protected File file;
    protected boolean update = false;
    protected boolean openFile = false;
    protected boolean download = false;
    protected int fileSize = 0;
    private int mega = 1024 * 1024; // per il buffer
    private String downloadUrl;
    private TextView tv1;
    private   Button b;
    public String response;
    Handler h1;
    private Bundle bb = new Bundle();

    public static FPdfViewer newInstance(String fileTypeString) {
        return newInstance(fileTypeString, false, false);
    }

    public static FPdfViewer newInstance(String fileType, boolean openFile) {
        return newInstance(fileType, false, openFile);
    }

    public static FPdfViewer newInstance(String fileTypeString, boolean update, boolean openFile) {
        FPdfViewer FPdfViewer = new FPdfViewer();

        FPdfViewer.setFileType(fileTypeString);
        FPdfViewer.setUpdate(update);
        FPdfViewer.setOpenFile(openFile);


        return FPdfViewer;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {


        Log.i("pdf", "onCreate: pdf");
        super.onCreate(savedInstanceState);
        this.control();

        h1 = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                bb = msg.getData();
                String str = bb.getString("response");
                setTitleText(str);
            }
        };


        //  Toast.makeText(getActivity(), "No Application available to view PDF", Toast.LENGTH_SHORT).show();
    }



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setOpenFile(true);

        View view = inflater.inflate(R.layout.f_pdf, container, false);

        tv1 = (TextView)view.findViewById(R.id.resp);
        tv1.setVisibility(View.INVISIBLE);

        ((ImageButton) view.findViewById(R.id.fmenBackIB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ABase) getActivity()).popFragment();
            }

        });
        PDFView = (PDFView) view.findViewById(R.id.pdfView);
        setPDFView(PDFView);
        if (file != null && file.exists()) {
            openPdf(file, PDFView);
        }
        b = (Button) view.findViewById(R.id.sendDocument);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getEmail();
            }

        });
        return view;
    }
    public void setTitleText(String resp) {
        this.response = resp;
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                b.setVisibility(View.INVISIBLE);
                tv1.setText(response);
                tv1.setVisibility(View.VISIBLE);
            }
        });

    }
    public void getEmail(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Inserisci una mail valida");

        // Add edit text for password input
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS );
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email =  input.getText().toString();
                DLog.I("Email inserita : " + ", "+email);
                if(isEmailValid(email)){

                        try {
                            SendEmail sendEmail= new SendEmail();
                            sendEmail.setEmail(email);
                            sendEmail.setHandler(h1);
                            HttpsConnector httpConnector = new HttpsConnector(getActivity());
                            httpConnector.Execute(sendEmail);


                        } catch (Exception e) {
                            DLog.E("SendEmail", e);

                        }
                    }

                else {
                    Toast.makeText(getActivity(), "EMAIL NON VALIDA", Toast.LENGTH_LONG).show();
                    return;
                }
                //  sendemail.execute();

            }
        });
        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public boolean isEmailValid(String email)
    {
        String regExpn =
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        +"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        +"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        +"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if(matcher.matches())
            return true;
        else
            return false;
    }
    protected void openPdf(File pdf, PDFView pdfView) {


        if (pdf != null && pdf.exists()) {
            Uri path = Uri.fromFile(pdf);
            Toast.makeText(getActivity(), "Caricamento", Toast.LENGTH_SHORT).show();
            pdfView.fromUri(path).load();
        } else {

            Toast.makeText(getActivity(), "Problemi durante l'apertura del file", Toast.LENGTH_SHORT).show();
            getFragmentManager().popBackStack();
        }

    }

    /*
    control if the pdf file is in local and check the time
     */
    public File[] checkPdf() throws ParseException {
        File folder = new File(Environment.getExternalStorageDirectory() + directory);
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().contains(getFileType()) && pathname.isFile() && pathname.length() > 0;
                }
            });


            if (listOfFiles.length > 0) {
                return listOfFiles;

            }
        } else {
            folder.mkdir();
            return null;
        }
        return null;
    }

    private boolean needUpdate(File[] listOfFiles) {
        String name = listOfFiles[0].getName().replaceAll("[^?0-9]+", "");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String TodayDate = sdf.format(new Date());
        int x = Integer.parseInt(TodayDate);
        int y = Integer.parseInt(name);
        if (x > y) {

            return true;
        }


        return false;
    }

    public void fileType(String fileType) {
        this.fileType = fileType;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }


    public void setJson(String json) throws JSONException {

        Json = new JSONObject(json);
    }

    public void setPLATE(String PLATE) {
        this.PLATE = PLATE;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void setOpenFile(boolean openFile) {
        this.openFile = openFile;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public void setPDFView(com.github.barteksc.pdfviewer.PDFView PDFView) {
        this.PDFView = PDFView;
    }

    public void control() {

        download = false;
        File[] pdf = new File[0];
        try {
            pdf = checkPdf();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (pdf != null) {
            if (pdf.length > 0) {
                setFile(pdf[0]);
                if (needUpdate(pdf)) {
                    setUpdate(true);
                    if (App.hasNetworkConnection()) {
                        Toast.makeText(getActivity(), "Scaricamento file aggiornato...", Toast.LENGTH_LONG).show();
                        new DownloadFile().execute();
                    } else {
                        Toast.makeText(getActivity(), "No Application available to view PDF", Toast.LENGTH_SHORT).show();
                        openPdf(file, PDFView);
                    }

                } else {
                    //    openPdf(pdf[0],getView());
                    setUpdate(false);
                }
            } else if(App.hasNetworkConnection()) {
                setDownload(true);
                Toast.makeText(getActivity(), "Scaricamento file, Attendere...", Toast.LENGTH_LONG).show();
                new DownloadFile().execute();
            }else
            {
                Toast.makeText(getActivity(), "Nessuna connessione internet", Toast.LENGTH_SHORT).show();
            }

        } else if (App.hasNetworkConnection()) {
            Toast.makeText(getActivity(), "Scaricamento file, Attendere...", Toast.LENGTH_LONG).show();
            new DownloadFile().execute();
        } else {
            Toast.makeText(getActivity(), "Nessuna connessione internet", Toast.LENGTH_SHORT).show();
            getFragmentManager().popBackStack();
        }
    }

    public void control(String fileType1) {
        setFileType(fileType1);
        control();
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    private class DownloadFile extends AsyncTask<String, String, String> {
       private boolean downloaded = false;
        String txtJson;
        protected void onPreExecute() {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Log.i("wait", "run: okk");
                }
            }, 10000);

        }

        @Override
        protected String doInBackground(String... voids) {


            HttpClient httpclient = new DefaultHttpClient(); // Create HTTP Client
            HttpGet httpget = new HttpGet(getURL() + "?PLATE=" + PLATE + "&FILE=" + fileType); // Set the action you want to do
            HttpResponse response = null; // Executeit
            try {
                response = httpclient.execute(httpget);
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream is = null; // Create an InputStream with the response
            try {
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
            } catch (IOException e) {
                Log.e("FPDF", "Download pdf: ",e );
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null) // Read line by line
                    sb.append(line + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            txtJson = sb.toString(); // Result is here

            try {
                setJson(txtJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if(Json.getString("filesize") == "false")
                    return null;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String x = Json.getString("filesize");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String url = null;
            try {

                url = Json.getString(fileType).replace(subDelete, "");
                if (url != null)
                {
                    setDownloadUrl(url);
                }else
                {
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (DownloadPdf()) {

                    return null;

                }
            } catch (JSONException e) {
                Toast.makeText(getActivity(), "problemi durante lo scaricamento del file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            return null;

        }



        @Override
        protected void onPostExecute(String result) {


                try {
                    setJson(txtJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (openFile)
                    try{
                    openPdf(file, PDFView);}
                    catch (Exception e){

                    }

        }

        protected boolean DownloadPdf() throws JSONException {

            String fileUrl = Json.getString(fileType).replace(subDelete, "");
            if (fileUrl == null)
                return FALSE;
            if (fileUrl == "http://dwh.sharengo.it/")
                return FALSE;
            if (Json.getString("filesize") == null || Json.getString("filesize") == "false")
                return FALSE;
       //     StringBuilder  temp = new StringBuilder();
         //   temp.append();
         //   temp.reverse();
            String tmp[] =fileUrl.replace(PLATE,"").replace(".pdf","").split("--"); //replaceAll("[^?0-9]+", "")
            String temp[] = tmp[1].split("_");
            String pdfname = fileType + "-" + temp[2]+temp[1]+temp[0] + ".pdf";

            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File folder = new File(extStorageDirectory, directory);
            folder.mkdir();
            Log.i("makdir", "doInBackground:creare pdf directory ");
            File pdfFile = new File(folder, pdfname);

            Log.i("makdir", "doInBackground:filedownloader ");

            try {

                URL url = new URL(fileUrl.replace(" ", "%20"));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                pdfFile.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                int totalSize = urlConnection.getContentLength();

                byte[] buffer = new byte[mega];
                int bufferLength = 0;
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, bufferLength);
                }
                fileOutputStream.close();
                setFile(pdfFile);
                setDownload(false);
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;

        }


    }
}