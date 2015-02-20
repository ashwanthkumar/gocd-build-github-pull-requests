package in.ashwanthkumar.gocd.github.model;

public class ModifiedFile {
    private String fileName;
    private String action;

    public ModifiedFile(String fileName, String action) {
        this.fileName = fileName;
        this.action = action;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "ModifiedFile{" +
                "fileName='" + fileName + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
