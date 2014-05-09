package nl.tudelft.sps.app.data;

import android.provider.BaseColumns;

import java.util.List;

/**
 * Defines a class that can be stored in the db
 */
public interface DAO extends BaseColumns {
    /**
     * Save this DOA to the database
     * @return ID of the DAO
     */
    public long save();
}
