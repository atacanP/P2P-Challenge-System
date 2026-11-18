package p2pbet.dataaccess.csv;
import p2pbet.dataaccess.IDataStorage;
import p2pbet.dataaccess.IRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public abstract class AbstractCsvRepository<T, ID> implements IRepository<T, ID> {
    protected final ConcurrentHashMap<ID, T> cache = new ConcurrentHashMap<>();
    protected final IDataStorage dataStorage;
    protected final String csvPath;
    protected final String header;
    public AbstractCsvRepository(IDataStorage dataStorage, String csvPath, String header) {
        this.dataStorage = dataStorage;
        this.csvPath = csvPath;
        this.header = header;
        loadFromCsv();
    }
    protected abstract T mapFromString(String line);
    protected abstract String mapToString(T entity);
    protected abstract ID getId(T entity);
    private void loadFromCsv() {
        List<String> lines = dataStorage.readLines(csvPath);
        boolean hasHeader = false;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            if (line.startsWith(header.split(",")[0])) {
                hasHeader = true;
                continue;
            }
            T entity = mapFromString(line);
            if (entity != null) {
                cache.put(getId(entity), entity);
            }
        }
        if (!hasHeader) {
            syncToCsv();
        }
    }
    protected void syncToCsv() {
        List<String> lines = cache.values().stream()
                .map(this::mapToString)
                .collect(Collectors.toList());
        lines.add(0, header);
        dataStorage.writeLines(csvPath, lines);
    }
    @Override
    public void save(T entity) {
        cache.put(getId(entity), entity);
        syncToCsv();
    }
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(cache.get(id));
    }
    @Override
    public List<T> findAll() {
        return new ArrayList<>(cache.values());
    }
    @Override
    public void delete(ID id) {
        if (cache.remove(id) != null) {
            syncToCsv();
        }
    }
}
