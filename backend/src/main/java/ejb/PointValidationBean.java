package ejb;

import dto.PointDTO;
import jakarta.ejb.Stateless;

import java.util.Arrays;

@Stateless
public class PointValidationBean {
    private static final double[] VALID_X_VALUES = {-4, -3, -2, -1, 0, 1, 2, 3, 4};
    private static final double Y_MIN = -5;
    private static final double Y_MAX = 3;
    private static final double[] VALID_R_VALUES = {-4, -3, -2, -1, 0, 1, 2, 3, 4};
    private static final double EPSILON = 1e-6;


    public boolean validateForm(PointDTO point) {
        if (point == null) return false;

        boolean validX = Arrays.stream(VALID_X_VALUES)
                .anyMatch(x -> Math.abs(point.x() - x) <= EPSILON);

        boolean validY = point.y() >= Y_MIN - EPSILON && point.y() <= Y_MAX + EPSILON;

        boolean validR = Arrays.stream(VALID_R_VALUES)
                .anyMatch(r -> Math.abs(point.r() - r) <= EPSILON);

        return validX && validY && validR;
    }

    public boolean validateGraphClick(PointDTO point, double graphXMin, double graphXMax, double graphYMin, double graphYMax) {
        if (point == null) return false;

        boolean validX = point.x() >= graphXMin - EPSILON && point.x() <= graphXMax + EPSILON;

        boolean validY = point.y() >= graphYMin - EPSILON && point.y() <= graphYMax + EPSILON;

        boolean validR = Arrays.stream(VALID_R_VALUES)
                .anyMatch(r -> Math.abs(point.r() - r) <= EPSILON);

        return validX && validY && validR;
    }
}
