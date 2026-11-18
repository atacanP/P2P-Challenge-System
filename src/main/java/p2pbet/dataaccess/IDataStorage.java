package p2pbet.dataaccess;
import java.util.List;
public interface IDataStorage {
    void writeLines(String path, List<String> lines);
    List<String> readLines(String path);
}
