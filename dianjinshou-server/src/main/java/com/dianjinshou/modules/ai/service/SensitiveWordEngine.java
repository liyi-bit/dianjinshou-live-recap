package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class SensitiveWordEngine {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordEngine.class);

    private final SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private TrieNode root = new TrieNode();
    private final Map<String, SensitiveWordLibrary> wordMetaMap = new HashMap<String, SensitiveWordLibrary>();

    public SensitiveWordEngine(SensitiveWordLibraryMapper sensitiveWordLibraryMapper) {
        this.sensitiveWordLibraryMapper = sensitiveWordLibraryMapper;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        long start = System.currentTimeMillis();
        LambdaQueryWrapper<SensitiveWordLibrary> query = new LambdaQueryWrapper<>();
        query.eq(SensitiveWordLibrary::getIsActive, 1);
        List<SensitiveWordLibrary> words = sensitiveWordLibraryMapper.selectList(query);

        TrieNode newRoot = new TrieNode();
        Map<String, SensitiveWordLibrary> newMeta = new HashMap<String, SensitiveWordLibrary>();
        for (SensitiveWordLibrary w : words) {
            insertWord(newRoot, w.getWord());
            newMeta.put(w.getWord(), w);
        }

        lock.writeLock().lock();
        try {
            root = newRoot;
            wordMetaMap.clear();
            wordMetaMap.putAll(newMeta);
        } finally {
            lock.writeLock().unlock();
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("SensitiveWordEngine reloaded: {} words in {}ms", words.size(), elapsed);
    }

    public List<MatchResult> scan(String text) {
        List<MatchResult> results = new ArrayList<MatchResult>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        lock.readLock().lock();
        try {
            int len = text.length();
            for (int i = 0; i < len; i++) {
                TrieNode node = root;
                for (int j = i; j < len; j++) {
                    char c = text.charAt(j);
                    TrieNode child = node.children.get(c);
                    if (child == null) {
                        break;
                    }
                    node = child;
                    if (node.isEnd) {
                        String word = text.substring(i, j + 1);
                        SensitiveWordLibrary meta = wordMetaMap.get(word);
                        if (meta != null) {
                            MatchResult mr = new MatchResult();
                            mr.setWord(word);
                            mr.setPosition(i);
                            mr.setCategory(meta.getCategory());
                            mr.setRiskLevel(meta.getRiskLevel());
                            mr.setReplacement(meta.getReplacementSuggestion());
                            results.add(mr);
                        }
                        break;
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return results;
    }

    public List<MatchResult> scan(String text, String platform) {
        List<MatchResult> all = scan(text);
        if (platform == null || platform.isEmpty()) {
            return all;
        }
        List<MatchResult> filtered = new ArrayList<MatchResult>();
        lock.readLock().lock();
        try {
            for (MatchResult mr : all) {
                SensitiveWordLibrary meta = wordMetaMap.get(mr.getWord());
                if (meta != null) {
                    String p = meta.getPlatform();
                    if ("all".equals(p) || platform.equals(p)) {
                        filtered.add(mr);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return filtered;
    }

    public int getWordCount() {
        lock.readLock().lock();
        try {
            return wordMetaMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void insertWord(TrieNode root, String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            TrieNode child = node.children.get(c);
            if (child == null) {
                child = new TrieNode();
                node.children.put(c, child);
            }
            node = child;
        }
        node.isEnd = true;
    }

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<Character, TrieNode>();
        boolean isEnd = false;
    }

    public static class MatchResult {
        private String word;
        private int position;
        private String category;
        private int riskLevel;
        private String replacement;

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getRiskLevel() { return riskLevel; }
        public void setRiskLevel(int riskLevel) { this.riskLevel = riskLevel; }

        public String getReplacement() { return replacement; }
        public void setReplacement(String replacement) { this.replacement = replacement; }
    }
}
