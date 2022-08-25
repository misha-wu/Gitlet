package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Michelle Wu
 */
public class Commit implements Serializable {

    /** default date and time of first commit. **/
    private static final LocalDateTime DEFAULT_TEMPORAL_TIME =
            LocalDateTime.of(1970, 1, 1, 0, 0, 0);

    /** default date and time with timezone. **/
    private static final ZonedDateTime DEFAULT_TIME =
            ZonedDateTime.of(DEFAULT_TEMPORAL_TIME,
                    ZoneId.of("America/Los_Angeles"));

    /** default String msg of first commit. **/
    private static final String DEFAULT_STRING =
            "initial commit";

    /** gitlet folder. **/
    private static final File COMMIT_FOLDER =
            new File("./.gitlet/.commits/");

    /** commit log. **/
    private String log;

    /** time of commit. **/
    private ZonedDateTime time;

    /** all files being tracked in this current commit.
     * key: name of blob (filename)
     * value: hashed blob.**/
    private TreeMap<String, String> trackedList;

    /**
     * immediate parent.
     */
    private Commit parent;

    /**
     * parent from merge; default null.
     */
    private Commit mergeParent = null;

    /**
     * unique id of a commit.
     */
    private String hash;

    /**
     * creates a new commit object with these params.
     * @param l : commit log l.
     * @param p : parent p.
     * @param addList : list of ADDS since last commit. must combine this
     *                  and parent list.
     * @param delList : list of REMOVES to be pushed
     */
    @SuppressWarnings("unchecked")
    public Commit(String l, Commit p, TreeMap<String, String> addList,
                  TreeMap<String, String> delList) throws IOException {
        log = l;
        parent = p;
        time = ZonedDateTime.now();
        trackedList = (TreeMap) p.trackedList.clone();

        for (String key : addList.keySet()) {
            trackedList.put(key, addList.get(key));
        }

        for (String key : delList.keySet()) {
            trackedList.remove(key);
        }
        hash = Utils.sha1(log, time.toString(),
                trackedList.toString(), parent.hash);
        saveFile();
    }

    /**
     * make a commit from a merge.
     * @param l
     * @param p
     * @param mergeP
     * @param addList
     * @param delList
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public Commit(String l, Commit p, Commit mergeP,
                  TreeMap<String, String> addList,
                  TreeMap<String, String> delList) throws IOException {
        log = l;
        parent = p;
        mergeParent = mergeP;
        time = ZonedDateTime.now();
        trackedList = (TreeMap) p.trackedList.clone();

        for (String key : addList.keySet()) {
            trackedList.put(key, addList.get(key));
        }

        for (String key : delList.keySet()) {
            trackedList.remove(key);
        }
        hash = Utils.sha1(log, time.toString(),
                trackedList.toString(), parent.hash + mergeParent.hash);
        saveFile();
    }

    /**
     * make a default commit.
     * @throws IOException
     *
     */
    @SuppressWarnings("unchecked")
    public Commit() throws IOException {
        log = DEFAULT_STRING;
        parent = null;
        time = DEFAULT_TIME;
        trackedList = new TreeMap();

        hash = Utils.sha1(log, time.toString());
        saveFile();
    }

    /**
     *get a commit from a file.
     * @param name
     * @return
     */
    public static Commit fromFile(String name) {
        File f = new File(COMMIT_FOLDER, name + ".txt");
        if (f.exists()) {
            Commit c = Utils.readObject(f, Commit.class);
            return c;
        } else {
            throw new IllegalArgumentException("File no exist");
        }

    }

    /**
     * save commit as a file.
     * @throws IOException
     */
    public void saveFile() throws IOException {
        File f = new File(COMMIT_FOLDER, hash + ".txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        Utils.writeObject(f, this);
    }

    /**
     * get a log.
     * @return log
     */
    public String getLog() {
        return log;
    }

    /**
     * get time.
     * @return time
     */
    public ZonedDateTime getTime() {
        return time;
    }

    /**
     * get tracked list.
     * @return tree map.
     */
    public TreeMap<String, String> trackedList() {
        return trackedList;
    }

    /**
     * get functional parent.
     * @return Commit
     */
    public Commit getParent() {
        return parent;
    }

    /**
     * get parent that resulted from a merge.
     * @return Commit
     */
    public Commit getMergeParent() {
        return mergeParent;
    }

    /**
     * get hash.
     * @return String
     */
    public String hash() {
        return hash;
    }
}
