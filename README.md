# 已过时

**2024/7/4: 它完成了它的使命。随着官方公开了多推接口，这个SDK可以认为过时了，并且不再维护，未来也不保证可用。 -- SNWCreations**

# KookVoice4J

一个简单的 Kook 语音 SDK ，可以让你的 Bot 进入一个语音频道。

见 snw.kookvoice.Connector#connect 方法。

原始实现作者: hank9999

此库以 MIT 许可证授权。

依赖: Okhttp 4.10.0, GSON 2.9.0

## 推流说明

推流需要自行完成，此 SDK **不做**处理。

以下内容复制自 hank9999 的 khl-voice-API 项目备份，向大佬致敬！

```
ffmpeg推流
ffmpeg -re -loglevel level+info -nostats -i "xxxxx.mp3" -map 0:a:0 -acodec libopus -ab 128k -filter:a volume=0.8 -ac 2 -ar 48000 -f tee [select=a:f=rtp:ssrc=1357:payload_type=100]rtp://xxxx
```

```
用ffmpeg zmq可以实现切歌不掉
ffmpeg -re -nostats -i "xxx.mp3" -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:1234

ffmpeg -re -loglevel level+info -nostats -stream_loop -1 -i zmq:tcp://127.0.0.1:1234 -map 0:a:0 -acodec libopus -ab 128k -filter:a volume=0.8 -ac 2 -ar 48000 -f tee [select=a:f=rtp:ssrc=1357:payload_type=100]rtp://xxxx
```
