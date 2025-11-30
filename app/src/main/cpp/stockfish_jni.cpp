#include <jni.h>
#include <string>
#include <thread>
#include <chrono>

// Имитация движка (пока что)
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_chessmentor_data_engine_StockfishEngine_nativeGetBestMove(
        JNIEnv* env,
        jobject /* this */,
        jstring fen) {

    // Имитируем "думание"
    std::this_thread::sleep_for(std::chrono::milliseconds(500));

    // Возвращаем фейковый ход
    return env->NewStringUTF("bestmove e2e4");
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_chessmentor_data_engine_StockfishEngine_nativeInit(
        JNIEnv* env,
jobject /* this */) {
// Инициализация
}