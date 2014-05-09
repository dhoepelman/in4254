package nl.tudelft.sps.app.activity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

@DatabaseTable(tableName = "act_measurements")
public class Sample {
    @DatabaseField(foreign = true)
    Measurement measurement;
    @DatabaseField
    long timestamp;
    @DatabaseField
    double X;
    @DatabaseField
    double Y;
    @DatabaseField
    double Z;

    public Sample() {
        // For ORMLite
    }

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

    public static DescriptiveStatistics[] toDescriptiveStatistics(Iterable<Sample> samples) {
        DescriptiveStatistics[] ret = new DescriptiveStatistics[] {
                new DescriptiveStatistics(),
                new DescriptiveStatistics(),
                new DescriptiveStatistics()
        };
        for(Sample s : samples) {
            ret[0].addValue(s.X);
            ret[1].addValue(s.Y);
            ret[2].addValue(s.Z);
        }
        return ret;
    }
}
