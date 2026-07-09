#include <jni.h>
#include <string>
#include <vector>
#include <string_view>
#include <android/log.h>

#define LOG_TAG "NativePlaylist"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace iptv {

struct Channel {
    std::string name;
    std::string url;
    std::string logo;
};

class M3UParser {
public:
    std::vector<Channel> parse(std::string_view input) {
        std::vector<Channel> channels;
        channels.reserve(1024); // pre-allocate for large playlists

        size_t pos = 0;
        const size_t len = input.size();

        while (pos < len) {
            // skip to next line
            size_t lineEnd = input.find('\n', pos);
            if (lineEnd == std::string_view::npos) lineEnd = len;
            std::string_view line = input.substr(pos, lineEnd - pos);
            pos = lineEnd + 1;

            // trim whitespace
            size_t start = line.find_first_not_of(" \t\r");
            if (start == std::string_view::npos) continue;
            size_t end = line.find_last_not_of(" \t\r");
            line = line.substr(start, end - start + 1);

            if (line.empty() || line[0] != '#') {
                // treat as URL
                if (!channels.empty() && channels.back().url.empty()) {
                    channels.back().url = std::string(line);
                }
                continue;
            }

            if (line.rfind("#EXTINF:", 0) == 0) {
                Channel ch;
                ch.name = extractName(line);
                ch.logo = extractLogo(line);
                channels.push_back(ch);
            }
        }

        // cleanup: remove empty entries
        channels.erase(
            std::remove_if(channels.begin(), channels.end(),
                [](const Channel& ch) { return ch.url.empty(); }),
            channels.end()
        );

        return channels;
    }

private:
    std::string extractName(std::string_view line) {
        size_t comma = line.rfind(',');
        if (comma != std::string_view::npos && comma + 1 < line.size()) {
            std::string name = std::string(line.substr(comma + 1));
            // trim leading spaces
            size_t s = name.find_first_not_of(" \t");
            if (s != std::string_view::npos) name = name.substr(s);
            return name;
        }
        return "Unknown";
    }

    std::string extractLogo(std::string_view line) {
        size_t logoPos = line.find("tvg-logo=\"");
        if (logoPos != std::string_view::npos) {
            size_t start = logoPos + 10;
            size_t end = line.find('"', start);
            if (end != std::string_view::npos && end > start) {
                return std::string(line.substr(start, end - start));
            }
        }
        return "";
    }
};

} // namespace iptv

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_iptvplayer_NativePlaylistParser_nativeCreateParser(JNIEnv *env, jobject thiz) {
    iptv::M3UParser* parser = new iptv::M3UParser();
    return reinterpret_cast<jlong>(parser);
}

JNIEXPORT void JNICALL
Java_com_example_iptvplayer_NativePlaylistParser_nativeDestroyParser(JNIEnv *env, jobject thiz, jlong ptr) {
    iptv::M3UParser* parser = reinterpret_cast<iptv::M3UParser*>(ptr);
    delete parser;
}

JNIEXPORT jobject JNICALL
Java_com_example_iptvplayer_NativePlaylistParser_nativeParseM3U(JNIEnv *env, jobject thiz, jlong ptr, jstring input) {
    iptv::M3UParser* parser = reinterpret_cast<iptv::M3UParser*>(ptr);
    if (!parser || !input) return nullptr;

    const char* chars = env->GetStringUTFChars(input, nullptr);
    if (!chars) return nullptr;

    std::string_view view(chars, env->GetStringUTFLength(input));
    std::vector<iptv::Channel> channels = parser->parse(view);

    env->ReleaseStringUTFChars(input, chars);

    // Create ArrayList<Channel>
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListCtor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jobject list = env->NewObject(arrayListClass, arrayListCtor);

    jclass channelClass = env->FindClass("com/example/iptvplayer/model/Channel");
    jmethodID channelCtor = env->GetMethodID(channelClass, "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    for (const auto& ch : channels) {
        jstring jName = env->NewStringUTF(ch.name.c_str());
        jstring jUrl = env->NewStringUTF(ch.url.c_str());
        jstring jLogo = env->NewStringUTF(ch.logo.c_str());

        jobject channelObj = env->NewObject(channelClass, channelCtor, jName, jUrl, jLogo);

        env->CallBooleanMethod(list, addMethod, channelObj);

        env->DeleteLocalRef(jName);
        env->DeleteLocalRef(jUrl);
        env->DeleteLocalRef(jLogo);
        env->DeleteLocalRef(channelObj);
    }

    return list;
}

} // extern "C"
