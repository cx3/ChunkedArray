import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ChunkedArray<Type> {


    public static class CacheError extends Exception {

        private String msg;

        public CacheError(String msg) {
            this.msg = msg;
        }

        public String toString() {
            return this.msg;
        }
    }

    //-----------------------------------------------------------



    public  Object[]  currentCache;
    private Type      defaultValue;
    private String    cacheDir;
    private long      maxSize;
    private int       chunkSize;
    private int       chunkPosition;
    private long      chunkCount;
    private long      currentIndex;
    private long      cacheFile;


    //-----------------------------------------------------------



    private ChunkedArray(String cacheDir)
            throws ChunkedArray.CacheError
    {
        //throw new ChunkedArray.CacheError("Functionality never coded before");

        try {
            FileInputStream fstream = new FileInputStream(cacheDir + "/cache_info.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine = "";
            ArrayList<String> cacheInfo = new ArrayList<>();

            while ((strLine = br.readLine()) != null)
                cacheInfo.add(strLine);

            fstream.close();
            br.close();


            this.maxSize = Long.valueOf( cacheInfo.get(0).split(":")[1] );
            this.chunkSize = Integer.valueOf( cacheInfo.get(1).split(":")[1] );
            String fileDefaultValue = cacheInfo.get(2).split(":")[1];


            System.out.println(">>fis="+cacheDir + "/" + fileDefaultValue.replaceAll("\n",""));
            fstream = new FileInputStream(cacheDir + "/" + fileDefaultValue.replaceAll("\n",""));
            ObjectInputStream ois = new ObjectInputStream(fstream);
            this.defaultValue = (Type) ois.readObject(); //type unsafe but works

            System.out.println("this.defV: " + this.defaultValue);

            this.currentCache = new Object[this.chunkSize];

            this.loadFromDisk(0);

            //below two instr should be above loadFromDisk ;>
            this.currentIndex = -1;
            this.chunkPosition = -1;
            this.cacheDir = cacheDir;

            System.out.println("zaladowano!");

        } catch (Exception e) {
            throw new ChunkedArray.CacheError("ChunkedArray ctor CacheError: "+e.getClass().getName());
        }

    }


    public ChunkedArray(Type defaulValue, String cacheDir, long maxSize, int chunkSize)
        throws ChunkedArray.CacheError
    {
        this.defaultValue = defaulValue;
        this.cacheDir = cacheDir;
        this.maxSize = maxSize;
        this.chunkSize = chunkSize;

        this.chunkPosition = 0;
        this.chunkCount = (int)( maxSize / chunkSize) + 1;

        this.currentIndex = 0;

        this.resetCache();
        this.fillCacheDefaultValues();

        this.createDiskStructure();
        this.saveCacheInfo();
        this.setCacheFile(this.currentIndex);
        this.saveCacheInfo();
    }



    private void createDiskStructure() {
        long chunkCount = (long) this.maxSize / this.chunkSize;
        (new File("./" + this.cacheDir)).mkdir();
        for (int i = 0; i < this.chunkCount; i++) {
            try {
                //(new File("./" + this.cacheDir + "/" + i + ".txt")).createNewFile();
                FileOutputStream fos = new FileOutputStream("./" + this.cacheDir + "/" + i + ".txt");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(this.currentCache);
                oos.close();
                fos.close();

            } catch (Exception e) {
                System.out.println("Create disk structure info");
            }
        }
    }


    public void fillCacheDefaultValues() {
        for (int i=0;  i<this.chunkSize;  i++)
            this.currentCache[i] = this.defaultValue;
    }


    public void resetCache() {
        this.currentCache = new Object[this.chunkSize];

        for (int i=0;  i<this.chunkSize;  i++)
            this.currentCache[i] = this.defaultValue;
    }


    public void saveCacheInfo() throws ChunkedArray.CacheError {
        try {
            PrintWriter pw = new PrintWriter("./" + this.cacheDir + "/cache_info.txt");
            pw.write("maxSize:" + this.maxSize + "\n");
            pw.write("chunkSize:" + this.chunkSize + "\n");
            pw.write("defaultValueFile:defaultValue.txt\n");
            pw.close();

            FileOutputStream fos = new FileOutputStream("./" + this.cacheDir + "/defaultValue.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(this.defaultValue);
            oos.close();
            fos.close();

        } catch (Exception e) {
            throw new ChunkedArray.CacheError("ChunkedArray.CacheError: saveCacheInfo Error");
        }
    }


    public void setCacheFile(long cacheFile) {
        this.cacheFile = cacheFile;
    }


    public ChunkedArray<Type> set(long index, Type value) throws ChunkedArray.CacheError
    {
        if (index > this.maxSize)
            throw new ChunkedArray.CacheError("Index out of bounds exception (index > maxSize");
        if (index < 0)
            throw new ChunkedArray.CacheError("Index out of bounds exception (index < 0");

        this.selectCacheFile(index);
        this.currentCache[ (int) (index % this.chunkSize) ] = value;
        return this;
    }


    public Type get(long index) throws ChunkedArray.CacheError {

        if (index > this.maxSize)
            throw new ChunkedArray.CacheError("Index out of bounds exception (index > maxSize");
        if (index < 0)
            throw new ChunkedArray.CacheError("Index out of bounds exception (index < 0");

        this.selectCacheFile(index);
        return (Type) this.currentCache[ (int) (index % this.chunkSize) ];
    }


    public void saveCache() {
        this.saveToDisk(this.currentIndex);
    }


    public void saveToDisk(long fileIndex) {
        try {
            FileOutputStream fos = new FileOutputStream("./" + this.cacheDir + "/" + fileIndex + ".txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.currentCache);
        } catch (Exception e) {
            System.out.println("ChunkedArray.saveToDisk() error: " + e);
            e.printStackTrace();
        }
    }


    public void loadFromDisk(long fileIndex) {
        if (fileIndex == this.currentIndex) return;

        try {
            //System.out.println("Loading cache...");
            FileInputStream fis = new FileInputStream("./" + this.cacheDir + "/" + fileIndex + ".txt"); //new FileInputStream("student.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.currentCache = (Type[]) ois.readObject();

            this.currentIndex = fileIndex;
        } catch (Exception e) {
            System.out.println("ChunkedArray.loadFromDisk() error at index: " + fileIndex + ", with error:"+ e);
            e.printStackTrace();
        }
    }


    public void selectCacheFile(long index) throws ChunkedArray.CacheError
    {
        long newIndex = 0;

        if (index != 0)
            newIndex = index / this.chunkSize;

        if (newIndex != this.currentIndex) {
            System.out.println("selectCacheFile: swap cache needed");
            this.saveToDisk(this.currentIndex);
            this.loadFromDisk(newIndex);
            this.currentIndex = newIndex;
        }
    }



    public long getMaxSize() {
        return this.maxSize;
    }


    //@Override
    public String toStringXY() {
        return "ChunkedArray{" +
                "currentCache=" + /*Arrays.toString(currentCache)*/  currentCache.length +
                ", defaultValue=" + defaultValue +
                ", cacheDir='" + cacheDir + '\n' +
                ", maxSize=" + maxSize + '\n' +
                ", chunkSize=" + chunkSize + '\n' +
                ", chunkPosition=" + chunkPosition + '\n' +
                ", chunkCount=" + chunkCount + '\n' +
                ", currentIndex=" + currentIndex + '\n' +
                ", cacheFile=" + cacheFile + '\n' +
                '}';
    }

    @Override
    public String toString() {

        /*

    public  Object[]  currentCache;
    private Type      defaultValue;
    private String    cacheDir;
    private long      maxSize;
    private int       chunkSize;
    private int       chunkPosition;
    private long      chunkCount;
    private long      currentIndex;
    private long      cacheFile;

         */

        StringBuilder sb = new StringBuilder();
        sb.append("currentCache.length: " + currentCache.length+"\n");
        sb.append("defaultType: " + defaultValue+"\n");
        sb.append("cacheDir: " + cacheDir+"\n");
        sb.append("maxSize:" + maxSize+"\n");
        sb.append("chunkSize: " + chunkSize+"\n");
        sb.append("chunkPosition: + " + chunkPosition+"\n");
        sb.append("chunkCount: " + chunkCount+"\n");
        sb.append("currentIndex: " + currentIndex+"\n");
        sb.append("cacheFile: " + cacheFile+"\n");

        return sb.toString();
    }

    public static <X> ChunkedArray<X>  loadFromCache(String cacheDir) throws ChunkedArray.CacheError
    {
        return new ChunkedArray<X>(cacheDir);
    }

}