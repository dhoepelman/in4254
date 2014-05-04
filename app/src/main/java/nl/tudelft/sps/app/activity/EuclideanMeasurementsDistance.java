package nl.tudelft.sps.app.activity;

import java.lang.Math;

public class EuclideanMeasurementsDistance implements IMeasurementsDistance {

    public double getDistance(IMeasurement measurement, IMeasurement neighborMeasurement) {
        final double[] featuresA = measurement.getFeatureVector();
        final double[] featuresB = neighborMeasurement.getFeatureVector();

        final double[] featuresEucl = new double[featuresA.length];

        for (int i = 0; i < featuresA.length; i++) {
            featuresEucl[i] = Math.sqrt(Math.pow(featuresA[i], 2) + Math.pow(featuresB[i], 2));
        }

        // TODO Haven't thought about how to combine all the individual distances
        return featuresEucl[3];
    }

}
