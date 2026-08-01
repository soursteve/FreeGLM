# FreeGLM

A minimal Android chat app (Kotlin + Jetpack Compose) that talks to the FreeGLM
chat endpoint and streams the reply token-by-token.

## Backend

```
POST https://u1s6661r6b71-d.space-z.ai/api/chat
Content-Type: application/json

{ "messages": [ { "role": "user", "content": "Hello, how are you?" } ] }
```

The endpoint responds with a `text/event-stream` of `data: {"content":"..."}`
lines terminated by `data: [DONE]`. `ChatRepository` reads this stream over
OkHttp and emits each content delta as it arrives.

## Project layout

- `app/src/main/java/com/freeglm/app/MainActivity.kt` — entry point
- `app/src/main/java/com/freeglm/app/ChatViewModel.kt` — chat state + streaming
- `app/src/main/java/com/freeglm/app/data/` — request/response models and the
  SSE-streaming repository
- `app/src/main/java/com/freeglm/app/ui/` — Compose chat UI and theme

## Build

```
./gradlew assembleDebug
```

Requires the Android SDK (compileSdk 34) and JDK 17+. Set `sdk.dir` in
`local.properties` or the `ANDROID_HOME` environment variable.
