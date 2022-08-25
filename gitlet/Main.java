package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Michelle Wu
 */
public class Main {
    /** Current Working Directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /**Main.java
     Entry point of the program; this is where git commands are taken.

     Fields
     Repo r: repo object(s) representing
     current working repo (Expand to support additional ones)
     initialize repo
     @param args
     **/
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        Repo currentRepo = null;
        if (args[0].equals("init")) {
            new Repo();
            return;
        } else {
            currentRepo = Repo.findRepo();
        }
        switch (args[0]) {
        case "add":
            currentRepo.add(args);
            break;
        case "commit":
            currentRepo.makeCommit(args);
            break;
        case "log":
            currentRepo.log();
            break;
        case "global-log":
            currentRepo.globalLog();
            break;
        case "find":
            currentRepo.find(args);
            break;
        case "status":
            currentRepo.status();
            break;
        case "checkout":
            currentRepo.checkoutMain(args);
            break;
        case "rm":
            currentRepo.remove(args);
            break;
        case "rm-branch":
            currentRepo.removeBranch(args);
            break;
        case "merge":
            currentRepo.merge(args);
            break;
        case "reset":
            currentRepo.reset(args);
            break;
        case "branch":
            currentRepo.branch(args);
            break;
        default:
            throw new GitletException("No command with that name exists.");
        }
        currentRepo.saveRepo();
        return;
    }

}
