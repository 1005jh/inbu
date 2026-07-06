# 인부장부

건설 현장의 작업 인원, 공수, 일당과 지급 상태를 기록하고 조회하는 Android 앱입니다.

## 개발 환경

- Android Studio 2026.1.1 Patch 2
- Android Gradle Plugin 9.2.1
- Gradle 9.4.1
- JDK 17
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.06.00
- compileSdk 37 / targetSdk 37 / minSdk 26
- Node.js 24.18.0 LTS (`nvm use`)

## 저장소 방향

앱의 화면과 업무 규칙은 공통으로 유지하고 저장 방식만 교체할 수 있게 개발합니다.

- 기기 저장 버전(`offline`): 로그인과 서버 없이 기기 내부 SQLite 데이터베이스만 사용
- 서버 버전: 서버 API와 데이터베이스

서버 버전은 카카오 로그인만 제공하며, Android 앱에서 받은 카카오 인증 결과를
서버 세션으로 교환하는 구조로 개발합니다.

## 카카오 로그인 설정

카카오 Developers에서 카카오 로그인을 활성화하고 Android 플랫폼을 등록합니다.
패키지명은 앱의 `applicationId`와 동일하게 입력하고, 개발·배포 인증서의 키 해시를
각각 생성해 등록합니다.

그 후 예시 파일을 복사하고 Git에 포함되지 않는 `local.properties`에 네이티브 앱 키를
추가합니다. 실제 키는 README나 `local.properties.example`에 기록하지 않습니다.

```bash
cp local.properties.example local.properties
```

앱 키가 없더라도 프로젝트는 빌드되며, 로그인 버튼을 누르면 설정 안내를 표시합니다.

## 실행

```bash
./gradlew :app:assembleOnlineDebug
./gradlew :app:assembleOfflineDebug
```

디버그 APK는 다음 위치에 생성됩니다.

- 서버 버전: `app/build/outputs/apk/online/debug/app-online-debug.apk`
- 기기 저장 버전: `app/build/outputs/apk/offline/debug/app-offline-debug.apk`

기기 저장 버전은 앱을 실행하면 로그인 화면 없이 바로 홈으로 이동합니다. 인터넷 권한,
카카오 SDK 및 서버 연결이 포함되지 않으며 앱을 삭제하기 전까지 해당 기기 안에 기록이
보관됩니다.

## API 서버 연결

Android 에뮬레이터에서는 기본 로컬 서버 주소를 사용하며 `local.properties`의
`API_BASE_URL`로 개발·배포 서버 주소를 바꿀 수 있습니다. 실제 주소와 카카오 앱 키가
들어가는 `local.properties`는 Git에서 제외됩니다. 디버그 빌드에서만 로컬 HTTP
통신을 허용하며 릴리스 서버는 HTTPS 주소를 사용해야 합니다. 백엔드는 별도 저장소로
관리합니다.
