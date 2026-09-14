package app.yoru.mobile;

import java.util.Locale;

final class Genres {
    private Genres(){}
    static final String[][] SHIKIMORI={
            {"1","Экшен"},{"2","Приключения"},{"3","Машины"},{"4","Комедия"},{"5","Безумие"},{"6","Демоны"},{"7","Детектив"},{"8","Драма"},{"9","Этти"},{"10","Фэнтези"},
            {"11","Игры"},{"12","Хентай"},{"13","Исторический"},{"14","Ужасы"},{"15","Детское"},{"16","Магия"},{"17","Боевые искусства"},{"18","Меха"},{"19","Музыка"},{"20","Пародия"},
            {"21","Самураи"},{"22","Романтика"},{"23","Школа"},{"24","Фантастика"},{"25","Сёдзё"},{"26","Сёдзё-ай"},{"27","Сёнен"},{"28","Сёнен-ай"},{"29","Космос"},{"30","Спорт"},
            {"31","Супер сила"},{"32","Вампиры"},{"33","Яой"},{"34","Юри"},{"35","Гарем"},{"36","Повседневность"},{"37","Сверхъестественное"},{"38","Военное"},{"39","Полиция"},{"40","Психологическое"},
            {"41","Триллер"},{"42","Сэйнэн"},{"43","Дзёсей"},{"539","Эротика"},{"541","Работа"},{"543","Гурман"}};
    static String[][] all(String source){
        if("yummy".equals(source))return new String[][]{{"","Все жанры"},{"63","Экшен"},{"32","Приключения"},{"16","Комедия"},{"14","Детектив"},{"15","Драма"},{"42","Фэнтези"},{"33","Романтика"},{"37","Фантастика"},{"86","Повседневность"},{"9","Этти"}};
        if("anilibria".equals(source))return new String[][]{{"","Все жанры"},{"14","Экшен"},{"27","Приключения"},{"1","Комедия"},{"8","Драма"},{"29","Фэнтези"},{"11","Романтика"}};
        String[][] out=new String[SHIKIMORI.length+1][];
        out[0]=new String[]{"","Все жанры"};
        System.arraycopy(SHIKIMORI,0,out,1,SHIKIMORI.length);
        return out;
    }
    static String idByName(String name){
        String n=name==null?"":name.trim().toLowerCase(Locale.ROOT);
        if(n.isEmpty())return "";
        for(String[] row:SHIKIMORI)if(row[1].toLowerCase(Locale.ROOT).equals(n))return row[0];
        for(String[] row:SHIKIMORI)if(row[1].toLowerCase(Locale.ROOT).contains(n)||n.contains(row[1].toLowerCase(Locale.ROOT)))return row[0];
        return "";
    }
    static String nameById(String id){
        for(String[] row:SHIKIMORI)if(row[0].equals(id))return row[1];
        return "";
    }
}
