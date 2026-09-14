package app.yoru.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class Net {
    private static final MediaType JSON=MediaType.get("application/json; charset=UTF-8");
    private static final MediaType FORM=MediaType.get("application/x-www-form-urlencoded; charset=UTF-8");
    private static volatile OkHttpClient client;
    private Net(){}
    static OkHttpClient http(){
        OkHttpClient c=client;
        if(c==null){
            synchronized(Net.class){
                c=client;
                if(c==null){
                    Dispatcher dispatcher=new Dispatcher();
                    dispatcher.setMaxRequests(16);
                    dispatcher.setMaxRequestsPerHost(8);
                    c=new OkHttpClient.Builder()
                            .dispatcher(dispatcher)
                            .connectTimeout(4,TimeUnit.SECONDS)
                            .readTimeout(9,TimeUnit.SECONDS)
                            .writeTimeout(9,TimeUnit.SECONDS)
                            .callTimeout(25,TimeUnit.SECONDS)
                            .connectionPool(new ConnectionPool(8,4,TimeUnit.MINUTES))
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .retryOnConnectionFailure(true)
                            .build();
                    client=c;
                }
            }
        }
        return c;
    }
    static String string(String url,String method,String body,boolean form,Map<String,String> headers)throws IOException{
        return string(url,method,body,form,headers,12*1024*1024);
    }
    static String string(String url,String method,String body,boolean form,Map<String,String> headers,int limit)throws IOException{
        TaskQueue.check();
        Request request=build(url,method,body,form,headers,null);
        try(Response r=http().newCall(request).execute()){
            TaskQueue.check();
            int code=r.code();
            if(code==429)throw new IOException("Каталог временно занят. Попробуйте позже.");
            if(code<200||code>=300)throw new IOException("Каталог сейчас недоступен. Попробуйте другой вариант.");
            return readBody(r,limit).toString("UTF-8");
        }
    }
    static byte[] bytes(String url)throws IOException{return bytes(url,null,8*1024*1024);}
    static byte[] bytes(String url,Map<String,String> headers,int limit)throws IOException{
        TaskQueue.check();
        Request request=build(url,"GET",null,false,headers,null);
        try(Response r=http().newCall(request).execute()){
            TaskQueue.check();
            int code=r.code();
            if(code<200||code>=300)throw new IOException("image");
            String type=r.header("Content-Type");
            String ct=type==null?"":type.toLowerCase(java.util.Locale.ROOT);
            if(!ct.isEmpty()&&!ct.startsWith("image/")&&!ct.contains("octet-stream"))throw new IOException("image");
            return readBody(r,limit).toByteArray();
        }
    }
    static long length(String url){return length(url,null);}
    static long length(String url,Map<String,String> headers){
        try{
            TaskQueue.check();
            Request head=build(url,"HEAD",null,false,headers,"bytes=0-0");
            try(Response r=http().newCall(head).execute()){
                long total=rangeTotal(r);
                if(total>0)return total;
                String cl=r.header("Content-Length");
                if(cl!=null&&r.code()==200){long n=parseLong(cl);if(n>0)return n;}
            }
            Request get=build(url,"GET",null,false,headers,"bytes=0-0");
            try(Response r=http().newCall(get).execute()){
                long total=rangeTotal(r);
                if(total>0)return total;
                String cl=r.header("Content-Length");
                if(cl!=null&&r.code()==200){long n=parseLong(cl);if(n>0)return n;
                    ResponseBody b=r.body();if(b!=null)return b.contentLength();}
            }
        }catch(Exception ignored){}
        return -1;
    }
    private static long rangeTotal(Response r){
        if(r.code()!=206&&r.code()!=416)return -1;
        String range=r.header("Content-Range");
        if(range==null)return -1;
        int cut=range.lastIndexOf('/');
        if(cut<0)return -1;
        return parseLong(range.substring(cut+1).trim());
    }
    private static long parseLong(String v){try{return Long.parseLong(v);}catch(Exception e){return -1;}}
    static final class Probe{int code;String contentType="";byte[] head=new byte[0];}
    static Probe probe(String url,Map<String,String> headers)throws IOException{
        TaskQueue.check();
        try(Response r=http().newCall(build(url,"GET",null,false,headers,"bytes=0-8191")).execute()){
            Probe p=new Probe();
            p.code=r.code();
            p.contentType=r.header("Content-Type","");
            ResponseBody b=r.body();
            if(b!=null)try(InputStream in=b.byteStream()){byte[] buf=new byte[8192];int n=in.read(buf);if(n>0){byte[] cut=new byte[n];System.arraycopy(buf,0,cut,0,n);p.head=cut;}}
            return p;
        }
    }
    static final class PageText{public String text="";public String cookies="";}
    static PageText page(String url,Map<String,String> headers)throws IOException{
        TaskQueue.check();
        try(Response r=http().newCall(build(url,"GET",null,false,headers,null)).execute()){
            if(r.code()<200||r.code()>=300)throw new IOException("Просмотр не ответил");
            PageText p=new PageText();
            p.text=readBody(r,12*1024*1024).toString("UTF-8");
            StringBuilder cookies=new StringBuilder();
            for(String v:r.headers("Set-Cookie")){String one=v.split(";",2)[0];if(one.isEmpty())continue;if(cookies.length()>0)cookies.append("; ");cookies.append(one);}
            p.cookies=cookies.toString();
            return p;
        }
    }
    private static Request build(String url,String method,String body,boolean form,Map<String,String> headers,String range){
        Request.Builder b=new Request.Builder().url(url);
        if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())if(e.getKey()!=null&&e.getValue()!=null)b.header(e.getKey(),e.getValue());
        if(range!=null)b.header("Range",range);
        if(body!=null)b.method(method,RequestBody.create(body,form?FORM:JSON));
        else if("POST".equalsIgnoreCase(method))b.post(RequestBody.create(new byte[0],FORM));
        else if("HEAD".equalsIgnoreCase(method))b.head();
        else b.get();
        return b.build();
    }
    private static ByteArrayOutputStream readBody(Response r,int limit)throws IOException{
        ResponseBody b=r.body();
        if(b==null)throw new IOException("Каталог не ответил");
        try(InputStream input=b.byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[16384];
            int n;
            while((n=input.read(buf))!=-1){
                TaskQueue.check();
                if(out.size()+n>limit)throw new IOException("Слишком большой ответ");
                out.write(buf,0,n);
            }
            return out;
        }
    }
}
