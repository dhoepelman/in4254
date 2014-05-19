package nl.tudelft.sps.app.localization;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

import java.util.Collection;
import java.util.Iterator;

/**
 * Exists solely to group wifiresults
 */
public class WifiResultCollection implements Collection<WifiResult> {

    @DatabaseField(generatedId = true)
    long id;

    @ForeignCollectionField(eager = true)
    ForeignCollection<WifiResult> wifiResults;

    @DatabaseField
    Room room;

    @DatabaseField
    long timestamp;

    public WifiResultCollection(long timestamp, Room room) {
        this.timestamp = timestamp;
        this.room = room;
    }

    public WifiResultCollection() {
        // ORMlite
    }

    @Override
    public boolean add(WifiResult object) {
        return wifiResults.add(object);
    }

    @Override
    public boolean addAll(Collection<? extends WifiResult> collection) {
        return wifiResults.addAll(collection);
    }

    @Override
    public void clear() {
        wifiResults.clear();
    }

    @Override
    public boolean contains(Object object) {
        return wifiResults.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return wifiResults.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return wifiResults.isEmpty();
    }

    @Override
    public Iterator<WifiResult> iterator() {
        return wifiResults.iterator();
    }

    @Override
    public boolean remove(Object object) {
        return wifiResults.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return wifiResults.retainAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return wifiResults.retainAll(collection);
    }

    @Override
    public int size() {
        return wifiResults.size();
    }

    @Override
    public Object[] toArray() {
        return wifiResults.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return wifiResults.toArray(array);
    }
}
