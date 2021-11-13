package android.bignerdranch.photogallery;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FlickrFetchr {
    //class responsible for establishing connection to Flickr, called by AsyncTask class FetchItems
    // so you guarantee the main thread doesn't execute any networking
    private static final String API_KEY = /*insert your api here*/;
    private String TAG="PhotoGalleryFragment";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/").buildUpon()
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build();

    //turns JSON response into byte array
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            Log.e(TAG, "CONTENT OF INPUT STREAM");
            longInfo(in.toString());

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " with: " + urlSpec);
            }
            copyIntoOut(in,out);
//            byte[] buffer = new byte[1024];
//            int bytesRead = in.read(buffer);
//
//            while (bytesRead != -1) {
//                out.write(buffer,0,bytesRead);
//                bytesRead=in.read(buffer);
//            }
//            Log.e(TAG, "CONTENT OF INPUT STREAM #2");
//            longInfo(in.toString());
//            Log.e(TAG, "CONTENT OF OUTPUT STREAM");
//            longInfo(out.toString());
//            Log.e(TAG, "CONTENT OF OUTPUT STREAM IS OVER");
            out.close();
            return out.toByteArray();

        } finally {
            connection.disconnect();
        }
    }

    //turns JSON byte array into string
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

//makes from the JSON string a JSON object to use easier through a built in class
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items= new ArrayList<>();

        try {
            String jsonString = getUrlString(url);
            longInfo(jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);
        }
        catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }
        catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i =0;i<photoJsonArray.length() ; i++){
            JSONObject photoJsonObject= photoJsonArray.getJSONObject(i);
            GalleryItem item= new GalleryItem();
            item.setCaption(photoJsonObject.getString("title"));
            item.setId(photoJsonObject.getString("id"));

            if(!photoJsonObject.has("url_s")){
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);

        }
    }

    private String buildUrl(String method,String Query){
        Uri.Builder uriBuilder= ENDPOINT.buildUpon().appendQueryParameter("method",method);

        if(method.equals(SEARCH_METHOD))
            uriBuilder.appendQueryParameter("text",Query);

        return uriBuilder.build().toString();
    }


    private static void longInfo(String str) {
        if (str.length() > 4000) {
            Log.d("", str.substring(0, 4000));
            longInfo(str.substring(4000));
        } else
            Log.d("", str);
    }

    private static void copyIntoOut(InputStream in,ByteArrayOutputStream out) throws IOException {

        byte[] buf = new byte[8192];
        int length;
        while ((length = in.read(buf)) > 0) {
            out.write(buf, 0, length);
        }
    }
}
