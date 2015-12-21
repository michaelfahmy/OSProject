package sfe.os;
import apps.TextEditor;
import apps.ImageViewer;
import apps.MyMedia;
import apps.WebBrowser;
import java.io.*;
import java.util.LinkedList;


class Directory implements Serializable {

    private String name, path, realPath;
    private Folder parent;
    boolean isHidden = false;

    public Directory(String name, String path, Folder parent) {
        this.name = name;
        this.path = path;
        this.parent = parent;
    }
    boolean isHidden() {
        return isHidden;
    }
    void setHidden() {
        isHidden = true;
    }
    public void setRealPath(String realPath) { this.realPath = realPath; }
    public String getRealPath() {
        return this.realPath;
    }
    public String getPath() {
        return this.path;
    }
    public void setPath(String path) { this.path = path; }
    public Folder getParent() { return this.parent; }
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
}

class Folder extends Directory {

    LinkedList<Directory> children;

    public Folder(String name, String path, Folder parent) {
        super(name, path, parent);
        children = new LinkedList<>();
    }

    public LinkedList<Directory> getChildren() {
        return children;
    }
}

class File extends Directory {

    String permission;
    String extension;

    public File(String name, String extension, String path, Folder parent, String permission) {
        super(name + "." + extension, path, parent);
        this.permission = permission;
        this.extension = extension;
    }
}

public class FileSystem {

    private static final String COPY_PROCESS = "copy";
    private static final String CUT_PROCESS = "cut";

    private Folder root, currentFolder;
    private Directory selected = null;
    Directory toBePasted;
    String whichProcess;

    public FileSystem() {
        root = new Folder("root", "", null);
        currentFolder = root;
        Folder storage = newFolder("home");
        storage.setHidden();
        this.seeds(storage, "src/storage");
        this.retrieve();
    }
    public Folder getRoot() {
        return this.root;
    }
    public void select(Directory selected) {
        this.selected = selected;
    }

    public Directory getSelected() {
        return selected;
    }

    public Folder getCurrentFolder() {
        return currentFolder;
    }

    Folder newFolder(String name) {
        String path = this.currentFolder.getPath() + "/" + name;
        Folder child = new Folder(name, path, this.currentFolder);
        this.currentFolder.children.add(child);
        return child;
    }

    File newFile(String name, String ext, String permission) {
        String path = this.currentFolder.getPath() + "/" + name + ext;
        File child = new File(name, ext, path, this.currentFolder, permission);
        this.currentFolder.children.add(child);
        return child;
    }

    void rename(Directory toBeRenamed, String name) {
        for (int i = 0; i < this.currentFolder.children.size(); ++i) {
            if (toBeRenamed == this.currentFolder.children.get(i)) {
                this.currentFolder.children.get(i).setName(name);
                break;
            }
        }
    }

    void delete(Directory toBeDeleted) {
        for (int i = 0; i < this.currentFolder.children.size(); ++i) {
            if (toBeDeleted == this.currentFolder.children.get(i)) {
                this.currentFolder.children.remove(i);
                break;
            }
        }
    }

    void open(Directory toBeOpened) {
        for (int i = 0; i < this.currentFolder.children.size(); ++i) {
            if (toBeOpened == this.currentFolder.children.get(i)) {
                if (toBeOpened instanceof Folder) {
                    this.currentFolder = (Folder) toBeOpened;
                } else {
                    String pth = toBeOpened.getRealPath();
                    //System.out.println(pth);
                    int idx = pth.indexOf("/storage");
                    pth = pth.substring(idx);
                    pth = pth.replaceAll("%20", " ");
                    switch (((File) toBeOpened).extension) {
                        case "txt":
                            System.out.println("Opening text editor");
                            new TextEditor();
                            break;
                        case "jpg":
                            System.out.println("Opening image viewer");
                            new ImageViewer(pth);
                            break;
                        case "mp3":
                            System.out.println("Opening music player");
                            new MyMedia(pth);
                            break;
                        case "mp4":
                            System.out.println("Opening video player");
                            new MyMedia(pth);
                            break;
                        case "pdf":
                            System.out.println("Opening pdf viewer");
//                            new PDFViewer(toBeOpened.getRealPath());
                            break;
                        case "html":
                            System.out.println("Opening browser");
                            new WebBrowser(toBeOpened.getRealPath());
                            break;
                    }
                }
            }
        }
    }

