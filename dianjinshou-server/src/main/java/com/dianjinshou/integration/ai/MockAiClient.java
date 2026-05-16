package com.dianjinshou.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MockAiClient implements AiAnalysisClient {

    private static final String[] CATEGORIES = {
            "话题话术", "行动指令", "留人话术", "塑品话术", "互动话术",
            "促单话术", "逼单话术", "福利话术", "开场话术", "人设话术"
    };

    private static final Logger log = LoggerFactory.getLogger(MockAiClient.class);

    @Override
    public AiAnalysisResult analyze(Long userId, String asrText, String industry, String promptTemplate) {
        log.info("Mock AI analyzing: industry={}, textLength={}", industry,
                asrText != null ? asrText.length() : 0);

        AiAnalysisResult result = new AiAnalysisResult();
        result.setAiResult("{\"dimensions\":{" +
                "\"opening\":{\"score\":78,\"comment\":\"开场有效但可以更加吸引人\"}," +
                "\"retention\":{\"score\":82,\"comment\":\"留人话术到位\"}," +
                "\"product\":{\"score\":75,\"comment\":\"产品塑造需要加强\"}," +
                "\"promotion\":{\"score\":80,\"comment\":\"促单节奏良好\"}," +
                "\"interaction\":{\"score\":70,\"comment\":\"互动频率可以提高\"}," +
                "\"expression\":{\"score\":85,\"comment\":\"表达力强\"}," +
                "\"compliance\":{\"score\":90,\"comment\":\"合规意识好\"}," +
                "\"fanClub\":{\"score\":65,\"comment\":\"粉丝团引导不足\"}," +
                "\"privateDomain\":{\"score\":60,\"comment\":\"私域引导缺失\"}," +
                "\"persona\":{\"score\":72,\"comment\":\"人设较清晰\"}," +
                "\"closing\":{\"score\":78,\"comment\":\"收尾平稳\"}," +
                "\"overall\":{\"score\":76,\"comment\":\"整体表现中等偏上\"}}}");

        result.setAiDiagnosis("{\"strengths\":[\"表达力强\",\"合规意识好\",\"留人话术到位\"]," +
                "\"weaknesses\":[\"互动频率低\",\"私域引导缺失\",\"粉丝团引导不足\"]," +
                "\"suggestions\":[\"增加互动话术频率\",\"添加私域引导环节\",\"优化粉丝团入会引导\"]}");

        result.setKeywordSummary("{\"operational\":{\"互动力\":[\"点赞\",\"关注\",\"评论\"],\"促单力\":[\"下单\",\"拍\",\"抢\"]}," +
                "\"sensitive\":{\"绝对化\":[\"最\",\"第一\"]}}");

        result.setContentCompass("{\"contentRatio\":{\"score\":85,\"items\":[" +
                "{\"name\":\"话题话术\",\"percent\":18.5}," +
                "{\"name\":\"行动指令\",\"percent\":8.2}," +
                "{\"name\":\"留人话术\",\"percent\":12.3}," +
                "{\"name\":\"塑品话术\",\"percent\":15.6}," +
                "{\"name\":\"互动话术\",\"percent\":10.1}," +
                "{\"name\":\"促单话术\",\"percent\":14.8}," +
                "{\"name\":\"逼单话术\",\"percent\":6.5}," +
                "{\"name\":\"福利话术\",\"percent\":5.2}," +
                "{\"name\":\"开场话术\",\"percent\":4.8}," +
                "{\"name\":\"人设话术\",\"percent\":4.0}]}," +
                "\"liveRhythm\":{\"score\":78,\"items\":[" +
                "{\"name\":\"开场暖场\",\"percent\":8.5}," +
                "{\"name\":\"产品讲解\",\"percent\":28.3}," +
                "{\"name\":\"互动环节\",\"percent\":15.2}," +
                "{\"name\":\"促单逼单\",\"percent\":18.6}," +
                "{\"name\":\"福利发放\",\"percent\":7.4}," +
                "{\"name\":\"过渡衔接\",\"percent\":12.8}," +
                "{\"name\":\"闲聊休息\",\"percent\":9.2}]}}");

        result.setSummary("本场直播整体表现中等偏上，表达力和合规性是主要优势。" +
                "建议重点提升互动频率和私域引导能力。");

        result.setConsumedChars(asrText != null ? asrText.length() : 0);

        return result;
    }

    @Override
    public String optimizeText(Long userId, String asrText) {
        log.info("Mock AI optimizing text: length={}", asrText != null ? asrText.length() : 0);
        if (asrText == null || asrText.trim().isEmpty()) return "";

        // Mock: clean up filler words, add paragraph breaks
        String optimized = asrText
                .replaceAll("嗯+", "")
                .replaceAll("啊+", "")
                .replaceAll("那个", "")
                .replaceAll("就是说", "")
                .replaceAll("然后呢", "")
                .replaceAll("对吧", "")
                .replaceAll("\\s{2,}", " ")
                .trim();

        // Add paragraph breaks every ~200 chars
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (char c : optimized.toCharArray()) {
            sb.append(c);
            count++;
            if (count > 200 && (c == '。' || c == '！' || c == '？')) {
                sb.append("\n\n");
                count = 0;
            }
        }
        return sb.toString();
    }

    @Override
    public List<String> classifyParagraphs(Long userId, List<String> paragraphTexts) {
        log.info("Mock AI classifying {} paragraphs", paragraphTexts.size());
        List<String> results = new ArrayList<>();
        for (int i = 0; i < paragraphTexts.size(); i++) {
            String text = paragraphTexts.get(i).toLowerCase();
            // Simple keyword-based mock classification
            if (i == 0) {
                results.add("开场话术");
            } else if (text.contains("点赞") || text.contains("关注") || text.contains("评论") || text.contains("扣")) {
                results.add("互动话术");
            } else if (text.contains("下单") || text.contains("拍") || text.contains("买") || text.contains("付款")) {
                results.add("促单话术");
            } else if (text.contains("倒计时") || text.contains("最后") || text.contains("赶紧") || text.contains("马上")) {
                results.add("逼单话术");
            } else if (text.contains("福利") || text.contains("优惠") || text.contains("红包") || text.contains("免费")) {
                results.add("福利话术");
            } else if (text.contains("停留") || text.contains("别走") || text.contains("等一下") || text.contains("新来的")) {
                results.add("留人话术");
            } else if (text.contains("品质") || text.contains("材质") || text.contains("功效") || text.contains("成分")) {
                results.add("塑品话术");
            } else if (text.contains("我") && (text.contains("经验") || text.contains("年") || text.contains("专业"))) {
                results.add("人设话术");
            } else {
                results.add(CATEGORIES[i % CATEGORIES.length]);
            }
        }
        return results;
    }
}
