package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Michelle Wu
 */
public class StagingArea implements Serializable {

    /** Map containing changed blobs to be added to next commit.
     * key = blob name. value = blob hash. **/
    private TreeMap<String, String> toAdd;

    /**
     * Map containing deleted blobs.
     */
    private TreeMap<String, String> toDelete;

    /** Current Working Directory. */
    private static File cwd = new File(System.getProperty("user.dir"));

    /** Main metadata folder. /gitlet/ in file directory. */
    private  static File gitletFolder = new File(cwd, ".gitlet");

    /**
     * constructs a staging area.
     */
    public StagingArea() {
        toAdd = new TreeMap<String, String>();
        toDelete = new TreeMap<String, String>();
    }

    /**
     * getter for toAdd.
     * @return TreeMap
     */
    public TreeMap<String, String> getToAdd() {
        return toAdd;
    }

    /**
     * getter for toDelete.
     * @return TreeMap
     */
    public TreeMap<String, String> getToDelete() {
        return toDelete;
    }

    /**
     * individual getter for toAdd.
     * @param key  key in add
     * @return String hash
     */
    public String getAdd(String key) {
        return (String) toAdd.get(key);
    }

    /**
     * individual setter for toAdd.
     * @param key : key to add to toAdd.
     * @param value
     */
    public void stageAdd(String key, String value) {
        toAdd.put(key, value);
    }

    /**
     * individual setter for toDelete.
     * @param key : key to add to toAdd.
     * @param value value
     */
    public void stageDelete(String key, String value) {
        toDelete.put(key, value);

    }

    /**
     * check if toAdd contains the file with filename name.
     * @param name : name checked
     * @return boolean true or false
     */
    public boolean addContains(String name) {
        return toAdd.containsKey(name);

    }

    /**
     * check if toDelete contains the file with filename name.
     * @param name : name checked
     * @return boolean true or false
     */
    public boolean delContains(String name) {
        return toDelete.containsKey(name);

    }

    /**
     * save StagingArea for persistence.
     * @throws IOException
     */
    public void saveArea() throws IOException {
        File f = new File(gitletFolder, "staging" + ".txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        Utils.writeObject(f, this);
    }

    /**
     * get a StagingArea from a file.
     * @return StagingArea
     */
    public static StagingArea fromFile() {
        File s = new File(gitletFolder, "staging" + ".txt");
        if (s.exists()) {
            StagingArea r = Utils.readObject(s, StagingArea.class);
            return r;
        } else {
            throw new IllegalArgumentException("staging area no exist");
        }
    }

    /**
     * reset both toAdd and toDel after commit.
     */
    public void reset() {
        toAdd = new TreeMap<>();
        toDelete = new TreeMap<>();

    }

    /**
     * checks if area is empty.
     * @return boolean.
     */
    public boolean empty() {
        return toAdd.size() == 0 && toDelete.size() == 0;
    }
}