    void back() {
        this.currentFolder = this.currentFolder.getParent() != null ? this.currentFolder.getParent() : this.root;
    }

    void copy(Directory toBeCopied) {
        for (int i = 0; i < this.currentFolder.children.size(); ++i) {
            if (toBeCopied == this.currentFolder.children.get(i)) {
                this.whichProcess = COPY_PROCESS;
                this.toBePasted = this.currentFolder.children.get(i);
                break;
            }
        }
    }

    void cut(Directory toBeCutted) {
        for (int i = 0; i < this.currentFolder.children.size(); ++i) {
            if (toBeCutted == this.currentFolder.children.get(i)) {
                this.whichProcess = CUT_PROCESS;
                this.toBePasted = this.currentFolder.children.get(i);
                break;
            }
        }
    }

    void paste() {
        if (this.whichProcess.equals(CUT_PROCESS)) {
            for (int i = 0; i < this.toBePasted.getParent().children.size(); i++) {
                if (toBePasted.getParent().children.get(i) == toBePasted) {
                    toBePasted.getParent().children.remove(i);
                    break;
                }
            }
        }
        toBePasted.setPath(this.currentFolder.getPath() + "/" + toBePasted.getName());
        this.currentFolder.children.add(toBePasted);
    }

    void store() {
        String address = "data.txt";
        ObjectOutputStream fileSystemData;
        try {
            fileSystemData = new ObjectOutputStream(new FileOutputStream(address));
            fileSystemData.writeObject(root);
            fileSystemData.close();
        } catch (IOException e) {
            System.out.println("store(): " + e.toString());
        }
    }

    void retrieve() {
        String address = "data.txt";
        ObjectInputStream fileSystemData;
        try {
            fileSystemData = new ObjectInputStream(new FileInputStream(address));
            Folder tmp = (Folder) fileSystemData.readObject();
            fileSystemData.close();
            this.root = tmp;
            this.currentFolder = tmp;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("retrieve(): " + e.toString());
        }
    }

    void seeds(Folder currPos, String path) {
        String name, extension, permission = "";
        java.io.File resDir = (new java.io.File(path));
        for(java.io.File currFile: resDir.listFiles()) {
            if(currFile.isDirectory()) {
                name = currFile.getName();
                Folder folder = new Folder(name, currPos.getPath() + "/" + name, currPos);
                folder.setRealPath(currFile.getPath());
                currPos.children.add(folder);
                seeds(folder, currFile.getPath());
            }else {
                name = currFile.getName().substring(0, currFile.getName().indexOf('.'));
                extension = currFile.getName().substring(currFile.getName().indexOf('.') + 1);
                permission = extension.equals(".html") ? "r" : "r/w";
                File fle = new File(name, extension, currPos.getPath() + "/" + name + "." + extension,currPos, permission);
                fle.setRealPath(currFile.toURI().toString());
                currPos.children.add(fle);
            }
        }
    }

    void printAll() {
        printAll(this.root, 0);
    }

    void printAll(Directory current, int cnt) {
        if (current == null) {
            return;
        }
        int t = cnt;
        while (t-- > 0) {
            System.out.print("-");
        }
        System.out.println(current.getName());
        if (current instanceof File) {
            return;
        }
        for (int i = 0; i < ((Folder) current).children.size(); ++i) {
            printAll(((Folder) current).children.get(i), cnt + 2);
        }
    }
}
