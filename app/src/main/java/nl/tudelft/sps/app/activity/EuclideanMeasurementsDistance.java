package nl.tudelft.sps.app.activity;

import java.lang.Math;

public class EuclideanMeasurementsDistance implements IMeasurementsDistance {

    public double getDistance(IMeasurement measurement, IMeasurement neighborMeasurement) {
        return getDistance(measurement.getFeatureVector(), neighborMeasurement.getFeatureVector());
    }

    @Override
    public double getDistance(double[] featureVector, double[] neighborFeatureVector) {
        final double[] featuresEucl = new double[featureVector.length];

        // TODO: Calculate this more efficiently, see https://en.wikipedia.org/wiki/Euclidean_distance. There's probably also a library
        for (int i = 0; i < featureVector.length; i++) {
            featuresEucl[i] = Math.sqrt(Math.pow(featureVector[i], 2) + Math.pow(neighborFeatureVector[i], 2));
        }

        // TODO Haven't thought about how to combine all the individual distances
        return featuresEucl[3];
    }

}
