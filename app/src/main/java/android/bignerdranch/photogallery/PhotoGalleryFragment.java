package android.bignerdranch.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// this class responsible for displaying the ui, you can consider it your entry point to the main thread
public class PhotoGalleryFragment extends VisibleFragment {
    private RecyclerView mPhotoRecyclerView;
    private String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Background thread started");
        setRetainInstance(true);//used to keep same screen when phone is tilted
        setHasOptionsMenu(true);//cuz in the toolbar you inserted a menu containing a searchView and a clear 'x'
        updateItems();// called to update the screen

        Handler responseHandler = new Handler();//since you using a HandleThread you need a handler and looper
        mThumbnailDownloader = new ThumbnailDownloader<> (responseHandler);//the thumb nail downloader is responsible for downloading the photos from flicker
       //to link the downloader object mThumbnailDownloader to the photogalleryFragment you call the next method
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void
            onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        //the downloading thread starts execution
        mThumbnailDownloader.start();
        //the handler and looper are your pawns to connect results of bkground thread to UI thread
        mThumbnailDownloader.getLooper();
    }

    //method called when Options menu is created
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery,menu);//inflated the components from the xml

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView= (SearchView) searchItem.getActionView();//bind the xml component to a SearchView object
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {//append a listener to it
            @Override
            public boolean onQueryTextSubmit(String s) {//called when a text is entered in the search bar
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(),s);
                updateItems();
                return true;

            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//a clickListener is attached to the search bar so that the last search in history is pluged in
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        }
        else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateItems(){
        String Query= QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(Query).execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //clear the queue of photos to be downloaded
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setupAdapter();
        return v;

    }

    private void setupAdapter(){
        if(isAdded()/*reutrn true if fragment is attached to activity*/){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
            mItemImageView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem=galleryItem;
        }

        @Override
        public void onClick(View view) {
            Intent i=PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoUri());
            startActivity(i);
        }
    }

private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
    private List<GalleryItem> mGalleryItems;

    public PhotoAdapter(List<GalleryItem> galleryItems) {
        mGalleryItems = galleryItems;
    }

    @Override
    public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoHolder photoHolder, int position) {
        GalleryItem galleryItem = mGalleryItems.get(position);
        photoHolder.bindGalleryItem(galleryItem);
        Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
        photoHolder.bindDrawable(placeholder);
        mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
    }

    @Override
    public int getItemCount() {
        return mGalleryItems.size();
    }
}

//AsyncTask is an abstraction of one of 3 forms of thread management,here you use it if you wanna perform tiny sequential tasks in the background
//this is considered a bkground job but instead of creating its own thread you pass it to AsuyncTask
    //the job is to fetch the json of the page and construct an array of GalleryItems where you can find each photo's url to download
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
    private String mQuery;

    public FetchItemsTask(String query) {
        mQuery = query;
    }

    @Override
    protected List<GalleryItem> doInBackground(Void... voids) {
        if(mQuery == null)
            return new FlickrFetchr().fetchRecentPhotos();
        else
            return new FlickrFetchr().searchPhotos(mQuery);
        }

    @Override
    protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
    }
}

}



