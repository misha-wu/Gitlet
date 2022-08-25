package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Michelle Wu
 */
public class Repo implements Serializable {
    /** Current Working Directory. */
    private static File cwd = new File(System.getProperty("user.dir"));

    /** Main metadata folder. /gitlet/ in file directory. */
    private static File gitletFolder = new File(cwd, ".gitlet");

    /** Commit folder. /.gitlet/.commits/ in directionry. */
    private static File commitFolder = new File(gitletFolder, ".commits");

    /** Blob folder. /.gitlet/.blobs/ in directory. */
    private static File blobFolder = new File(gitletFolder, ".blobs");

    /** Staging area of this repo.**/
    private StagingArea stage;

    /**formatter for log.**/
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy ZZZ");

    /** first commit; same in every repo.**/
    private Commit first;

    /** current head commit. **/
    private Commit head;

    /** current branch. **/
    private String branch;

    /** map of branches of current repo.
     * key: branch name
     * value: commit ID (hash) of most recent commit in branch key.**/
    private TreeMap<String, String> branchMap;

    /**
     * initializes a repo in directory parent,
     * with a single branch (master).
     * makes a first commit with message "first commit";
     * commit time is set to unix epoch.
     * if there is already a gitlet repo,
     * throws error
     * **/
    public Repo() throws IOException {
        if (gitletFolder.exists()) {
            File f = new File(cwd, "repo.txt");
            throw new GitletException("A Gitlet version-control "
                    + "system already exists in the current directory.");
        } else {
            init();
            saveRepo();
        }

    }

    /**
     * starting from head commit,
     * make your way back to first commit via parents, ignoring
     * first parents.
     * NOT UPDATED FOR MERGE
     */
    public void log() {
        Commit c = head;
        while (c != null) {
            System.out.println("===");
            System.out.println("commit " + c.hash());
            System.out.println("Date: " + TIME_FORMATTER.format(c.getTime()));
            System.out.println(c.getLog());
            System.out.println();
            c = c.getParent();
        }

    }

    /**
     * prints global log of commits.
     */
    public void globalLog() {
        List<String> arr = Utils.plainFilenamesIn(commitFolder);
        for (String filename : arr) {
            File f = new File(commitFolder, filename);
            Commit c = Utils.readObject(f, Commit.class);
            System.out.println("===");
            System.out.println("commit " + c.hash());

            System.out.println("Date: " + TIME_FORMATTER.format(c.getTime()));
            System.out.println(c.getLog());
            System.out.println();

        }
    }

    /**
     * checks if a gitlet repository exists in the cwd.
     * @return boolean true or false
     */
    public static boolean repoExists() {
        File f = new File(gitletFolder, "repo.txt");
        return f.exists();
    }

