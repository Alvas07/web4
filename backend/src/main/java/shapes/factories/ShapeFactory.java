package shapes.factories;

import shapes.templates.Shape;

public interface ShapeFactory {
    Shape create(double r);
}
