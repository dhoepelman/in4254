package nl.tudelft.sps.app.activity;

import java.util.List;
import java.util.Random;

/**
 * A classifier that just returns a Random activity for testing
 */
public class RandomClassifier implements IClassifier {

    private final Random r = new Random();

    @Override
    public ACTIVITY classify(IMeasurement m) {
        if(m.isValid()) {
            return ACTIVITY.UNKNOWN;
        } else {
            return ACTIVITY.values()[r.nextInt(ACTIVITY.values().length)];
        }
    }

    @Override
    public void train(ACTIVITY a, IMeasurement m) {
        // Philosoraptor: can you train randomness?
    }

    @Override
    public void train(List<TrainingPoint> trainingPoints) {

    }

    public boolean isTrained() {
        return true;
    }
}
