# Project-specific R8 rules will be added as the release build evolves.

-keep class com.kakao.sdk.**.model.* { <fields>; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**
