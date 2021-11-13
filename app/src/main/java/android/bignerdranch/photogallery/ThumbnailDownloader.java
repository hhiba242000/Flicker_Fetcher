package android.bignerdranch.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    //a thread for downloading in the background
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Cache<T> lruCache;
    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        //the listener interface that will be passed to the main thread
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        //the method that will be called upon ThumbnailDownloader object in main thread so it works as a listener
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        lruCache=Cache.getInstance();
    }

    public ThumbnailDownloader() {
        super(TAG);
    }


    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }

            }
        };
    }


    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);
        if (url == null) {
            mRequestMap.remove(target);
        }
        else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

//the almighty method responsible for downloading the pictures
    private void handleRequest(final T target) {
        T lTarget;
        Bitmap lBitmap;
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }
            else if (lruCache.getLru().snapshot().containsKey(target)) {
                lBitmap = lruCache.retrieveBitmapFromCache(target);
                Log.i(TAG, "bitmap  retrieved from cache");
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mThumbnailDownloadListener.onThumbnailDownloaded(target, lBitmap);

                    }
                });
            }
            else{
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            mResponseHandler.post(new Runnable() {
                                      public void run() {
                                          if (!mRequestMap.get(target).equals(url) || mHasQuit) {
                                              return;
                                          }
                                          mRequestMap.remove(target);
                                          lruCache.saveBitmapToCache(target, bitmap);
                                          Log.i(TAG, "bitmap saved to cache");
                                          mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                                      }
                                  }

            );
        }
        }
        catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }

}
}
