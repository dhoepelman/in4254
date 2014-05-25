package nl.tudelft.sps.app.localization;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class FFTResult {

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField
    private double value;

    public FFTResult(double value) {
        this.value = value;
    }

    public FFTResult() {
        // This constructor solely exists to make ORMLite happy
    }

    @Override
    public String toString() {
        return "FFTResult{" +
               "value=" + value +
               "}";
    }

}
