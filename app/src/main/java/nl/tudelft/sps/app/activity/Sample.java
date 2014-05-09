package nl.tudelft.sps.app.activity;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

import nl.tudelft.sps.app.data.DAO;

public class Sample implements DAO {
    public static final String TABLE_NAME = "act_samples";
    public static final String COLUMN_MEASUREMENT = "mid";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_X = "X";
    public static final String COLUMN_Y = "Y";
    public static final String COLUMN_Z = "Z";

    public final Measurement measurement;
    public final long timestamp;
    public final double X;
    public final double Y;
    public final double Z;

    public Sample(Measurement m, long timestamp, double x, double y, double z) {
        this.measurement = m;
        this.timestamp = timestamp;
        X = x;
        Y = y;
        Z = z;
    }

    public Sample(Measurement measurement, long timestamp, float[] values) {
        this(measurement, timestamp, values[0], values[1], values[2]);
    }

    public static DescriptiveStatistics[] toDescriptiveStatistics(List<Sample> samples) {
        return null;
    }

    public static List<Sample> fromDescriptiveStatistics(DescriptiveStatistics[] windows) {
        return null;
    }
}
