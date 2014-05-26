package nl.tudelft.sps.app.localization;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class FFTResult {

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField
    private double valueX;

    @DatabaseField
    private double valueY;

    @DatabaseField
    private double valueZ;

    public FFTResult(double[] value) {
        this.valueX = value[0];
        this.valueY = value[1];
        this.valueZ = value[2];
    }

    public FFTResult() {
        // This constructor solely exists to make ORMLite happy
    }

    @Override
    public String toString() {
        return "FFTResult{" +
               "valueX=" + valueX +
               "valueY=" + valueY +
               "valueZ=" + valueZ +
               "}";
    }

}
