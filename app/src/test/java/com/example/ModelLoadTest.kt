package com.example

import org.junit.Test
import org.junit.Assert.assertTrue
import org.pytorch.LiteModuleLoader
import java.io.File

class ModelLoadTest {
    @Test
    fun testLoad() {
        val file = File("src/main/assets/chicken_model_lite.ptl")
        println(file.absolutePath)
        assertTrue("Model file should exist in assets", file.exists())
        try {
            val module = LiteModuleLoader.load(file.absolutePath)
            println("Loaded successfully!")
        } catch (e: UnsatisfiedLinkError) {
            println("PyTorch native JNI library not available on host JVM (only available on Android device/emulator runtime)")
        }
    }
}
