package nl.tudelft.sps.app.activity;

import java.lang.Math;

public class EuclideanMeasurementsDistance implements IMeasurementsDistance {

    public double getDistance(IMeasurement measurement, IMeasurement neighborMeasurement) {
        final double a = measurement.getFeatureVector()[3];
        final double b = neighborMeasurement.getFeatureVector()[3];
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }

}
