package com.dianjinshou.integration.asr;

import java.util.List;

public interface AsrClient {

    /**
     * 转写音频文件为带时间戳的段落。
     * @param userId 发起者 userId
     * @param audioFilePath 本地音频文件路径
     */
    List<AsrSegmentResult> transcribe(Long userId, String audioFilePath);

    class AsrSegmentResult {

        private int segmentIndex;
        private String startTime;
        private String endTime;
        private String text;

        public AsrSegmentResult() {
        }

        public AsrSegmentResult(int segmentIndex, String startTime, String endTime, String text) {
            this.segmentIndex = segmentIndex;
            this.startTime = startTime;
            this.endTime = endTime;
            this.text = text;
        }

        public int getSegmentIndex() {
            return segmentIndex;
        }

        public void setSegmentIndex(int segmentIndex) {
            this.segmentIndex = segmentIndex;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
