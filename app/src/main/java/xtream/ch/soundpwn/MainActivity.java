package xtream.ch.soundpwn;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    // Progress Dialog
    private ProgressDialog pDialog;

    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;


    static  Button downloadButton = null;
    static Button button = null;
    static EditText profileLinkTf = null;
    static String resolveUrl = "http://api.soundcloud.com/resolve.json?";
    static String userParam = "url=";
    static String clientId = "&client_id=b45b1aa10f1ac2941910a7f0d10f8e28";
    String resString = "";
    static TextView outputText =  null;
    private static JSONObject jObject = null;
    private ListView mainListView ;
    private ArrayAdapter<String> listAdapter ;
    private static String delPrefix = "o_";
    static EditText ammountEdit = null;


    String downClientId = "?client_id=b45b1aa10f1ac2941910a7f0d10f8e28";

    static String resurllikes = "https://api-v2.soundcloud.com/users/";
    static String clientidlikes1 = "/track_likes?limit=";
    static String clientidlikes2 = "&offset=0&linked_partitioning=1&client_id=b45b1aa10f1ac2941910a7f0d10f8e28";
    static String downAmmount = "";
    static ArrayList<Track> tracksToDownload = new ArrayList<Track>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        downloadButton = (Button) findViewById(R.id.button2);
        profileLinkTf  = (EditText) findViewById(R.id.editText);
        ammountEdit  = (EditText) findViewById(R.id.editText2);
       // profileLinkTf.setText("");



        ArrayList<String> songList = new ArrayList<String>();


        button  = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        mainListView = (ListView) findViewById( R.id.listView );
        listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, songList);
        mainListView.setAdapter( listAdapter );
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                tracksToDownload.clear();
                listAdapter.clear();
                downAmmount = ammountEdit.getText().toString();
                new GetLikes().execute(resurllikes + profileLinkTf.getText() + clientidlikes1 + downAmmount + clientidlikes2);

            break;
            case R.id.button2:

                ArrayList<String> streamUrls  = new ArrayList<String>() ;
                ArrayList<String> trackTitles  = new ArrayList<String>();
                ArrayList<String> imgLinks = new ArrayList<String>();

                for(Track t : tracksToDownload){
                    streamUrls.add(t.getStreamUrl()+ downClientId);
                    trackTitles.add(t.getTitle());
                    imgLinks.add(t.getImgLink());
                }

                new DownloadFileFromURL().execute(streamUrls, trackTitles, imgLinks);
                //  new DownloadFileFromURL().execute(currentTrack.getStreamUrl() + downClientId, currentTrack.getTitle(), currentTrack.getImgLink());

            break;
        }
    }

    /**
     * Showing Dialog
     * */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setMessage("Syncronizing");
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    private class GetLikes extends AsyncTask<String, Void, ArrayList<String>> {


        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        InputStream inputStream = null;
        String jsonResult = "";
        InputStream is = null;
        JSONObject jObj = null;
        String json = "";
        ArrayList<Track> trackList = new ArrayList<Track>();


        @Override
        protected ArrayList<String> doInBackground(String... params) {



            try {
                // defaultHttpClient
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpPost = new HttpGet(params[0]);

                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                inputStream = httpEntity.getContent();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "n");
                }
                inputStream.close();
                json = sb.toString();
            } catch (Exception e) {
                Log.e("Buffer Error", "Error converting result " + e.toString());
            }

            // try parse the string to a JSON object
            try {
                jObj = new JSONObject(json);
            } catch (JSONException e) {
                Log.e("JSON Parser", "Error parsing data " + e.toString());
            }


            try {
                JSONArray collection = jObj.getJSONArray("collection");
                for (int i = 0; i < collection.length(); i++) {
                    JSONObject trackObj = collection.getJSONObject(i);
                    JSONObject finalTrackObj = trackObj.getJSONObject("track");

                    if(finalTrackObj.getBoolean("streamable")) {
                        Track theTrack = new Track();

                        theTrack.setTitle(finalTrackObj.getString("title"));
                        theTrack.setStreamUrl(finalTrackObj.getString("stream_url"));
                        theTrack.setImgLink(finalTrackObj.getString("artwork_url"));
                        trackList.add(theTrack);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            ArrayList<String> dat = new ArrayList<String>();
            dat.add(json);

            return dat;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {


            for(Track t : trackList){
                listAdapter.add(t.getTitle());
            }

            //txt.setText(result.get(0)); // txt.setText(result);
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
            tracksToDownload = trackList;
            this.progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Grabbing..All your Likes belong to us!...");
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface arg0) {
                    GetLikes.this.cancel(true);
                }
            });

        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    class DownloadFileFromURL extends AsyncTask<ArrayList<String>, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(ArrayList<String>... f_url) {

            int count;

            for(int countr = 0; countr < f_url[0].size(); countr++ ) {

                try {
                    String extr = Environment.getExternalStorageDirectory().toString();
                    File mFolder = new File(extr + "/soundpwn/");
                    if (!mFolder.exists()) {
                        mFolder.mkdir();
                    }

                    boolean fileExists =  new File(mFolder.getAbsolutePath() + "/" + f_url[1].get(countr) + ".mp3").isFile();
                    if(!fileExists) {

                        URL url = new URL(f_url[0].get(countr));
                        URLConnection conection = url.openConnection();
                        conection.connect();
                        // this will be useful so that you can show a tipical 0-100% progress bar
                        int lenghtOfFile = conection.getContentLength();

                        // download the file
                        InputStream input = new BufferedInputStream(url.openStream(), 8192);

                        // Output stream


                        OutputStream output = new FileOutputStream(mFolder.getAbsolutePath() + "/" + delPrefix + f_url[1].get(countr) + ".mp3");

                        byte data[] = new byte[1024];

                        long total = 0;

                        while ((count = input.read(data)) != -1) {
                            total += count;
                            // publishing the progress....
                            // After this onProgressUpdate will be called
                            publishProgress("" + (int) ((total * 100) / lenghtOfFile), f_url[1].get(countr));

                            // writing data to file
                            output.write(data, 0, count);
                        }

                        // flushing output
                        output.flush();

                        // closing streams
                        output.close();
                        input.close();


                        Mp3File mp3file = new Mp3File(mFolder.getAbsolutePath() + "/"+ delPrefix + f_url[1].get(countr) + ".mp3");



                        ID3v2 id3v2Tag;

                        if (mp3file.hasId3v2Tag()) {
                            id3v2Tag = mp3file.getId3v2Tag();
                        } else {
                            // mp3 does not have an ID3v2 tag, let's create one..
                            id3v2Tag = new ID3v24Tag();
                            mp3file.setId3v2Tag(id3v2Tag);
                        }


                        File mFolderImg = new File(extr + "/soundpwn/artwork/");
                        if (!mFolderImg.exists()) {
                            mFolderImg.mkdir();
                        }

                        URL ImgUrl = new URL(f_url[2].get(countr));
                        URLConnection imgConnection = ImgUrl.openConnection();
                        imgConnection.connect();
                        InputStream inputImg = new BufferedInputStream(ImgUrl.openStream(), 8192);
                        try {

                            // Output stream

                            OutputStream imgOutput = new FileOutputStream(mFolderImg.getAbsolutePath() + "/artwork" +  countr + ".jpg");

                            try {
                                byte[] buffer = new byte[1024];
                                int bytesRead = 0;
                                while ((bytesRead = inputImg.read(buffer, 0, buffer.length)) >= 0) {
                                    imgOutput.write(buffer, 0, bytesRead);
                                }

                            } finally {

                                imgOutput.close();
                            }
                        } finally {
                            inputImg.close();
                        }






                        RandomAccessFile imgfile = null;
                        try {
                            imgfile = new RandomAccessFile(mFolderImg.getAbsolutePath() + "/artwork" +  countr + ".jpg", "r");
                            byte[] bytes = new byte[(int) imgfile.length()];
                            imgfile.read(bytes);
                            id3v2Tag.setAlbumImage(bytes, "image/jpeg");
                            id3v2Tag.setAlbum("SoundPwn");
                            mp3file.setId3v2Tag(id3v2Tag);


                            mp3file.save(mFolder.getAbsolutePath() + "/" + f_url[1].get(countr) + ".mp3");

                            //Delete Old One
                            File delfile = new File(mFolder.getAbsolutePath() + "/"+ delPrefix + f_url[1].get(countr) + ".mp3");
                            delfile.delete();

                        } catch (IOException e) {
                            // do nothing
                        } finally {
                            if (imgfile != null) {
                                try {
                                    imgfile.close();
                                } catch (IOException e) {
                                    // do nothing
                                }
                            }
                        }




                    }
                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }

                count = 0;
            }


            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
            pDialog.setMessage("Downloading: " + progress[1]);
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type);
        }

    }

}
