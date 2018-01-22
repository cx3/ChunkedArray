public class Usage {


    public static void zapis() {

        try {
            Runtime.getRuntime().exec("rm -rf marek"); //usuwanie cache z dysku - w windowsie bedzie del nazwa_folderu
            ChunkedArray<Integer> ca = new ChunkedArray<Integer>(0, "marek", 1000, 100);
            ca.set(333, 333);
            ca.saveCache();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void odczyt() {
        try{
            ChunkedArray<Integer> ca = ChunkedArray.loadFromCache("marek");
            for (int i=330;  i<335;  i++) {
                System.out.println(i+"\t"+ca.get(i));
            }
        } catch (ChunkedArray.CacheError cace) {
        }
    }


    public static void main(String []args) {

        zapis();
        odczyt();
    }


}
