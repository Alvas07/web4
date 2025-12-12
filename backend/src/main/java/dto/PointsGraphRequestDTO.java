package dto;

import java.util.List;

public record PointsGraphRequestDTO(List<PointDTO> points,
                                    double graphXMin, double graphXMax,
                                    double graphYMin, double graphYMax) {}
