package shapes.templates;

import dto.PointDTO;

public interface Shape {
    boolean contains(PointDTO p);
}