    public void saveRepo() throws IOException {
        File f = new File(gitletFolder, "repo" + ".txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        Utils.writeObject(f, this);
        stage.saveArea();

    }

    /**
     * gets a repository from a file.
     * @return gitlet object.
     */
    public static Repo findRepo() {
        if (Repo.repoExists()) {
            File f = new File(gitletFolder, "repo" + ".txt");
            if (f.exists()) {
                Repo r = Utils.readObject(f, Repo.class);
                r.stage = StagingArea.fromFile();
                return r;
            } else {
                throw new IllegalArgumentException("repo no exist");
            }
        } else {
            throw new
                    GitletException("Not in an initialized Gitlet directory.");
        }

    }

    public void init() throws IOException {
        gitletFolder.mkdir();
        commitFolder.mkdir();
        blobFolder.mkdir();
        stage = new StagingArea();
        branch = "master";
        branchMap = new TreeMap<String, String>();

        Commit c = new Commit();
        head = c;
        first = c;
        branchMap.put(branch, head.hash());

    }

    /**
     * equiv to gitlet add command. adds a file to stagingarea.
     * @param args args of add
     * @throws IOException
     */
    public void add(String[] args) throws IOException {
        if (args.length < 2) {
            throw new GitletException("not enough args (add)" + args[1]);
        }
        add(args[1]);

    }

    /**
     * (1) add the file to the staging area if the file is not being tracked,
     * or the file is tracked but has been changed,
     * or the file has been staged but was changed.
     * (2) If the file is in the staging area and then the
     * file was added and was reverted back to the current commit,
     * we remove it from staging area.
     * (3) If the file is in the
     * stage for removal, it will be removed if we add the file
     * and added to staging area
     * @param name : filename; name of file being added
     */
    public void add(String name) throws IOException {
        File f = new File(cwd, name);
        File prev = null;
        String prevHash = "";
        if (!f.exists()) {
            throw new GitletException("File does not exist.");
        } else if (stage.addContains(name)) {
            prev = new File(blobFolder, stage.getAdd(name) + ".txt");
            prevHash = Utils.sha1(Utils.readContentsAsString(prev));
        } else if (stage.delContains(name)) {
            stage.getToDelete().remove(name);
            return;
        } else if (head.trackedList().get(name) != null) {
            prev = new File(blobFolder, head.trackedList().get(name) + ".txt");
            prevHash = Utils.sha1(Utils.readContentsAsString(prev));
        } else {
            prevHash = "";
        }
        String currHash = Utils.sha1(Utils.readContentsAsString(f));
        if (prevHash.equals(currHash)) {
            if (stage.addContains(name)) {
                stage.getToAdd().remove(name);
            }
            return;
        }
        File newLoc = new File(blobFolder, currHash + ".txt");
        if (!newLoc.exists()) {
            newLoc.createNewFile();
        }
        Utils.writeContents(newLoc, Utils.readContentsAsString(f));
        stage.stageAdd(name, currHash);
    }

    /**
     * wrapped function for remove.
     * @param args filename being removed
     */
    public void remove(String[] args) {
        if (args.length != 2) {
            throw new GitletException("wrong # of args (rm)");
        }
        remove(args[1]);
    }

    /**
     * Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit,
     * stage it for removal and remove the file from the working directory
     * if the user has not already done so
     * do not remove it unless it is tracked in the current commit).
     * @param name : name of file to be removed
     */
    private void remove(String name) {
        File f = new File(cwd, name);

        if (stage.addContains(name)) {
            stage.getToAdd().remove(name);
        } else if (head.trackedList().containsKey(name)) {
            if (f.exists()) {
                Utils.restrictedDelete(f);
            }
            stage.stageDelete(name, head.trackedList().get(name));
        } else {
            throw new GitletException("No reason to remove the file.");
        }

    }

    /**
     * inner function for making a commit.
     * @param args : commit message
     * @throws IOException
     */
    public void makeCommit(String[] args) throws IOException {

        if (args.length != 2) {
            throw new GitletException("wrong # of arguments (commit)");
        } else if (args[1].length() == 0) {
            throw new GitletException("Please enter a commit message.");
        }
        if (stage.getToAdd().size() != 0 || stage.getToDelete().size() != 0) {
            makeCommit(args[1], branch, stage.getToAdd(), stage.getToDelete());
        } else {
            throw new GitletException("No changes added to the commit.");
        }

    }

    /**
     *  creates a commit object of everything in current staging area
     * and appends to HEAD.
     * @param msg
     * @param branchName
     * @param addTracking
     * @param delTracking
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void makeCommit(String msg, String branchName,
                           TreeMap addTracking, TreeMap delTracking)
            throws IOException {
        Commit parent = head;
        Commit c = new Commit(msg, parent, addTracking, delTracking);
        head = c;
        branchMap.put(branchName, c.hash());
        stage.reset();
    }

    /**
     * creates a commit object of everything in current staging area;
     * includes parent options.
     * @param msg : String message of commit.
     * @param addTracking : files that are added/changed.
     * @param branchName : branch name
     * @param delTracking : files that have been deleted.
     * @param parent : immediate parent.
     * @param mergeParent : parent created through merging; default null
     */
    @SuppressWarnings("unchecked")
    public void makeCommit(String msg,
                           String branchName,
                           TreeMap addTracking, TreeMap delTracking,
                           Commit parent,
                           Commit mergeParent) throws IOException {

        Commit c = new Commit(msg,
                parent, mergeParent, addTracking, delTracking);
        head = c;
        branchMap.put(branchName, c.hash());
        stage.reset();
    }

    /**
     * for java gitlet.Main checkout -- [file name] form
     * Takes the version of the file as it exists in the head commit,
     * + puts it in the cwd, overwriting the version of the file
     * that's already there if there is one.
     * The new version of the file is not staged.
     * @param args : checkout -- [file name] format
     */
    public void checkoutLast(String[] args) throws IOException {
        if (args.length != 3) {
            throw new GitletException("wrong # args (checkout recent commit)");
        } else if (!args[1].equals("--")) {
            throw new GitletException("Incorrect operands.");
        }
        String fileHash = head.trackedList().get(args[2]);
        File src = new File(blobFolder, fileHash + ".txt");
        File dest = new File(cwd, args[2]);

        checkoutByName(src, dest);
    }

    /**
     * for java gitlet.Main checkout -- [file name] form
     * Takes the version of the file as it exists in the head commit,
     * + puts it in the cwd, overwriting the version of the file
     * that's already there if there is one.
     * The new version of the file is not staged.
     * @param src : source file
     * @param dest : dest file
     */
    public void checkoutByName(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.createNewFile();
        }
        if (stage.addContains(dest.getName())) {
            stage.getToAdd().remove(dest);
        }
        Utils.writeContents(dest, Utils.readContentsAsString(src));
    }

