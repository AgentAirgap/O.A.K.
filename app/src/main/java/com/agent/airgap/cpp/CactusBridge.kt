package com.agent.airgap.cpp

object CactusBridge {
    init {
        System.loadLibrary("cactus_agent")
    }

    external fun processAudio(pcmData: ByteArray): String
}