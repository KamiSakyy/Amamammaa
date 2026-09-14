package app.yoru.mobile;

import android.graphics.*;
import android.widget.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

public final class ImageLoader {
    private final ExecutorService loadPool=new ThreadPoolExecutor(6,8,15L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(160),r->{Thread t=new Thread(r,"yoru-img");t.setPriority(Thread.NORM_PRIORITY-1);return t;},new ThreadPoolExecutor.DiscardPolicy());
    private final LinkedHashMap<String,Bitmap> memory=new LinkedHashMap<>(0,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Bitmap> eldest){return size()>64;}};
    private String lastKey="";
    public ImageLoader(android.content.Context context){}
    private String tag(String url){return url==null?"":url.trim();}
    private File cacheFile(String key){String raw="yoru-image-"+key.hashCode()+".webp";File dir=YoruApp.app().getCacheDir();return new File(dir,raw);}
    public void load(ImageView v,Anime a){if(v==null||a==null)return;load(v,a.poster,a.key());}
    public void load(final ImageView v,final String url,final String key){if(v==null)return;final String target=key==null||key.trim().isEmpty()?tag(url):key.trim();final String safe=ApiRepository.safeUrl(url);v.setTag(target);
        Bitmap cached=memory.get(target);
        if(cached!=null&&!cached.isRecycled()){lastKey=target;v.setImageBitmap(cached);return;}
        final File file=cacheFile(target);
        final boolean warm=file.exists()&&file.length()>0;
        if(!warm)v.setImageResource(R.drawable.ic_yoru);
        if(safe.isEmpty())return;
        loadPool.execute(new Runnable(){public void run(){if(Thread.currentThread().isInterrupted())return;try{Bitmap b=warm?loadFile(file,target):bytes(safe);if(b==null)return;synchronized(memory){memory.put(target,b);if(memory.size()>64){for(String k:memory.keySet()){memory.remove(k);break;}}}lastKey=target;final Object tag=v.getTag();if(tag!=null&&tag.equals(target))YoruApp.app().main.post(new Runnable(){public void run(){if(tag.equals(v.getTag()))v.setImageBitmap(b);}});}catch(Exception ignored){}}});
    }
    private Bitmap loadFile(File file,String key){try{Bitmap b=BitmapFactory.decodeFile(file.getAbsolutePath());if(b!=null)return b;}catch(Exception ignored){}return null;}
    private Bitmap bytes(String url){
        ArrayList<String> tries=new ArrayList<>();
        boolean saver=false;
        try{YoruApp app=YoruApp.app();if(app!=null&&app.store!=null&&app.store.ready())saver=app.store.dataSaver();}catch(Exception ignored){}
        if(saver){for(int q:QualityPlus.values())tries.add(heightVariant(url,q));}
        tries.add(url);
        for(String t:tries){try{byte[] data=Net.bytes(t);if(data!=null&&data.length>0){Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length);if(b!=null){cache(t,b);return b;}}}catch(Exception ignored){}}
        return null;
    }
    private static String heightVariant(String url,int q){try{return url.replaceFirst("(?i)([0-9]{3,4})p",q+"p");}catch(Exception e){return url;}}
    private void cache(String url,Bitmap b){try{String key=url==null||url.trim().isEmpty()?url:url.trim();File file=cacheFile(key);if(file.getParentFile()!=null)file.getParentFile().mkdirs();File tmp=new File(file.getAbsolutePath()+".tmp");FileOutputStream out=new FileOutputStream(tmp);b.compress(Bitmap.CompressFormat.WEBP,86,out);out.flush();out.close();if(!tmp.renameTo(file))file.delete();}catch(Exception ignored){}}
    void trim(boolean hard){synchronized(memory){memory.clear();}System.gc();}
    public void clear(){synchronized(memory){memory.clear();}try{File dir=YoruApp.app().getCacheDir();File[] rows=dir.listFiles();if(rows!=null)for(File f:rows)if(f!=null&&f.getName()!=null&&f.getName().startsWith("yoru-image-"))f.delete();}catch(Exception ignored){}}
}