    /**
     * search for a commit by hash (id).
     * @param shortHash : short or long version of a commit hash.
     * @return File that is the commit.
     */
    private File findCommitById(String shortHash) {
        List<String> s = Utils.plainFilenamesIn(commitFolder);
        for (String str : s) {
            if (str.startsWith(shortHash)) {
                File f = new File(commitFolder, str);
                return f;
            }
        }
        return new File(commitFolder, shortHash + ".txt");
    }

    /**
     * the other form of checkout;
     * java gitlet.Main checkout [commit id] -- [file name].
     * Takes the version of the
     * file as it exists in the commit with the given id,
     * and puts it in the working directory,
     * overwriting the version of the
     * file that's already there if there is one.
     * The new version of the file is not staged.
     * @param args : commit id, --, filename.
     */
    public void checkout(String[] args) throws IOException {
        if (!args[2].equals("--")) {
            throw new GitletException("Incorrect operands.");
        }
        String filename = args[3];
        File srcComm = findCommitById(args[1]);
        if (!srcComm.exists()) {
            throw new GitletException("No commit with that id exists.");
        } else {
            Commit c = Utils.readObject(srcComm, Commit.class);
            File dest = new File(cwd, filename);
            if (dest.exists() && !(stage.addContains(filename)
                    || head.trackedList().containsKey(filename))) {
                throw new
                        GitletException("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            }
            if (c.trackedList().containsKey(filename)) {
                File src = new File(blobFolder,
                        c.trackedList().get(filename) + ".txt");
                checkoutByName(src, dest);
            } else {
                throw new
                        GitletException("File does not exist in that commit.");
            }
        }
    }

    /**
     * helper to check merge validity.
     * @param newBranch
     */
    public void checkValidityMerge(String newBranch) {
        if (!branchMap.containsKey(newBranch)) {
            throw new
                    GitletException("A branch with that name does not exist.");
        } else if (newBranch.equals(branch)) {
            throw new
                    GitletException("Cannot merge a branch with itself.");
        } else if (!stage.empty()) {
            throw new
                    GitletException("You have uncommitted changes.");
        }
    }

    /**
     * merge two branches together
     * and create a resulting commit.
     * @param args : args[1]
     * @throws IOException
     */
    public void merge(String[] args) throws IOException {
        if (args.length != 2) {
            throw new GitletException("wrong # of args (merge)");
        }
        String newBranch = args[1];
        Commit currHead = head;

        checkValidityMerge(newBranch);
        File headFile =
                new File(commitFolder, branchMap.get(newBranch) + ".txt");
        Commit givenHead =
                Utils.readObject(headFile, Commit.class);
        Commit split =
                findSplitPoint(currHead, givenHead);

        if (split.hash().equals(currHead.hash())) {
            checkoutBranch(new String[]{"checkout", newBranch});
            throw new GitletException("Current branch fast-forwarded.");
        } else if (split.hash().equals(givenHead.hash())) {
            throw new GitletException("Given branch is "
                    + "an ancestor of the current branch.");
        }
        checkAgainstGiven(split, currHead, givenHead);

        for (String filename : split.trackedList().keySet()) {
            if (!currHead.trackedList().containsKey(filename)
                    || !givenHead.trackedList().containsKey(filename)) {
                File f = new File(cwd, filename);
                if (f.exists()) {
                    Utils.restrictedDelete(f);
                    stage.stageDelete(filename,
                            split.trackedList().get(filename));
                }
            }
        }

        for (String filename : currHead.trackedList().keySet()) {
            if (modified(split, currHead, filename)
                    && modified(split, givenHead, filename)
                    && !givenHead.trackedList().containsKey(filename)) {
                System.out.println("Encountered a merge conflict.");
                writeMergeError(givenHead, currHead, filename);
            }
        }

        String msg = "Merged "
                + newBranch + " into " + branch + ".";
        makeCommit(msg, branch,
                stage.getToAdd(), stage.getToDelete(),
                currHead, givenHead);


    }

    public void checkAgainstGiven(Commit split,
                                  Commit currHead, Commit givenHead)
                                throws IOException {
        for (String filename : givenHead.trackedList().keySet()) {
            if (modified(split, givenHead, filename)) {
                if (modified(split, currHead, filename)) {
                    if (!currHead.trackedList().get(filename).
                            equals(givenHead.trackedList().get(filename))) {
                        System.out.println("Encountered a merge conflict.");
                        writeMergeError(givenHead, currHead, filename);
                    }

                } else {
                    checkout(new String[]{"checkout",
                            givenHead.hash(), "--", filename});
                    stage.stageAdd(filename,
                            givenHead.trackedList().get(filename));

                }
            }
        }
    }


    /**
     * write a merge error for filename in commits given and curr.
     * @param given : head of given branch in merge
     * @param curr : head of current branch in merge
     * @param filename : file that
     * has been changed in both commits; overwriting file.
     */
    private void writeMergeError(Commit given,
                                 Commit curr, String filename)
                                throws IOException {
        File currVersion = new File(blobFolder,
                curr.trackedList().get(filename) + ".txt");
        String currPart = "<<<<<<< HEAD\n"
                + Utils.readContentsAsString(currVersion);


        File givenVersion = new File(blobFolder,
                given.trackedList().get(filename) + ".txt");
        String givenPart = "=======\n" + ">>>>>>>\n";
        if (givenVersion.exists()) {
            givenPart = "=======\n"
                    + Utils.readContentsAsString(givenVersion)
                    + ">>>>>>>\n";
        }
        String contents = currPart + givenPart;
        File src = new File(cwd, filename);

        Utils.writeContents(src, contents);
        File newLoc = new File(blobFolder,
                Utils.sha1(contents) + ".txt");
        if (!newLoc.exists()) {
            newLoc.createNewFile();
        }
        Utils.writeContents(newLoc, contents);
        stage.stageAdd(filename, Utils.sha1(contents));

    }

    /**
     * checks if filename has been edited since split
     * commit in current.
     * @param split : most recent parent commit.
     * @param current : current commit:
     * @param filename : file checking.
     * @return
     */
    public boolean modified(Commit split, Commit current, String filename) {
        String splitHash =
                split.trackedList().get(filename);
        String currentHash =
                current.trackedList().get(filename);
        if ((splitHash == null)
                && (currentHash == null)) {
            return false;
        }
        if ((splitHash == null)
                ^ (currentHash == null)) {
            return true;
        }
        return !splitHash.equals(currentHash);
    }

    /**
     * find the split point using bfs.
     * @param oldHead : first parent from current.
     * @param newHead : second parent from given.
     * @return most recent parent commit.
     */
    private Commit findSplitPoint(Commit oldHead, Commit newHead) {
        HashSet<String> seen = new HashSet<String>();
        ArrayDeque<Commit> queue = new ArrayDeque<Commit>();

        queue.add(newHead);
        queue.add(oldHead);

        while (queue.size() != 0) {
            Commit next = queue.pop();
            if (seen.contains(next.hash())) {
                return next;
            } else {
                seen.add(next.hash());
                if (next.getParent() != null) {
                    queue.add(next.getParent());
                }
                if (next.getMergeParent() != null) {
                    queue.add(next.getMergeParent());
                }
            }
        }
        return first;
    }

    /**
     * find a commit by commit log.
     * @param args : args[1] = log.
     */
    public void find(String[] args) {
        if (args.length != 2) {
            throw new GitletException("wrong # of args (find)");
        }
        boolean found = false;
        List<String> arr = Utils.plainFilenamesIn(commitFolder);
        for (String filename : arr) {
            File f = new File(commitFolder, filename);
            Commit c = Utils.readObject(f, Commit.class);
            if (c.getLog().equals(args[1])) {
                System.out.println(c.hash());
                found = true;
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /**
     * make a new branch in commit tree.
     * does not check out immediately.
     * @param args : args[1] = branch name.
     */
    public void branch(String[] args) {
        if (args.length != 2) {
            throw new GitletException("wrong # of args (branch)");
        }
        String name = args[1];
        if (branchMap.containsKey(name)) {
            throw new GitletException("A branch with that name"
                    + " already exists.");
        } else {
            branchMap.put(name, head.hash());
        }
    }

    /**
     * prints out a status of the files in CWD.
     */
    public void status() {
        System.out.println("=== Branches ===");
        for (String b : branchMap.keySet()) {
            if (b.equals(branch)) {
                System.out.println("*" + b);
            } else {
                System.out.println(b);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String s : stage.getToAdd().keySet()) {
            System.out.println(s);
        }
        System.out.println();
        ArrayList<String> modified = new ArrayList<>();
        ArrayList<String> untracked = new ArrayList<>();
        List<String> strs = Utils.plainFilenamesIn(cwd);

        for (String s : strs) {
            File src = new File(cwd, s);
            String contents = Utils.sha1(Utils.readContentsAsString(src));
            if (stage.addContains(s)) {
                File f = new File(blobFolder, stage.getAdd(s) + ".txt");
                if (Utils.sha1(
                        Utils.readContentsAsString(f)).equals(contents)) {
                    modified.add(s);
                }
            } else if (head.trackedList().containsKey(s)) {
                File f = new File(blobFolder,
                        head.trackedList().get(s) + ".txt");
                if (Utils.sha1(
                        Utils.readContentsAsString(f)).equals(contents)) {
                    modified.add(s);
                }
            } else {
                untracked.add(s);
            }
        }
        System.out.println("=== Removed Files ===");
        for (String s : stage.getToDelete().keySet()) {
            System.out.println(s);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for(String s : modified) {
            System.out.println(s);
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");
        for(String s : untracked) {
            System.out.println(s);
        }
        System.out.println();
    }

    /**
     * remove a branch.
     * @param args : branch name.
     */
    public void removeBranch(String[] args) {
        if (args.length != 2) {
            throw new GitletException("wrong # of args");
        }
        String name = args[1];
        if (branchMap.containsKey(name)) {
            if (name.equals(branch)) {
                throw new GitletException("Cannot remove the current branch.");
            } else {
                branchMap.remove(name);
            }
        } else {
            throw new GitletException("branch with that name does not exist.");
        }
    }

    /**
     * equivalent to git reset.
     * @param args funny args
     * @throws IOException
     */
    public void reset(String[] args) throws IOException {
        if (args.length != 2) {
            throw new GitletException("wrong # of args (reset)");
        }
        File f = findCommitById(args[1]);
        if (f.exists()) {
            Commit c = Utils.readObject(f, Commit.class);
            for (String filename : c.trackedList().keySet()) {
                File temp = new File(cwd, filename);
                File src = new File(blobFolder,
                        c.trackedList().get(filename) + ".txt");
                if (temp.exists()) {
                    if (Utils.sha1(Utils.readContentsAsString(temp)).
                            equals(Utils.sha1(
                                    Utils.readContentsAsString(src)))) {
                        temp = null;
                    } else if (!stage.addContains(filename)
                            || !head.trackedList().containsKey(filename)) {
                        throw new GitletException("There is an"
                                + "untracked file in the way; "
                                + "delete it, or add and commit it first.");
                    }
                }
            }
            for (String filename : c.trackedList().keySet()) {
                File src = new File(blobFolder,
                        c.trackedList().get(filename) + ".txt");
                File rec = new File(cwd, filename);
                if (!rec.exists()) {
                    rec.createNewFile();
                }
                Utils.writeContents(rec, Utils.readContentsAsString(src));
            }
            stage.reset();
            head = c;
            branchMap.put(branch, c.hash());
        } else {
            throw new GitletException("No commit with that id exists.");
        }

    }

    /**
     * Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory,
     * overwriting the versions of the files
     * that are already there if they exist.
     * Also, at the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not present
     * in the checked-out branch are deleted.
     * The staging area is cleared,
     * unless the checked-out branch
     * is the current branch (see Failure cases below).
     * @param args
     */
    public void checkoutBranch(String[] args) throws IOException {
        String newBranch = args[1];
        if (!branchMap.containsKey(newBranch)) {
            throw new GitletException("No such branch exists.");
        } else if (newBranch.equals(branch)) {
            throw new
                    GitletException("No need to checkout the current branch.");
        }
        String fn = branchMap.get(newBranch);
        File k = new File(commitFolder, fn + ".txt");
        Commit c = Utils.readObject(k, Commit.class);
        for (String filename : c.trackedList().keySet()) {
            File temp = new File(cwd, filename);
            if (temp.exists()
                    && !(stage.addContains(filename)
                    || head.trackedList().containsKey(filename))) {
                throw new GitletException("There is an untracked file"
                        + " in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (String filename : head.trackedList().keySet()) {
            if (!c.trackedList().containsKey(filename)) {
                File f = new File(cwd, filename);
                if (f.exists()) {
                    Utils.restrictedDelete(f);
                }
            }
        }
        for (String filename : stage.getToAdd().keySet()) {
            if (!c.trackedList().containsKey(filename)) {
                File f = new File(cwd, filename);
                if (f.exists()) {
                    Utils.restrictedDelete(f);
                }
            }
        }
        for (String filename : c.trackedList().keySet()) {
            File src = new File(blobFolder,
                    c.trackedList().get(filename) + ".txt");
            File rec = new File(cwd, filename);
            if (!rec.exists()) {
                rec.createNewFile();
            }
            Utils.writeContents(rec,
                    Utils.readContentsAsString(src));
        }
        stage.reset();
        branch = newBranch;
        head = c;
    }

    /**
     * director function for checkout.
     * @param args string list
     */
    public void checkoutMain(String[] args) throws IOException {
        if (args.length == 3) {
            checkoutLast(args);
        } else if (args.length == 4) {
            checkout(args);
        } else if (args.length == 2) {
            checkoutBranch(args);
        }
    }
}
