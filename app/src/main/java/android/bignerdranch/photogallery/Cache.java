package android.bignerdranch.photogallery;

import android.graphics.Bitmap;
import android.util.LruCache;

public class Cache<T> {
    private static Cache instance;
    private LruCache<Object,Object> lru;

    private Cache(){
        
        lru=new LruCache<>(1024);
    }

    public static Cache getInstance(){
        if(instance==null)
            instance=new Cache();
        return instance;
    }

    public LruCache<Object,Object> getLru(){
        return lru;
    }

    public  void saveBitmapToCache(T target,Bitmap bitmap){
       instance.getLru().put(target,bitmap);

    }

    public  Bitmap retrieveBitmapFromCache(T target){
       return (Bitmap) instance.getLru().get(target);
    }
}
