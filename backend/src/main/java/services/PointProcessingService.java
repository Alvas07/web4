package services;

import dto.CheckResultDTO;
import dto.HistoryEntryDTO;
import dto.PointDTO;
import entities.HistoryEntry;
import entities.User;
import ejb.HistoryBean;
import ejb.PointCheckBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;

@Stateless
public class PointProcessingService {
    @EJB
    private PointCheckBean pointCheckBean;
    
    @EJB
    private HistoryBean historyBean;

    public List<HistoryEntryDTO> processPointsFromForm(List<PointDTO> points, User user) {
        long startTime = System.nanoTime();
        List<CheckResultDTO> results = pointCheckBean.checkPointsFromForm(points);
        double execTime = (System.nanoTime() - startTime) / 1e6;
        return processResults(results, user, execTime);
    }

    public List<HistoryEntryDTO> processPointsFromGraph(List<PointDTO> points, double graphXMin, double graphXMax, 
                                                       double graphYMin, double graphYMax, User user) {
        long startTime = System.nanoTime();
        List<CheckResultDTO> results = pointCheckBean.checkPointsFromGraph(points, graphXMin, graphXMax, graphYMin, graphYMax);
        double execTime = (System.nanoTime() - startTime) / 1e6;
        return processResults(results, user, execTime);
    }
    
    private List<HistoryEntryDTO> processResults(List<CheckResultDTO> results, User user, double execTime) {
        List<HistoryEntry> entries = results.stream()
                .map(r -> {
                    HistoryEntry entry = new HistoryEntry(
                            new entities.Point(r.point().x(), r.point().y(), r.point().r()),
                            r.hit(),
                            user
                    );
                    entry.setExecTime(execTime);
                    return entry;
                })
                .toList();
        
        historyBean.save(entries);
        
        return entries.stream()
                .map(e -> new HistoryEntryDTO(
                        new PointDTO(e.getPoint().getX(), e.getPoint().getY(), e.getPoint().getR()),
                        e.isHit(),
                        user.getUsername(),
                        e.getCreatedAt(),
                        e.getExecTime()
                ))
                .toList();
    }
}

