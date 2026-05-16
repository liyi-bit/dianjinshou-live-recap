package com.dianjinshou.modules.recap.vo;

import java.util.List;
import java.util.Map;

public class KeywordListVO {

    private List<KeywordVO> items;
    private Map<String, Integer> stats;
    private long total;

    public static KeywordListVO of(List<KeywordVO> items, Map<String, Integer> stats, long total) {
        KeywordListVO vo = new KeywordListVO();
        vo.setItems(items);
        vo.setStats(stats);
        vo.setTotal(total);
        return vo;
    }

    public List<KeywordVO> getItems() {
        return items;
    }

    public void setItems(List<KeywordVO> items) {
        this.items = items;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public void setStats(Map<String, Integer> stats) {
        this.stats = stats;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
