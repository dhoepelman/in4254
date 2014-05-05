package nl.tudelft.sps.app.activity;

/**
 * Created by vm on 5/2/14.
 */
public interface IMeasurementsDistance {

    public double getDistance(IMeasurement measurement, IMeasurement neighborMeasurement);
    public double getDistance(double[] featureVector, double[] neighborFeatureVector);

}
