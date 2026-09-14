package app.yoru.mobile;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

final class DownloadSizes {
    private DownloadSizes(){}
    static long exact(String url){
        String safe=ApiRepository.safeUrl(url);
        if(safe.isEmpty())return -1;
        if(safe.toLowerCase(Locale.ROOT).contains(".m3u8"))return hls(safe);
        try{return Net.length(safe);}catch(Exception e){return -1;}
    }
    static HashMap<Integer,Long> resolve(List<ApiRepository.DownloadOption> options){
        HashMap<Integer,Long> out=new HashMap<>();
        if(options==null||options.isEmpty())return out;
        int n=Math.min(options.size(),12);
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(8,n));
        ArrayList<Future<Long>> futures=new ArrayList<>();
        for(int i=0;i<n;i++){ApiRepository.DownloadOption o=options.get(i);String url=o==null||o.episode==null?"":o.episode.streams.get(o.quality);futures.add(pool.submit(()->exact(url)));}
        for(int i=0;i<n;i++){long value=-1;try{value=futures.get(i).get(9,TimeUnit.SECONDS);}catch(Exception ignored){}out.put(i,value);}
        pool.shutdownNow();
        return out;
    }
    private static long hls(String master){
        try{
            String playlist=Net.string(master,"GET",null,false,null);
            if(playlist==null)return -1;
            String base=master;
            if(playlist.contains("#EXT-X-STREAM-INF")){
                String best=null;long bestBw=-1,bw=-1;
                for(String line:playlist.split("\\r?\\n")){
                    String l=line.trim();
                    if(l.startsWith("#EXT-X-STREAM-INF")){Matcher m=Pattern.compile("BANDWIDTH=(\\d+)").matcher(l);bw=m.find()?Long.parseLong(m.group(1)):-1;}
                    else if(!l.isEmpty()&&!l.startsWith("#")){if(bw>bestBw){bestBw=bw;best=abs(base,l);}bw=-1;}
                }
                if(best==null)return -1;
                playlist=Net.string(best,"GET",null,false,null);
                base=best;
            }
            if(playlist==null)return -1;
            ArrayList<String> segments=new ArrayList<>();
            for(String line:playlist.split("\\r?\\n")){String l=line.trim();if(!l.isEmpty()&&!l.startsWith("#")&&!l.startsWith("data:"))segments.add(abs(base,l));}
            if(segments.isEmpty())return -1;
            int probes=Math.min(segments.size(),60);
            ExecutorService pool=Executors.newFixedThreadPool(Math.min(10,probes));
            ArrayList<Future<Long>> futures=new ArrayList<>();
            for(int i=0;i<probes;i++){String seg=segments.get(i);futures.add(pool.submit(()->Net.length(seg)));}
            long sum=0;int ok=0;
            for(Future<Long> f:futures){try{long v=f.get(7,TimeUnit.SECONDS);if(v>0){sum+=v;ok++;}}catch(Exception ignored){}}
            pool.shutdownNow();
            if(ok<Math.min(3,probes))return -1;
            return sum/ok*segments.size();
        }catch(Exception e){return -1;}
    }
    private static String abs(String base,String value){try{if(value==null||value.trim().isEmpty())return "";return new URL(new URL(base),value.trim()).toString();}catch(Exception e){return "";}}
}
