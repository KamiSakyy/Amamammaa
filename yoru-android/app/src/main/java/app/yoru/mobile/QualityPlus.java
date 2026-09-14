package app.yoru.mobile;

import java.util.Arrays;
import java.util.Collection;

final class QualityPlus {
    static final int BEST=9999;
    static final int[] VALUES={144,240,360,480,720,1080,1440,2160};
    static final String[] LABELS={"144p","240p","360p","480p","720p","1080p","2K · 1440p","4K · 2160p"};
    static final String[] LABELS_WITH_BEST={"144p","240p","360p","480p","720p","1080p","2K · 1440p","4K · 2160p","Лучшее доступное"};
    private QualityPlus(){}
    static int clamp(int value){if(value>=BEST)return 2160;if(value<=144)return 144;if(value<=240)return 240;if(value<=360)return 360;if(value<=480)return 480;if(value<=720)return 720;if(value<=1080)return 1080;if(value<=1440)return 1440;return 2160;}
    static int index(int value){int q=clamp(value);for(int i=0;i<VALUES.length;i++)if(VALUES[i]==q)return i;return 4;}
    static int[] values(){return Arrays.copyOf(VALUES,VALUES.length);}
    static int[] valuesWithBest(){int[] out=Arrays.copyOf(VALUES,VALUES.length+1);out[out.length-1]=BEST;return out;}
    static String name(int value){int q=clamp(value);if(q<=144)return "144p";if(q<=240)return "240p";if(q<=360)return "360p";if(q<=480)return "480p";if(q<=720)return "720p";if(q<=1080)return "1080p";if(q<=1440)return "2K (1440p)";return "4K (2160p)";}
    static String streamLabel(int value){return value<=0?"Оригинал":name(value);}
    static int bestAtOrBelow(Collection<Integer> rows,int cap){if(rows==null||rows.isEmpty())return 0;int limit=cap>=BEST?Integer.MAX_VALUE:clamp(cap),best=0;for(Integer q:rows){if(q==null)continue;int v=q; if(v<=0){if(best==0)best=v;continue;} if(v<=limit&&v>best)best=v;}if(best==0)for(Integer q:rows)if(q!=null){best=q;break;}return best;}
}
