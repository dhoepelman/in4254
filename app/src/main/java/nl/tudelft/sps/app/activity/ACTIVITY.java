package nl.tudelft.sps.app.activity;

import android.provider.BaseColumns;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
* Enum for different type of activities
*/
public enum ACTIVITY {
    Sitting,
    Walking,
    Running,
    Elevator,
    Unknown;
}
