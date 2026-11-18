package p2pbet.dataaccess.csv;
import p2pbet.dataaccess.IDataStorage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;
@Component
public class CsvDataStorage implements IDataStorage {
    private final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();
    private ReentrantLock getLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
    }
    @Override
    public void writeLines(String filePath, List<String> lines) {
        ReentrantLock lock = getLock(filePath);
        lock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing to CSV: " + filePath, e);
        } finally {
            lock.unlock();
        }
    }
    @Override
    public List<String> readLines(String filePath) {
        ReentrantLock lock = getLock(filePath);
        lock.lock();
        List<String> lines = new ArrayList<>();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                return lines;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from CSV: " + filePath, e);
        } finally {
            lock.unlock();
        }
        return lines;
    }
}
