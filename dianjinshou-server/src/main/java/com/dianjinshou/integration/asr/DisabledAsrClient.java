package com.dianjinshou.integration.asr;

import java.util.List;

public class DisabledAsrClient implements AsrClient {

    @Override
    public List<AsrSegmentResult> transcribe(Long userId, String audioFilePath) {
        throw new UnsupportedOperationException("服务端 ASR 已禁用，请使用桌面端本机 ASR 生成逐字稿");
    }
}
