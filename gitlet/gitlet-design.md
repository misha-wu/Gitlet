

# Gitlet Design Document

**Name**: Michelle Wu, Jessica Fan

## Classes and Data Structures

### Main.java
Entry point of the program; this is where git commands are taken.

#### Fields
1. `Repo` r: repo object(s) representing current working repo (Expand to support additional ones)
2. initialize repo

### Staging.java
This class represents the staging area, which holds files that have been added but not yet commit-ed.

#### Fields and Methods
1. private TreeMap stagingMap: a `TreeMap` of blob objects corresponding to files that have been added; key: filename, hash(blob contents); you can get the actual file contents by going to /blobs/hash(blob contents).txt.
2. public TreeMap getMap: get staging map

### Commit.java
- implements `Serializable`

#### Fields
1. private String log: the hashed id (to be used in printout) as well as the key
2. public Commit parent: the parent commit; the commit right before the current commit
3. fromFile(): reads from a File and makes a new Commit object.
4. saveFile(): saves a Commit to a new file.
4. DateTime time: system time during commit
5. `HashMap<Blob>` blobs: list of blob data in this commit; key: blob name (ie "shark" if shark.txt), value: hashed value of the blob **object**

### Blob.java
This class represents the data of each file being committed.
Implements `Comparable`

#### Fields and Methods
1. private String contents: contents of the String; calls `readContentsAsString`
2. File blobFolder: parent folder of the blob
3. fromFile(): reads from a File and makes a new Blob object.
4. saveFile(): saves a Blob to a new file.
5. `override` compareTo(): compares one Blob's contents to another, and returns the differences
6. `override` toString()`: returns contents.

### Repo.java
This class represents the data of each file being committed.

#### Fields and Methods
1. static final File CWD: A pointer to the current working directory of the program.
2. static final File gitlet: A pointer to the `gitlet` directory in the current working directory
3. private `Commit` first: A treemap of all commits; first empty commit
4. private `Commit` head: A pointer to the current commit object of the current branch
5. private Map branches: a map of branches of commits. key: branch name, value: current commit id
7. private `StagingArea` s: staging area
8. public void `init()`: sets up persistence, makes first commit, etc.
9. public void `setupPersistence()`: sets up CWD to align by gitlet organization; creates gitlet folder if not already there
10. public void `add(String name)`: equiv to git add `filename`
11. public void `add()`: equiv to git add -A
12. public void `merge()`: merge branches.
13. public void `remove(String name)`: equiv to git rm `filename`
14. public void `checkout(args)`: equiv to git checkout `filename`, `hash`, etc. revert to listed branch and update head to this branch

## Algorithms

### Main.java
1. main(String[] args): entry point; sets up a repo object, and calls functions on repo depending on the arguments in terminal.
### Repo.java
1. setupPersistence(): creates directories for folder for .gitlet, .commits if not yet existing
2. add(): adds all files that have been changed but not yet staged to `StagingArea`. Check for difference by comparing hashed versions to last commit.
3. add(String filename). searches for filename.txt in CWD, compares to last commit for difference and adds to `StagingArea` if different.
4. remove(String filename). searches for filename.txt in CWD, removes file?? idk
5. commit(): makes a new commit object, copies Map in StagingArea to Map in commit field ("blobs"), assigns parents etc., serialize commit
6. checkout(String args). searches for filename.txt or gitlet state during `args` identifier commit. Reverts CWD files to this commit and updated head to point to this commit.
7. merge(String args): merge branches idk
8. validateNumArgs(String cmd, String[] args, int n): (	referenced from lab 12) checks # of args and throws error if not right.

## Persistence

### add -A or add (filename)
- When a file is changed and tracked, a blob file is created in /gitlet/blobs whose filename is hash(blob).

### commit
- Each time we call commit, we create a new commit object and serialize it under /gitlet/commit


## Design Diagram  

